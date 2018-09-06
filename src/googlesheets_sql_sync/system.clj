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

(defn- show-init-message
  ([c] (show-init-message c true))
  ([c new?]
   (when new?
     (println "no access token found."))
   (println "Authenticating...\n Please visit the oauth url in your browser:")
   (println (oauth/get-url c))))

(defn- do-sync [config-file]
  ; TODO stop system on err
  (let [c (config/read-file config-file)]
    (if-let [token (get-access-token c)]
      (try
        (println "starting sync")
        (oauth/handle-refresh-token config-file)
        (->> c
             :sheets
             (map #(sheets/fetch-rows % token))
             (run! #(db/update-table c %)))
        (println "sync done")
        (catch Exception e (println (.getMessage e) (ex-data e))))
      (show-init-message c))
    (interval/to-ms (:interval c))))

(defn- call-maybe [m k]
  (when-let [f (k m)]
    (f)))

(defn stop [ctx]
  (println "\nshutting down ...")
  (call-maybe ctx :stop-server)
  (call-maybe ctx :stop-sync)
  (close! (:code-chan ctx)))

(defn- go-handle-auth-codes [ctx]
  (println "setup auth code handler")
  (let [code-chan (:code-chan ctx)
        auth-only (:auth-only ctx)]
    (go-loop []
      (if-let [code (<! code-chan)]
        (do
          (try
            (oauth/handle-code (:config-file ctx) code)
            (catch Exception e (println "errror handling code" (.getMessage e))))
          (if auth-only
            (do
              (println "auth done")
              (stop ctx))
            (recur)))
        (println "stop handling auth codes"))))
  ctx)

(defn- auth [ctx]
  (when (:auth-only ctx)
    (let [config-file (:config-file ctx)
          c (config/read-file config-file)]
      (if (get-access-token c)
        (do
          (println "Found access token.")
          (if (oauth/handle-refresh-token config-file)
            (do
              (println "Token valid. Nothing to do.")
              (stop ctx))
            (show-init-message c false)))
        (show-init-message c))))
  ctx)

(defn- sync-in-interval [ctx]
  (if (:auth-only ctx)
    ctx
    (let [config-file (:config-file ctx)]
      (println "start interval scheduler")
      (assoc ctx :stop-sync (interval/start #(do-sync config-file))))))

(defn start [ctx]
  ; TODO stop system on err
  ; (catch java.io.FileNotFoundException e (prn (.getMessage e)))
  (-> ctx
      (assoc :code-chan (chan))
      web/start
      sync-in-interval
      go-handle-auth-codes
      auth))

(comment
  (prn s)
  (do
    (stop s)
    (def s (start {:port 9955
                   :config-file "googlesheets_sql_sync.json"}))))
