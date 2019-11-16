(ns googlesheets-sql-sync.core
  (:require
   [clojure.core.async :refer [>!! <! <!! chan close! go]]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.throttle :as throttle]
   [googlesheets-sql-sync.web :as web]
   [googlesheets-sql-sync.metrics :as metrics]
   [googlesheets-sql-sync.worker :as worker]))

(defn stop
  "Stops system. If system is not running, nothing happens."
  [{:keys [timeout> work> stop-server]}]
  (log/info "\nShutting down")
  (when stop-server
    (stop-server))
  (when timeout>
    (close! timeout>))
  (when work>
    (close! work>)))

(defn start
  "Build a system from options, start it and return it."
  [{:as options :keys [api-rate-limit auth-only no-metrics no-server single-sync]}]
  (try
    (let [ctx (assoc options
                     :work>     (chan)
                     :throttler (throttle/make api-rate-limit))]
      (if auth-only
        (let [stop-server (web/start ctx)]
          (assoc ctx
               :stop-server stop-server
               :worker> (go (when-let [c (worker/auth-only ctx)]
                              (<! c))
                            (stop-server))))
        (cond-> ctx
          (not single-sync)
          (assoc :timeout>
                 (interval/create-timeout> (:work> ctx) [:sync]))

          single-sync
          (assoc :timeout>
                 (chan))

          (not (or no-server no-metrics))
          (metrics/init)

          (not no-server)
          (#(assoc % :stop-server (web/start %)))

          true
          (#(assoc % :worker> (worker/start %)))

          single-sync
          (#(do (go (<! (:timeout> %))
                    (stop %))
                %)))))
    (catch Exception e (do (log/error "Error while starting:" (.getMessage e))
                           :not-ok))))

(defn trigger-sync
  "Tell system to sync, but doesn't wait for sync to complete."
  [{:keys [work>]}]
  (when work>
    (log/info "Sync triggered")
    (>!! work> [:sync])))

(defn wait
  "Block until system is stopped"
  [{:as ctx :keys [worker>]}]
  (if worker>
    (<!! worker>)
    ctx))
