(ns googlesheets-sql-sync.core
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.core.async :refer [<! <!! >!! alt! chan close! go go-loop timeout]]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as sql]
    [clojure.string :as string]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.params :refer [wrap-params]])
  (:gen-class))

(def usage "

  Generate a config file using:

    java -jar googlesheets_sql_sync.jar <client-id> <client-secret>

  Fill out the config file

  Then run:

    java -jar googlesheets_sql_sync.jar googlesheets_sql_sync.json

  Follow setup instructions

")

(def defaults {:config-file-path "googlesheets_sql_sync.json"
               :config {:sheets [{:table          "your_sql_table_name"
                                  :spreadsheet_id "COPY AND PAST FROM URL IN BROWSER"
                                  :target         "TARGET NAME"}]
                        :targets {"TARGET NAME" {}}
                        :google_credentials {:client_id     "COPY FROM GOOGLE CONSOLE"
                                             :client_secret "COPY FROM GOOGLE CONSOLE"
                                             :redirect_uri  "http://localhost:9955/oauth"}
                        :port 9955
                        :interval {:minutes 30}}
               :sheet-range "A:ZZ"})

(def user-oauth-url   "https://accounts.google.com/o/oauth2/v2/auth")
(def server-oauth-url "https://www.googleapis.com/oauth2/v4/token")
(def sheets-url       "https://sheets.googleapis.com/v4/spreadsheets/")
(def scope            "https://www.googleapis.com/auth/spreadsheets.readonly")
(def oauth-route      "/oauth")

(def default-user-params {:scope          scope
                          :access_type    "offline"
                          :response_type  "code"})

(def request-params-code    {:grant_type "authorization_code"})
(def request-params-refresh {:grant_type "refresh_token"})

(defn get-user-url [config]
  (->> (select-keys (:google_credentials config) [:client_id :redirect_uri])
       (merge default-user-params)
       (http/generate-query-string)
       (str user-oauth-url "?")))

(defn pwd [] (System/getProperty "user.dir"))

(defn write-json [data file]
  (->> (json/generate-string data {:pretty true})
       (spit file)))

(defn generate-config
  ([]
   (generate-config nil))
  ([file-path]
   (let [f (or file-path (str (pwd) "/" (:config-file-path defaults)))]
     (if (.exists (io/as-file f))
       (do
         (println "stopping because file already exists:" f)
         false)
       (do
         (println "generating" f)
         (write-json (:config defaults) f)
         (println "done")
         true)))))

(defn fetch-sheet [token id]
  (http/get (str sheets-url id "/values/" (:sheet-range defaults))
            {:headers {"Authorization" (str "Bearer " token)}
             :as :json}))

(defn get-sheet-with-data [token sheet]
  (let [id (:spreadsheet_id sheet)]
    (println "fetching data for" id)
    {:sheet sheet
     :rows (get-in (fetch-sheet token id) [:body :values])}))

(defn escape
  "Replace all c in text with \\c"
  [text c]
  (string/replace text c (str "\\" c)))

(comment
  (escape "hey \"you\"!" "\""))

(defn create [db headers table]
  (println table "- creating table")
  (let [columns (->> headers
                     (map #(str % " text"))
                     (string/join ", "))
        s (str "create table " table " ( " columns " )")]
    (sql/execute! db s)))

(defn get-columns [db table]
  (println table "- fetching db columns for table")
  (println "TODO this only works with special permission. use easier way to get columns")
  (let [s (str "select column_name from information_schema.columns where table_name = '" (escape table "'") "';")]
    (sql/query db s)))

(comment
  (let [config (read-json (:config-file-path defaults))
        sheet (first (:sheets config))]
    (if (table-exists? (get-in config [:targets (keyword (:target sheet))]) (:table sheet))
      "ok"
      "nope")))

(defn table-exists? [db table]
  (println "checking if table exists" table)
  (println "TODO this only works with special permission. use easier way to check this")
  (let [s (str "select 1 from information_schema.tables where table_name = '" (escape table "'") "';")]
    (< 0 (count (sql/query db s)))))

(defn setup-table [config sheet]
  (let [db (get-in config [:targets (keyword (get-in sheet [:sheet :target]))])
        table (get-in sheet [:sheet :table])
        rows (:rows sheet)
        headers (->> rows
                     first
                     (map #(escape % "\""))
                     (map #(str "\"" % "\"")))
        columns (get-columns db table)
        data (rest rows)]
    (if (table-exists? db table)
      (println "TODO check columns for changes here...")
      (create db headers table))
    (println table "- clearing table")
    (sql/execute! db (str "truncate table " table))
    (println table "- writing " (count data) "rows to table")
    (sql/insert-multi! db table headers data)
    sheet))

(defn interval-in-ms [config]
  (let [t (:interval config)
        d (-> t (get :days    0)       (* 24))
        h (-> t (get :hours   0) (+ d) (* 60))
        m (-> t (get :minutes 0) (+ h) (* 60))
        s (-> t (get :seconds 0) (+ m) (* 1000))]
    s))

(defn set-interval
  "see https://stackoverflow.com/a/28370388/986455"
  [f]
  (let [stop (chan)]
    (go-loop [t (f)]
      (alt!
        (timeout t) (recur (f))
        stop (do
               (println "stopped interval")
               :stop)))
    stop))

(def not-found
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body "not found"})

(def ok
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "all good"})

(defn make-handler
  [code-chan]
  (fn [req]
    (if-not (and
             (= :get (:request-method req))
             (= oauth-route (:uri req)))
      not-found
      (let [params (:params req)]
        (if-let [err (get params "error")]
          (println "got error" err)
          (let [code (get params "code")]
            (println "got code")
            (>!! code-chan code)))
        ok))))

(defn read-json [path]
  (json/parse-string (slurp path) true))

(defn get-access-token [config]
  (get-in config [:google_credentials :access_token]))

(defn fetch-access-token [config params]
  (println "fetching access token")
  (let [p (merge (select-keys (:google_credentials config) [:client_id :client_secret :redirect_uri])
                 params)]
    (-> (http/post server-oauth-url {:form-params p :as :json})
        :body
        (select-keys [:access_token :expires_in :refresh_token]))))

(defn update-credentials
  "re-reading config file just in case something changed during token HTTP fetch"
  [config-file-path creds]
  (println "updating config file" config-file-path)
  (-> (read-json config-file-path)
      (update :google_credentials #(merge % creds))
      (write-json config-file-path))
  (println "update done"))

(defn refresh-access-token [config-file-path]
  (println "refresh access token")
  (let [config (read-json config-file-path)
        params (merge request-params-refresh
                      (select-keys (:google_credentials config) [:refresh_token]))
        creds (fetch-access-token config params)]
    (update-credentials config-file-path creds)))

(defn handle-auth-code [config-file-path c]
  (when c
    (let [params (merge request-params-code {:code c})
          config (read-json config-file-path)
          creds  (fetch-access-token config params)]
      (update-credentials config-file-path creds))))

(defn do-sync [config-file-path]
  (refresh-access-token config-file-path)
  (let [config (read-json config-file-path)
        token (get-access-token config)]
    (println "starting sync")
    (->> config
         :sheets
         (map #(get-sheet-with-data token %))
         (run! #(setup-table config %)))
    (println "sync done")
    (interval-in-ms config)))

(defn sync-in-interval [config-file-path]
  (set-interval #(do-sync config-file-path)))

(defn handle-auth-codes [config-file-path code-chan]
  (go-loop []
    (if-let [c (<! code-chan)]
      (do
        (handle-auth-code config-file-path c)
        (recur))
      (println "stop handling auth codes"))))

(defn run [config-file-path]
  (let [config (read-json config-file-path)
        code-chan (chan)
        handler (make-handler code-chan)
        app (-> handler wrap-params)
        server (run-jetty app {:port (:port config
                                          :join? false)})
        stop (chan)]
    (when-not (get-access-token config)
      (println "no access token found. initializing...")
      (println "please visit the oauth url in your browser:")
      (println (get-user-url config))
      (handle-auth-code config-file-path (<!! code-chan)))
    (println "found access token")
    (handle-auth-codes config-file-path code-chan)
    (let [syncer (sync-in-interval config-file-path)]
      (go (<! stop)
          (.stop server)
          (close! syncer)
          (close! code-chan)))
    stop))

(defn -main
  [& args]
  (let [a1 (first args)
        a2 (second args)
        c (count args)]
    (cond
      (and (= "init" a1) (<= c 2)) (if-not (generate-config a2) (System/exit 1))
      (= c 1) (run a1)
      :else (do (println usage)
                (System/exit 1)))))
