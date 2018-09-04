(ns googlesheets-sql-sync.system
  (:require
   [clojure.core.async :refer [<! <!! chan close! go go-loop]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.db :as db]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.oauth :as oauth]
   [googlesheets-sql-sync.sheets :as sheets]
   [googlesheets-sql-sync.web :as web]))

(defn- get-access-token [config]
  (get-in config [:google_credentials :access_token]))

(defn- go-handle-auth-codes [config-file-path code-chan]
  (go-loop []
    (if-let [c (<! code-chan)]
      (do
        (oauth/handle-code config-file-path c)
        (recur))
      (println "stop handling auth codes"))))

(defn- do-sync [config-file-path]
  (oauth/handle-refresh-token config-file-path)
  (let [c (config/read-file config-file-path)
        token (get-access-token c)]
    (println "starting sync")
    (->> c
         :sheets
         (map #(sheets/fetch-rows % token))
         (run! #(db/update-table c %)))
    (println "sync done")
    (interval/to-ms (:interval c))))

(defn- sync-in-interval [config-file-path]
  (interval/start #(do-sync config-file-path)))

(defn start [config-file-path]
  (let [c (config/read-file config-file-path)
        code-chan (chan)
        stop-server (web/start c code-chan)]
    (when-not (get-access-token c)
      (println "no access token found. initializing...")
      (println "please visit the oauth url in your browser:")
      (println (oauth/get-url c))
      (oauth/handle-code config-file-path (<!! code-chan)))
    (println "found access token")
    (go-handle-auth-codes config-file-path code-chan)
    (let [stop-sync (sync-in-interval config-file-path)]
      #(do (println "\nshutting down ...")
           (stop-server)
           (stop-sync)
           (close! code-chan)))))
