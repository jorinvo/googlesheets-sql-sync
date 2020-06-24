(ns googlesheets-sql-sync.worker
  (:require
   [clojure.core.async :as async :refer [<! go go-loop]]
   [clojure.java.browse :refer [browse-url]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.machine :as machine]
   [googlesheets-sql-sync.db :as db]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.metrics :as metrics]
   [googlesheets-sql-sync.oauth :as oauth]
   [googlesheets-sql-sync.auth-file :as auth-file]
   [googlesheets-sql-sync.sheets :as sheets]))

(defn- show-init-message
  "Prompt user to visit auth URL.
  When running locally, open browser automatically."
  [google_credentials user-oauth-url]
  (let [url (oauth/url google_credentials user-oauth-url)]
    (log/info "Please visit the oauth url in your browser:\n" url)
    (when (oauth/local-redirect? google_credentials)
      (browse-url url))))

(defn- do-sync
  "Authenticate, fetch data and update DB"
  [ctx]
  (machine/execute
    {:get-config config/get
     :get-auth-data auth-file/get}
    ctx))

(comment defn- do-sync
  "Authenticate, fetch data and update DB"
  [{:as ctx :keys [config-file no-server timeout> throttler user-oauth-url single-sync]}]
  (try
    (let [cfg (config/get config-file)]
      (try
        (if-let [token (oauth/refresh-token ctx)]
          (do
            (run! #(let [sheet (sheets/get-rows % token throttler)]
                     (db/update-table cfg sheet))
                  (:sheets cfg))
            (log/info "Sync done")
            (metrics/count-sync ctx))
          (if no-server
            (do
              (log/error "Cannot authenticate when server is disabled")
              :not-ok)
            (show-init-message (:google_credentials cfg) user-oauth-url)))
        (catch Exception e (log/error (.getMessage e) "\nSync failed")))
      (when-not single-sync
        (log/info "Next sync in" (interval/->string (:interval cfg))))
      (async/put! timeout> (:interval cfg)))
    (catch Exception e (do (log/error "Failed reading config file" (.getMessage e))
                           :not-ok))))

(defn auth-only
  "Authenticate async and exit once done.
  Returns channel that gets value once done."
  [{:keys [config-file user-oauth-url work>] :as ctx}]
  (let [cfg (config/get config-file)]
    (if (oauth/refresh-token ctx)
      (log/info "Already authenticated")
      (go
        (show-init-message (:google_credentials cfg) user-oauth-url)
        (when-let [[job code] (<! work>)]
          (when (= :code job)
            (oauth/handle-code (merge ctx {:code code}))))
        (log/info "Authentication done")))))

(defn start
  "Start system.
  Returns channel that gets value once done.
  Channel gets :not-ok on failure."
  [{:keys [work>] :as ctx}]
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
