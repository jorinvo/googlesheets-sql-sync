(ns googlesheets-sql-sync.worker
  (:require
   [clojure.core.async :as async :refer [<! go go-loop]]
   [clojure.java.browse :refer [browse-url]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.db :as db]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.metrics :as metrics]
   [googlesheets-sql-sync.oauth :as oauth]
   [googlesheets-sql-sync.sheets :as sheets]))

(defn- show-init-message
  "Prompt user to visit auth URL.
  When running locally, open browser automatically."
  [cfg]
  (let [url (oauth/url cfg)]
    (log/info "Please visit the oauth url in your browser:\n" url)
    (when (oauth/local-redirect? cfg)
      (browse-url url))))

(defn- do-sync [{:keys [config-file no-server timeout> throttler] :as ctx}]
  (try
    (let [cfg (config/get config-file)]
      (try
        (if-let [token (oauth/refresh-token ctx)]
          (do
            (->> (:sheets cfg)
                 (map #(sheets/get-rows % token throttler))
                 (run! #(db/update-table cfg %)))
            (log/info "Sync done"))
          (if no-server
            (do
              (log/error "Cannot authenticate when server is disabled")
              :not-ok)
            (show-init-message cfg)))
        (catch Exception e (log/error (.getMessage e) "\nSync failed")))
      (log/info "Next sync in" (interval/->string (:interval cfg)))
      (async/put! timeout> (:interval cfg)))
    (metrics/count-sync ctx)
    (catch Exception e (do (log/error "Failed reading config file" (.getMessage e))
                           :not-ok))))

(defn auth-only [{:keys [config-file work>] :as ctx}]
  (let [cfg (config/get config-file)]
    (if (oauth/refresh-token ctx)
      (log/info "Already authenticated")
      (go
        (show-init-message cfg)
        (when-let [[job code] (<! work>)]
          (when (= :code job)
            (oauth/handle-code (merge ctx {:code code}))))))))

(defn start [{:keys [work>] :as ctx}]
  (log/info "Starting worker")
  (async/put! work> [:sync])
  (go-loop []
    (if-let [[job code] (<! work>)]
      (do (when (= :code job)
            (oauth/handle-code (merge ctx {:code code})))
          (if (:not-ok (do-sync ctx))
            :not-ok
            (recur)))
      (log/info "Stopping worker"))))
