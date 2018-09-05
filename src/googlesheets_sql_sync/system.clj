(ns googlesheets-sql-sync.system
  (:require
   [clojure.core.async :refer [<! chan close! go-loop]]
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
        (try
          (oauth/handle-code config-file-path c)
          (catch Exception e (println "errror handling code" (.getMessage e))))
        (recur))
      (println "stop handling auth codes"))))

(defn- show-init-message [c]
  (println "no access token found. initializing...")
  (println "please visit the oauth url in your browser:")
  (println (oauth/get-url c)))

(defn- do-sync [config-file-path]
  ; TODO stop system on err
  (let [c (config/read-file config-file-path)]
    (if-let [token (get-access-token c)]
      (try
        (println "starting sync")
        (oauth/handle-refresh-token config-file-path)
        (->> c
            :sheets
            (map #(sheets/fetch-rows % token))
            (run! #(db/update-table c %)))
        (println "sync done")
        (catch Exception e (println (.getMessage e))))
      (show-init-message c))
    (interval/to-ms (:interval c))))

(defn- sync-in-interval [config-file-path]
  (interval/start #(do-sync config-file-path)))

(defn start [config-file-path]
  ; TODO stop system on err
  ; (catch java.io.FileNotFoundException e (prn (.getMessage e)))
  (let [c (config/read-file config-file-path)
        code-chan (chan)
        stop-server (web/start c code-chan)
        stop-sync (sync-in-interval config-file-path)]
    (go-handle-auth-codes config-file-path code-chan)
    #(do (println "\nshutting down ...")
         (stop-server)
         (stop-sync)
         (close! code-chan))))
