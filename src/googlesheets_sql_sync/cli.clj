(ns googlesheets-sql-sync.cli
  (:require
   [clojure.tools.cli :refer [parse-opts]]
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

(defn print-usage [opts]
  (println usage (:summary opts)))

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 9955
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-c" "--config-file PATH" "Config file path"
    :default "googlesheets_sql_sync.json"]
   [nil "--init" "Initialize a new config file"]
   [nil "--auth-only" "Setup authentication, then quit. Don't sync."]
   ["-h" "--help"]])

(defn run
  [args]
  (let [opts (parse-opts args cli-options)
        ctx (:options opts)
        errs (:errors opts)]
    (cond
      errs
      (do (run! #(println %) errs)
          :not-ok)

      (or (:help ctx) (and (:init ctx) (:auth ctx)))
      (print-usage opts)

      (:init ctx)
      (config/generate ctx)

      :else
      (let [system (system/start ctx)]
        (signal/with-handler :term (system/stop system))
        (signal/with-handler :int (system/stop system))
        (signal/with-handler :alrm (system/trigger-sync system))))))
