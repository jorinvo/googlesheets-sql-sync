(ns googlesheets-sql-sync.config
  (:require
   [cheshire.core :as json]
   [googlesheets-sql-sync.web :refer [oauth-route]]
   [clojure.java.io :as io]))

(def default-file-path "googlesheets_sql_sync.json")

(def default-port 9955)

(def default-config {:sheets [{:table          "your_sql_table_name"
                               :spreadsheet_id "COPY AND PAST FROM URL IN BROWSER"
                               :target         "my_target"}]
                     :targets {"my_target" {}}
                     :google_credentials {:client_id     "COPY FROM GOOGLE CONSOLE"
                                          :client_secret "COPY FROM GOOGLE CONSOLE"
                                          :redirect_uri  (str "http://localhost:" default-port oauth-route)}
                     :port default-port
                     :interval {:minutes 30}})

(defn- write-file [data file]
  (->> (json/generate-string data {:pretty true})
       (spit file)))

(defn read-file [path]
  (json/parse-string (slurp path) true))

(defn generate
  ([]
   (generate nil))
  ([file-path]
   (let [f (or file-path default-file-path)]
     (if (.exists (io/as-file f))
       (do
         (println "stopping because file already exists:" f)
         :not-ok)
       (do
         (println "generating" f)
         (write-file default-config f)
         (println "done"))))))

(defn merge-file
  [config-file-path k f]
  ;re-reading config file just in case something changed during token HTTP fetch
  (-> (read-file config-file-path)
      (update k #(merge % (f %)))
      (write-file config-file-path))
  (println "updated config file" config-file-path))
