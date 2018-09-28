(ns googlesheets-sql-sync.config
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
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

(defn- write-file
  "Validate config and write as JSON to file.
  Return passed config."
  [data config-file]
  (spit
    config-file
    (json/generate-string (spec/valid-config data) {:pretty true}))
  data)

(defn read-file
  "Read config from JSON file and validate it."
  [config-file]
  (spec/valid-config (json/parse-string (slurp config-file) true)))

(defn generate
  "Write config template to a file."
  [{:keys [config-file port oauth-route]}]
  (if (.exists (io/as-file config-file))
    (do
      (println "Stopping because file already exists:" config-file)
      :not-ok)
    (do
      (println "generating" config-file)
      (write-file (template-config port oauth-route) config-file)
      (println "done"))))

(defn merge-in-file
  "Update value for given key k in config file by applying f to it
  and write it back to file.
  Returns resulting config."
  [config-file k f]
  ;re-reading config file just in case something changed during token HTTP fetch
  (println "Updating config file" config-file)
  (-> (read-file config-file)
      (update k #(merge % (f %)))
      (write-file config-file)))
