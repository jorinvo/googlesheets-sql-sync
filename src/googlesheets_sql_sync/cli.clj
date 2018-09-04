(ns googlesheets-sql-sync.cli
  (:require
    [googlesheets-sql-sync.config :as config]
    [googlesheets-sql-sync.system :as system]))

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
      (and (= "init" a1) (<= c 2)) (if (config/generate a2)
                                     :ok
                                     :not-ok)
      (= c 1) (do (system/start a1)
                  :ok)
      :else (do (println usage)
                :not-ok))))
