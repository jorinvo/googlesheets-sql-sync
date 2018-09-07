(ns googlesheets-sql-sync.config
  (:require
   [cheshire.core :as json]
   [googlesheets-sql-sync.web :refer [oauth-route]]
   [clojure.java.io :as io]))

(defn- get-default-config [ctx]
  {:sheets [{:table          "your_sql_table_name"
             :spreadsheet_id "COPY AND PAST FROM URL IN BROWSER"
             :target         "my_target"}]
   :targets {"my_target" {}}
   :google_credentials {:client_id     "COPY FROM GOOGLE CONSOLE"
                        :client_secret "COPY FROM GOOGLE CONSOLE"
                        :redirect_uri  (str "http://localhost:" (:port ctx) oauth-route)}
   :interval {:minutes 30}})

(defn- write-file [data config-file]
  (->> (json/generate-string data {:pretty true})
       (spit config-file))
  data)

(defn read-file [config-file]
  (json/parse-string (slurp config-file) true))

(defn generate
  ([ctx]
   (let [f (:config-file ctx)]
     (if (.exists (io/as-file f))
       (do
         (println "Stopping because file already exists:" f)
         :not-ok)
       (do
         (println "generating" f)
         (write-file (get-default-config ctx) f)
         (println "done"))))))

(defn merge-file
  [config-file k func]
  ;re-reading config file just in case something changed during token HTTP fetch
  (println "Updating config file" config-file)
  (-> (read-file config-file)
      (update k #(merge % (func %)))
      (write-file config-file)))
