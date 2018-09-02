(ns googlesheets-sql-sync.core
  (:require [clojure.string :as s]
            [clojure.core.async :refer [chan go go-loop alt! timeout close! >!! <!]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as ring-jetty]
            [clojure.java.jdbc :as jdbc])
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
               :config {:sheets [{:table "your_sql_table_name"
                                  :spreadsheet-id "COPY AND PAST FROM URL IN BROWSER"
                                  :target "TARGET NAME"}]
                        :targets {"TARGET NAME" {}}
                        :google-credentials {:redirect-uri "http://localhost:9955/oauth"}
                        :port 9955
                        :interval {:minutes 30}}
               :sheet-range "A:ZZ"})

(def user-oauth-url   "https://accounts.google.com/o/oauth2/v2/auth")
(def server-oauth-url "https://www.googleapis.com/oauth2/v4/token")
(def sheets-url       "https://sheets.googleapis.com/v4/spreadsheets/")
(def scope            "https://www.googleapis.com/auth/spreadsheets.readonly")
(def oauth-route      "/oauth")

(def default-user-params {:scope scope
                          :access_type "offline"
                          :response_type "code"})

(def default-server-params {:grant_type "authorization_code"})

(defn get-user-url [config]
  (let [user-params (merge default-user-params
                           {:client_id (get-in config [:google-credentials :client-id])
                            :redirect_uri get-in config [:google-credentials :redirect-uri]})]
    (str user-oauth-url "?" (http/generate-query-string user-params))))

(defn pwd [] (System/getProperty "user.dir"))

(defn generate-config [client-id client-secret]
  (let [file (str (pwd) "/" (:config-file-path defaults))]
    (println "generating" file)
    (->> (-> defaults
            :config
            (assoc-in [:google-credentials :client-id] client-id)
            (assoc-in [:google-credentials :client-secret] client-secret)
            (json/generate-string {:pretty true}))
        (spit file))
    (println "generated" file)))

(defn fetch-sheet [token id]
  (http/get (str sheets-url id "/values/" (:sheet-range defaults))
            {:headers {"Authorization" (str "Bearer " token)}
             :as :json}))

(defn get-sheet-with-data [token sheet]
  (let [id (:spreadsheet-id sheet)]
    (println "fetching data for" id)
    {:sheet sheet
     :rows (first (fetch-sheet token id))}))

(defn escape
  "Replace all c in text with \\c"
  [text c]
  (s/replace text c (str "\\" c)))

(comment
  (escape "hey \"you\"!" "\""))

(defn create [db headers table]
  (println "creating table" table)
  (let [columns (->> headers
                     (map #(str % " text"))
                     (s/join ", "))
        sql (str "create table " table " ( " columns " )")]
    (jdbc/execute! db sql)))

(defn get-columns [db table]
  (println "fetching columns for table" table)
  (let [sql (str "select column_name from information_schema.columns where table_name = '" (escape table "'") "';")]
    (jdbc/query db sql)))

(defn setup-table [db sheet]
  (let [table (get-in sheet [:sheet :table])
        rows (:rows sheet)
        headers (->> rows
                     first
                     (map #(escape % "\""))
                     (map #(str "\"" % "\"")))
        columns (get-columns db table)
        data (rest rows)]
    (if (> 1 (count columns))
      (create db headers table)
      (println "TODO check columns for changes here..."))
    (println "clearing table" table)
    (jdbc/execute! db (str "truncate table " table))
    (println "writing " (count data) "rows to table" table)
    (jdbc/insert-multi! db table headers data)
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
        stop :stop))
    stop))

(def not-found
  {:status 404
    :headers {"Content-Type" "text/html"}
    :body "not found"})

(defn fetch-access-token [config code]
  (let [params (merge default-server-params
                      {:client_id (get-in config [:google-credentials :client-id])
                       :client_secret (get-in config [:google-credentials :client-secret])
                       :redirect_uri (get-in config [:google-credentials :redirect-uri])
                       :code code})]
    (http/post server-oauth-url {:form-params params :as :json})))

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
            (println "got code" code)
            (>!! code-chan code)))
        ok))))

(defn read-json [path]
  (json/parse-string (slurp path) true))

(defn get-access-token [config]
  (get-in config [:google-credentials :access-token]))

(defn do-sync [config-file-path]
  (let [config (read-json config-file-path)
        db (get-in config [:targets :bi])
        token (get-access-token config)]
    (println "start sync")
    (->> config
         :sheets
         (map #(get-sheet-with-data token %))
         (run! #(setup-table db %)))
    (println "sync done")
    (interval-in-ms config)))

(defn sync-in-interval [config-file-path]
  (set-interval #(do-sync config-file-path)))

(defn run [config-file-path]
  (let [config (read-json config-file-path)
        code-chan (chan)
        handler (make-handler code-chan)
        app (-> handler wrap-params)
        server (ring-jetty/run-jetty app {:port (:port config)
                                          :join? false})]
    (go (<! code-chan))
    (if (get-access-token config)
      (do
        (println "found access token")
        (sync-in-interval config-file-path))
      (do
        (println "no access token found. initializing...")
        (println "please visit the oauth url in your browser:")
        (println (get-user-url config))))
    server))

(defn -main
  [& args]
  (cond
    (= 1 (count args)) (run (first args))
    (= 2 (count args)) (generate-config (first args) (last args))
    :else (do (println usage)
              (System/exit 1))))

(comment
  (.stop server)
  (.start server))
