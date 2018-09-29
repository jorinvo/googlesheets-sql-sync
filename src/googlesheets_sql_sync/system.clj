(ns googlesheets-sql-sync.system
  (:require
   [clojure.core.async :as async :refer [<!! >!! chan close! dropping-buffer]]
   [clojure.java.browse :refer [browse-url]]
   [mount.core :as mount]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.db :as db]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.oauth :as oauth]
   [googlesheets-sql-sync.sheets :as sheets]))

(defn- show-init-message
  "Prompt user to visit auth URL.
  When running locally, open browser automatically."
  [cfg]
  (let [url (oauth/url cfg)]
    (println "Please visit the oauth url in your browser:\n" url)
    (when (oauth/local-redirect? cfg)
      (browse-url url))))

(defn- do-sync [{:keys [auth-only config-file no-server timeout>]}]
  (try
    (let [cfg (config/read-file config-file)]
      (try
        (if-let [token (oauth/refresh-token config-file)]
          (if auth-only
            (do (println "Authentication done")
                (System/exit 0))
            (do
              (->> (:sheets cfg)
                   (map #(sheets/get-rows % token))
                   (run! #(db/update-table cfg %)))
              (println "Sync done")))
          (if no-server
            (do
              (println "Cannot authenticate when server is disabled")
              (System/exit 1))
            (show-init-message cfg)))
        (catch Exception e (println (.getMessage e) "\nSync failed")))
      (println "Next sync in" (interval/->string (:interval cfg)))
      (async/put! timeout> (interval/->ms (:interval cfg))))
    (catch Exception e (do (println "Failed reading config file" (.getMessage e))
                           (System/exit 1)))))

(defn- start-worker [{:keys [config-file work>] :as ctx}]
  (println "Starting worker")
  (async/put! work> [:sync])
  (loop []
    (if-let [[job code] (<!! work>)]
      (do (case job
            :sync (do-sync ctx)
            :code (do
                    (oauth/handle-code config-file code)
                    (do-sync ctx)))
          (recur))
      (println "Stopping worker"))))

(defn start
  [options]
  (try
    (let [timeout> (chan (dropping-buffer 1))
          work>    (chan)
          ctx      {:work>    work>
                    :timeout> timeout>}]
      (interval/connect-timeouts ctx)
      (start-worker (merge options ctx))
      ctx)
    (catch Exception e (do (println "Error while starting:" (.getMessage e))
                           (System/exit 1)))))

(defn stop
  "Stops system. If system is not running, nothing happens."
  [{:keys [timeout> work>]}]
  (println "\nShutting down")
  (when timeout>
    (close! timeout>))
  (when work>
    (close! work>)))

(mount/defstate state
  :start (start (mount/args))
  :stop (stop state))

(defn trigger-sync
  []
  (let [{:keys [work>]} state]
    (println "Sync triggered")
    (>!! work> [:sync])))
