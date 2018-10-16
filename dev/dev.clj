(ns dev
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [googlesheets-sql-sync.core :as core]
   [googlesheets-sql-sync.options :as options]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(def system)

(defn start []
  (alter-var-root #'system (constantly (core/start (options/defaults)))))

(defn stop []
  (alter-var-root #'system #(do (core/stop %) nil)))

(comment
  (start)
  (stop)
  (core/trigger-sync system)
  (core/wait system))
