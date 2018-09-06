(ns googlesheets-sql-sync.system
  (:require
   [clojure.core.async :as async :refer [<! >! >!! alt! chan close! dropping-buffer go-loop pipeline-async]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.db :as db]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.oauth :as oauth]
   [googlesheets-sql-sync.sheets :as sheets]
   [googlesheets-sql-sync.web :as web]))

(defn- show-init-message [c]
  (println "Please visit the oauth url in your browser:")
  (println (oauth/get-url c)))

(defn stop [{:keys [stop-server timeout> work>]}]
  (println "\nshutting down ...")
  (when stop-server (stop-server))
  (close! timeout>)
  (close! work>))

(defn- do-sync [{:keys [auth-only config-file timeout>] :as ctx}]
  (println "do sync")
  ; TODO stop system on err
  (try
    (let [c (config/read-file config-file)]
      (try
        (if-let [token (oauth/refresh-token config-file)]
          (if auth-only
            (do (println "Authentication done.")
                (stop ctx))
            (->> (:sheets c)
                 (map #(sheets/fetch-rows % token))
                 (run! #(db/update-table c %))))
          (show-init-message c))
        (catch Exception e (println (.getMessage e) (ex-data e))))
      (async/put! timeout> (interval/to-ms (:interval c))))
    (catch java.io.FileNotFoundException e (do (println (.getMessage e))
                                               (stop ctx)))))

(defn- handle-code [{:keys [config-file]} code]
  (println "handle code")
  (try
    (oauth/handle-code config-file code)
    (catch Exception e (println "errror handling code" (.getMessage e)))))

(defn- start-worker [{:keys [work>] :as ctx}]
  (println "start worker")
  (go-loop []
    (if-let [[job code] (<! work>)]
      (do (case job
            :sync (do-sync ctx)
            :code (handle-code ctx code))
          (recur))
      (println "stopping worker")))
  (async/put! work> [:sync]))

(defn start [ctx]
  (-> ctx
      (assoc :work> (chan))
      (assoc :timeout> (chan (dropping-buffer 1)))
      web/start
      (doto interval/connect-timeouts start-worker)))

(defn trigger-sync [{:keys [work>]}]
  (>!! work> [:sync]))

(comment
  (do
    (stop s)
    (def s (start {:port 9955 :config-file "googlesheets_sql_sync.json"})))
  (def s (start {:port 9955 :config-file "googlesheets_sql_sync.json" :auth-only true}))
  (config/generate {:port 9955 :config-file "googlesheets_sql_sync.json"}))
