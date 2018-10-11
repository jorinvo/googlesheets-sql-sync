(ns dev
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [googlesheets-sql-sync.core :as core]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(def options {:port 9955
              :config-file "googlesheets_sql_sync.json"
              :auth-file "googlesheets_sql_sync.auth.json"
              ; :no-server true
              ; :no-metrics true
              ; :auth-only true
              :oauth-route "/oauth"
              :metrics-route "/metrics"
              :api-rate-limit 4000})

(def system)

(defn start []
  (alter-var-root #'system (constantly (core/start options))))

(defn stop []
  (alter-var-root #'system #(do (core/stop %) nil)))

(comment
  (config/generate options)
  (start)
  (stop)
  (core/trigger-sync system)
  (core/wait system))
