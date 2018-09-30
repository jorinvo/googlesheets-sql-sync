(ns googlesheets-sql-sync.core
  (:require
   [clojure.string :as string]
   [clojure.tools.cli :refer [parse-opts]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.system :as system]
   [googlesheets-sql-sync.web]
   [googlesheets-sql-sync.util :refer [valid-port?]]
   [mount.core :as mount]
   [signal.handler :as signal])
  (:gen-class))

(def usage "

  1. Generate a config file by running:

    java -jar googlesheets_sql_sync.jar --init

  2. Fill out the config file.

  3. Then run:

    java -jar googlesheets_sql_sync.jar

  4. Follow setup instructions and you are good to go.

")

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 9955
    :parse-fn #(Integer/parseInt %)
    :validate [valid-port? "Must be a number between 0 and 65536"]]
   ["-c" "--config-file PATH" "Config file path"
    :default "googlesheets_sql_sync.json"]
   [nil "--oauth-route" "Set URL route path to used in OAuth redirect URL."
    :default "/oauth"]
   [nil "--api-rate-limit" "Max interval calling Google API in ms"
    :default 1000
    :parse-fn #(Integer/parseInt %)
    :validate [pos-int? "Must be a positive integer"]]
   [nil "--init" "Initialize a new config file"]
   [nil "--auth-only" "Setup authentication, then quit. Don't sync."]
   [nil "--no-server" "Disable server. Disables authentication and metrics."]
   ["-h" "--help"]])

(defn- print-list [errs]
  (run! println errs))

(defn- invalid-flags
  "Make sure combination of flags is valid."
  [options]
  (let [bad [[:init :auth-only]
             [:init :no-server]
             [:auth-only :no-server]]
        errs (->> bad
                  (filter #(every? options %))
                  (map #(str "Sorry, you cannot combine flags "
                             (string/join " and " (map name %))
                             ".")))]
    (when-not (empty? errs) errs)))

(comment
  (= (invalid-flags {:init true :auth-only true})
     '("Sorry, you cannot combine flags init and auth-only."))
  (= (invalid-flags {})
     nil))

(defn- print-usage [{:keys [summary]}]
  (println usage)
  (println summary))

(defn- handle-signals
  "Connect OS signals with system"
  []
  (signal/with-handler :term (mount/stop))
  (signal/with-handler :int (mount/stop))
  (signal/with-handler :alrm (system/trigger-sync)))

(defn -main
  "Handles args parsing and does the appropriate action."
  [& args]
  (let [opts (parse-opts args cli-options)
        options (:options opts)
        errs (:errors opts)
        flag-errs (invalid-flags options)]
    (cond
      errs            (do (print-list errs) (System/exit 1))
      flag-errs       (do (print-list flag-errs) (System/exit 1))
      (:help options) (print-usage opts)
      (:init options) (config/generate options)
      :else           (do (-> (mount/with-args options)
                              (mount/except (if (:no-server options)
                                              (do (println "Server disabled")
                                                  [#'googlesheets-sql-sync.web/server])
                                              []))
                              (mount/start))
                          (handle-signals)))))
