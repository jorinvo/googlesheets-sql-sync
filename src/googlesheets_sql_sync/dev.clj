(ns googlesheets-sql-sync.dev
  (:require
   [clojure.core.async :as async :refer [<! >! >!! chan close! dropping-buffer go go-loop pipeline-async timeout]]
   [clojure.java.browse :refer [browse-url]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.java.io :as io]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [clj-http.client :as clj-http]
   [cheshire.core :as json]
   [expound.alpha :as expound]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.db :as db]
   [googlesheets-sql-sync.http :as http]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.oauth :as oauth]
   [googlesheets-sql-sync.sheets :as sheets]
   [googlesheets-sql-sync.system :as system]
   [googlesheets-sql-sync.web :as web]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(def s nil)

(defn run []
  (when s
    (system/stop s))
  (def s (system/start {:port 9955 :config-file "googlesheets_sql_sync.json" :auth-only true})))

(comment
  (run))
