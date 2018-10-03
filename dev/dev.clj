(ns dev
  (:require
   [clojure.core.async :as async :refer [<! >! >!! chan close! dropping-buffer go go-loop pipeline-async timeout]]
   [clojure.java.browse :refer [browse-url]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.java.io :as io]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [org.httpkit.client :as http-client]
   [jsonista.core :as json]
   [expound.alpha :as expound]
   [mount.core :as mount]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.db :as db]
   [googlesheets-sql-sync.http :as http]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.oauth :as oauth]
   [googlesheets-sql-sync.sheets :as sheets]
   [googlesheets-sql-sync.system :as system]
   [googlesheets-sql-sync.throttle :as throttle]
   [googlesheets-sql-sync.util :as util]
   [googlesheets-sql-sync.web :as web]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(def options {:port 9955
              :config-file "googlesheets_sql_sync.json"
              :auth-file "googlesheets_sql_sync.auth.json"
              ; :auth-only true
              :oauth-route "/oauth"
              :api-rate-limit 4000})

(comment
  (config/generate options)
  (mount/stop)
  (mount/start-with-args options)
  (mount/find-all-states)
  (mount/running-states))
