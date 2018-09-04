(ns googlesheets-sql-sync.core
  (:require
    [googlesheets-sql-sync.cli :as cli])
  (:gen-class))

(defn -main
  [& args]
  (when (= (cli/run args) :not-ok)
    (System/exit 1)))
