(ns googlesheets-sql-sync.config
  (:refer-clojure :exclude [get])
  (:require
   [clojure.java.io :as io]
   [clojure.java.jdbc.spec :as jdbc-spec]
   [clojure.spec.alpha :as s]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.util :as util :refer [fail]]))

(s/def ::table ::util/str-not-empty)
(s/def ::spreadsheet_id ::util/str-not-empty)
(s/def ::target ::util/str-not-empty)
(s/def ::range ::util/str-not-empty)
(s/def ::sheets (s/coll-of (s/keys :req-un [::table
                                            ::spreadsheet_id
                                            ::target
                                            ::range])))

(s/def ::targets (s/map-of keyword?
                           ::jdbc-spec/db-spec))

(s/def ::client_id ::util/str-not-empty)
(s/def ::client_secret ::util/str-not-empty)
(s/def ::redirect_uri util/valid-url?)

(s/def ::google_credentials (s/keys :req-un [::client_id
                                             ::client_secret
                                             ::redirect_uri]))

(s/def ::days nat-int?)
(s/def ::hours nat-int?)
(s/def ::minutes nat-int?)
(s/def ::seconds nat-int?)
(s/def ::interval (s/keys :req-un [(or ::days
                                       ::hours
                                       ::minutes
                                       ::seconds)]))

(defn- targets-exist?
  "Check if targets in sheets actually exist"
  [{:keys [sheets targets]}]
  (every? #(targets (keyword (:target %))) sheets))

(comment
  (= true targets-exist? {:sheets [{:target "a"} {:target "b"}]
                          :targets {:a 1 :b {}}})
  (= false (targets-exist? {:sheets [{:target "a"}]
                            :targets {:b 1}})))

(s/def ::config (s/and (s/keys :req-un [::sheets
                                        ::targets
                                        ::google_credentials
                                        ::interval])
                       targets-exist?))

(defn- valid
  "Validates config data.
  Throws error containing spec violation."
  [data]
  (when-not (s/valid? ::config data)
    (fail (s/explain-str ::config data)))
  data)

(defn get
  "Read config from JSON file, validate and return it."
  [config-file]
  (valid (util/read-json-file (io/file config-file))))

(defn- template-config [port oauth-route]
  {:sheets [{:table          "your_sql_table_name"
             :spreadsheet_id "COPY AND PAST FROM URL IN BROWSER"
             :target         "my_target"
             :range          "A:ZZ"}]
   :targets {:my_target {:dbtype "postgresql"
                         :dbname "postgres"
                         :host "localhost"
                         :user "postgres"}}
   :google_credentials {:client_id     "COPY FROM GOOGLE CONSOLE"
                        :client_secret "COPY FROM GOOGLE CONSOLE"
                        :redirect_uri  (str "http://localhost:" port oauth-route)}
   :interval {:days     0
              :hours    0
              :minutes 30
              :seconds  0}})

(defn generate
  "Write config template to a file."
  [{:keys [config-file port oauth-route]}]
  (when (.exists (io/file config-file))
    (fail "File already exists: " config-file))
  (log/info "Generating" config-file)
  (util/write-json-file config-file (template-config port oauth-route))
  (log/info "Done"))
