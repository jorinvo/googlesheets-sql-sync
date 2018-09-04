(ns googlesheets-sql-sync.cli
  (:require
    [googlesheets-sql-sync.config :as config]
    [googlesheets-sql-sync.system :as system]
    [signal.handler :as signal]))

(def usage "

  Generate a config file using:

    java -jar googlesheets_sql_sync.jar <client-id> <client-secret>

  Fill out the config file

  Then run:

    java -jar googlesheets_sql_sync.jar googlesheets_sql_sync.json

  Follow setup instructions

")

(defn run
  [args]
  (let [a1 (first args)
        a2 (second args)
        c (count args)]
    (cond
      (and (= "init" a1) (<= c 2))
      (config/generate a2)
      (= c 1)
      (let [stop-system (system/start a1)]
        (signal/with-handler :term (stop-system))
        (signal/with-handler :int (stop-system)))
      :else (do (println usage) :not-ok))))
