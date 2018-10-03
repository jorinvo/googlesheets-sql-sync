(ns googlesheets-sql-sync.system
  (:require
   [clojure.core.async :as async :refer [<! >!! <!! chan close! dropping-buffer go-loop]]
   [clojure.java.browse :refer [browse-url]]
   [mount.core :as mount]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.db :as db]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.oauth :as oauth]
   [googlesheets-sql-sync.sheets :as sheets]
   [googlesheets-sql-sync.util :refer [fail]]))

(defn- show-init-message
  "Prompt user to visit auth URL.
  When running locally, open browser automatically."
  [cfg]
  (let [url (oauth/url cfg)]
    (log/info "Please visit the oauth url in your browser:\n" url)
    (when (oauth/local-redirect? cfg)
      (browse-url url))))

(defn- do-sync [{:keys [auth-only config-file auth-file no-server timeout> sys-exit] :as ctx}]
  (try
    (let [cfg (config/get config-file)]
      (try
        (if-let [token (oauth/refresh-token ctx)]
          (if auth-only
            (do (log/info "Authentication done")
                (sys-exit 0))
            (do
              (->> (:sheets cfg)
                   (map #(sheets/get-rows % token))
                   (run! #(db/update-table cfg %)))
              (log/info "Sync done")))
          (if no-server
            (do
              (log/error "Cannot authenticate when server is disabled")
              (sys-exit 1))
            (show-init-message cfg)))
        (catch Exception e (log/error (.getMessage e) "\nSync failed")))
      (log/info "Next sync in" (interval/->string (:interval cfg)))
      (async/put! timeout> (interval/->ms (:interval cfg))))
    (catch Exception e (do (log/error "Failed reading config file" (.getMessage e))
                           (sys-exit 1)))))

(defn- start-worker [{:keys [config-file work>] :as ctx}]
  (log/info "Starting worker")
  (async/put! work> [:sync])
  (go-loop []
    (if-let [[job code] (<! work>)]
      (do (case job
            :sync (do-sync ctx)
            :code (do
                    (oauth/handle-code (merge ctx {:code code}))
                    (do-sync ctx)))
          (recur))
      (log/info "Stopping worker"))))

(defn start
  [options]
  (try
    (let [timeout> (chan (dropping-buffer 1))
          work>    (chan)
          ctx      {:work>    work>
                    :timeout> timeout>}]
      (interval/connect-timeouts ctx)
      (assoc ctx :worker> (start-worker (merge options ctx))))
    (catch Exception e (do (log/error "Error while starting:" (.getMessage e))
                           ((:sys-exit options) 1)))))

(defn stop
  "Stops system. If system is not running, nothing happens."
  [{:keys [timeout> work>]}]
  (log/info "\nShutting down")
  (when timeout>
    (close! timeout>))
  (when work>
    (close! work>)))

(mount/defstate state
  :start (start (mount/args))
  :stop (stop state))

(defn trigger-sync
  []
  (when-let [{:keys [work>]} state]
    (do
      (log/info "Sync triggered")
      (>!! work> [:sync]))))

(defn wait
  []
  (when-let [{:keys [worker>]} state]
    (<!! worker>)))
