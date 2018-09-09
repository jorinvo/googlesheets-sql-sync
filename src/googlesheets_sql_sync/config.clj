(ns googlesheets-sql-sync.config
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.java.jdbc.spec :as jdbc-spec]
   [clojure.spec.alpha :as s]
   [googlesheets-sql-sync.web :refer [oauth-route]]))

(defn- default-config [{:keys [port]}]
  {:sheets [{:table          "your_sql_table_name"
             :spreadsheet_id "COPY AND PAST FROM URL IN BROWSER"
             :target         "my_target"}]
   :targets {"my_target" {:dbtype "postgresql"
                          :dbname "postgres"
                          :host "localhost"
                          :user "postgres"}}
   :google_credentials {:client_id     "COPY FROM GOOGLE CONSOLE"
                        :client_secret "COPY FROM GOOGLE CONSOLE"
                        :redirect_uri  (str "http://localhost:" port oauth-route)}
   :interval {:minutes 30}})

(s/def ::str-not-empty (s/and string? not-empty))

(s/def ::table ::str-not-empty)
(s/def ::spreadsheet_id ::str-not-empty)
(s/def ::target ::str-not-empty)
(s/def ::sheets (s/coll-of (s/keys :req-un [::table]
                                   ::spreadsheet_id
                                   ::target)))

(s/def ::targets (s/map-of keyword?
                           ::jdbc-spec/db-spec))

(defn- valid-url? [s]
  (try
    (.toURL (java.net.URI. s))
    true
    (catch Exception e false)))

(comment
  (true? (valid-url? "http://localhost:80"))
  (false? (valid-url? "http//localhost:80")))

(s/def ::client_id ::str-not-empty)
(s/def ::client_secret ::str-not-empty)
(s/def ::redirect_uri valid-url?)
(s/def ::access_token ::str-not-empty)
(s/def ::expires_in pos-int?)
(s/def ::refresh_token ::str-not-empty)

(s/def ::google_credentials (s/keys :req-un [::client_id
                                             ::client_secret
                                             ::redirect_uri]
                                    :opt-un [::access_token
                                             ::expires_in
                                             ::refresh_token]))

(s/def ::days pos-int?)
(s/def ::hours pos-int?)
(s/def ::minutes pos-int?)
(s/def ::seconds pos-int?)
(s/def ::interval (s/and (s/keys :req-un [(or ::days
                                              ::hours
                                              ::minutes
                                              ::seconds)])))

(comment
  (s/valid? ::interval (:interval (default-config {:port 1234}))))

(defn- targets-exist? [{:keys [sheets targets]}]
  (every? #(targets (keyword (:target %))) sheets))

(comment
  (targets-exist? {:sheets [{:target "a"} {:target "b"}]
                   :targets {:a 1 :b {}}})
  (false? (targets-exist? {:sheets [{:target "a"}]
                           :targets {:b 1}})))

(s/def ::config (s/and (s/keys :req-un [::sheets
                                        ::targets
                                        ::google_credentials
                                        ::interval])
                       targets-exist?))

(defn- valid-config [data]
  (when-not (s/valid? ::config config)
    (throw (Exception. (s/explain-str ::config config))))
  config)

(defn- write-file [data config-file]
  (->> (json/generate-string (valid-config data) {:pretty true})
       (spit config-file))
  data)

(defn read-file [config-file]
  (valid-config (json/parse-string (slurp config-file) true)))

(defn generate
  ([ctx]
   (let [f (:config-file ctx)]
     (if (.exists (io/as-file f))
       (do
         (println "Stopping because file already exists:" f)
         :not-ok)
       (do
         (println "generating" f)
         (write-file (default-config ctx) f)
         (println "done"))))))

(defn merge-file
  [config-file k func]
  ;re-reading config file just in case something changed during token HTTP fetch
  (println "Updating config file" config-file)
  (-> (read-file config-file)
      (update k #(merge % (func %)))
      (write-file config-file)))
