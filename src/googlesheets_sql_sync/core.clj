(ns googlesheets-sql-sync.core
  (:require
   [clojure.core.async :refer [>!! <!! chan close!]]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.throttle :as throttle]
   [googlesheets-sql-sync.web :as web]
   [googlesheets-sql-sync.worker :as worker]))

(defn start
  "Build a system from options, start it and return it."
  [options]
  (try
    (let [ctx (assoc options
                     :work>     (chan)
                     :throttler (throttle/make (:api-rate-limit options)))]
      (if (:auth-only options)
        (assoc ctx :worker> (worker/auth-only ctx))
        (-> ctx
            (#(assoc % :timeout> (interval/create-timeout> (:work> %) [:sync])))
            (#(if (:no-server %)
                %
                (assoc % :stop-server (web/start %))))
            (#(assoc % :worker> (worker/start %))))))
    (catch Exception e (do (log/error "Error while starting:" (.getMessage e))
                           ((:sys-exit options) 1)))))

(defn stop
  "Stops system. If system is not running, nothing happens."
  [{:keys [timeout> work> stop-server] :as ctx}]
  (log/info "\nShutting down")
  (when stop-server
    (stop-server))
  (when timeout>
    (close! timeout>))
  (when work>
    (close! work>)))

(defn trigger-sync
  "Tell system to sync, but doesn't wait for sync to complete."
  [{:keys [work>]}]
  (when work>
    (log/info "Sync triggered")
    (>!! work> [:sync])))

(defn wait
  "Block until system is stopped"
  [{:keys [worker>]}]
  (when worker>
    (<!! worker>)))
