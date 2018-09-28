(ns googlesheets-sql-sync.core
  (:require
   [clojure.string :as string]
   [clojure.tools.cli :refer [parse-opts]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.system :as system]
   [signal.handler :as signal])
  (:gen-class))

(def usage "

  Generate a config file using:

    java -jar googlesheets_sql_sync.jar <client-id> <client-secret>

  Fill out the config file

  Then run:

    java -jar googlesheets_sql_sync.jar googlesheets_sql_sync.json

  Follow setup instructions

")

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 9955
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-c" "--config-file PATH" "Config file path"
    :default "googlesheets_sql_sync.json"]
   [nil "--oauth-route" "Set URL route path to used in OAuth redirect URL."
    :default "/oauth"]
   [nil "--init" "Initialize a new config file"]
   [nil "--auth-only" "Setup authentication, then quit. Don't sync."]
   [nil "--no-server" "Disable server. Disables authentication and metrics."]
   ["-h" "--help"]])

(defn- print-list [errs]
  (run! println errs))

(defn- invalid-flags
  "Make sure combination of flags is valid."
  [ctx]
  (let [bad [[:init :auth-only]
             [:init :no-server]
             [:auth-only :no-server]]
        errs (->> bad
                  (filter #(every? ctx %))
                  (map #(str "Sorry, you cannot combine flags "
                             (string/join " and " (map name %))
                             ".")))]
    (when-not (empty? errs) errs)))

(comment
  (invalid-flags {:init true :auth-only true})
  (invalid-flags {:init true :auth-only true :no-server true})
  (invalid-flags {}))

(defn- print-usage [{:keys [summary]}]
  (println usage summary))

(defn- handle-signals
  "Connect OS signals with system"
  [ctx]
  (signal/with-handler :term (system/stop ctx))
  (signal/with-handler :int (system/stop ctx))
  (signal/with-handler :alrm (system/trigger-sync ctx)))

(defn- run
  "Call from your main function with the args list.
  Handles args parsing and does the appropriate action.
  Returns :not-ok on error, else nil."
  [args]
  (let [opts (parse-opts args cli-options)
        ctx (:options opts)
        errs (:errors opts)
        flag-errs (invalid-flags ctx)]
    (cond
      errs        (do (print-list errs) :not-ok)
      flag-errs   (do (print-list flag-errs) :not-ok)
      (:help ctx) (print-usage opts)
      (:init ctx) (config/generate ctx)
      :else       (-> ctx system/start handle-signals))))

(defn -main
  [& args]
  (when (= (run args) :not-ok)
    (System/exit 1)))
