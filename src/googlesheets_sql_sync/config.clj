(ns googlesheets-sql-sync.config
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [googlesheets-sql-sync.web :refer [oauth-route]]
   [googlesheets-sql-sync.spec :as spec]))

(defn- default-config [port]
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
   :interval {:minutes 30}})

(defn- write-file [data config-file]
  (->> (json/generate-string (spec/valid-config data) {:pretty true})
       (spit config-file))
  data)

(defn read-file [config-file]
  (spec/valid-config (json/parse-string (slurp config-file) true)))

(defn generate [{:keys [config-file port]}]
  (if (.exists (io/as-file config-file))
    (do
      (println "Stopping because file already exists:" f)
      :not-ok)
    (do
      (println "generating" f)
      (write-file (default-config port) f)
      (println "done"))))

(defn merge-file
  [config-file k func]
  ;re-reading config file just in case something changed during token HTTP fetch
  (println "Updating config file" config-file)
  (-> (read-file config-file)
      (update k #(merge % (func %)))
      (write-file config-file)))
