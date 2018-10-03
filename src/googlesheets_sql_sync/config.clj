(ns googlesheets-sql-sync.config
  (:refer-clojure :exclude [get])
  (:require
   [clojure.java.io :as io]
   [jsonista.core :as json]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.spec :as spec]))

(defn- template-config [port oauth-route]
  {:sheets [{:table          "your_sql_table_name"
             :spreadsheet_id "COPY AND PAST FROM URL IN BROWSER"
             :target         "my_target"}]
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

(defn- write-json
  "Write data as JSON to file"
  [file data]
  (json/write-value (io/file file) data (json/object-mapper {:pretty true})))

(defn get
  "Read config from JSON file, validate and return it."
  [config-file]
  (spec/valid-config (json/read-value
                      (io/file config-file)
                      (json/object-mapper {:decode-key-fn true}))))

(defn generate
  "Write config template to a file."
  [{:keys [config-file port oauth-route]}]
  (when (.exists (io/file config-file))
    (log/error "Stopping because file already exists:" config-file)
    (System/exit 1))
  (log/info "Generating" config-file)
  (write-json config-file (template-config port oauth-route))
  (log/info "Done"))

(defn get-auth
  "Read auth from JSON file, validate and return it."
  [auth-file]
  (let [f (io/file auth-file)]
    (when (.exists f)
      (spec/valid-auth (json/read-value f (json/object-mapper {:decode-key-fn true}))))))

(defn save-auth
  "Validate data and write to auth-file."
  [auth-file data]
  (write-json auth-file (spec/valid-auth data)))
