(ns googlesheets-sql-sync.cli
  (:require
   [clojure.string :as string]
   [clojure.tools.cli :refer [parse-opts]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.core :as core]
   [googlesheets-sql-sync.util :refer [valid-port? valid-url?]]
   [signal.handler :as signal])
  (:gen-class))

(def usage "Keep your SQL database in sync with Google Sheets.

Use this to let users manually insert data using Google Sheets
while having the power of all available SQL tooling for further processing.

For more, have a look in the README at https://github.com/jorinvo/googlesheets-sql-sync
")

; validate
; to cli opts
; defaults
(comment {:port             {:desc     "Port number"
                             :default  9955
                             :validate [valid-port? "Must be an integer between 0 and 65536"]}
          :config-file      {:desc     "Config file path"
                             :default  "googlesheets_sql_sync.json"}
          :auth-file        {:desc     "File path to store Google auth secrets, file is updated on sync"
                             :default  "googlesheets_sql_sync.auth.json"}
          :oauth-route      {:desc     "Route to use in OAuth redirect URL"
                             :default  "/oauth"}
          :metrics-route    {:desc     "Route to serve metrics at"
                             :default  "/metrics"}
          :api-rate-limit   {:desc     "Max interval calling Google API in ms"
                             :default  1000
                             :validate [pos-int? "Must be a positive integer"]}
          :user-oauth-url   {:desc     "URL to prompt user with"
                             :default  "https://accounts.google.com/o/oauth2/v2/auth"}
          :server-oauth-url {:desc     "URL to prompt user with"
                             :default  "https://www.googleapis.com/oauth2/v4/token"}
          :no-server        {:desc     "Disable server, disables authentication and metrics"
                             :default  false}})

(def cli-options
  ;; An option with a required argument
  [[nil "--port PORT" "Port number"
    :default 9955
    :parse-fn #(Integer/parseInt %)
    :validate [valid-port? "Must be a number between 0 and 65536"]]
   [nil "--config-file PATH" "Config file path"
    :default "googlesheets_sql_sync.json"]
   [nil "--auth-file PATH" "File path to store Google auth secrets, file is updated on sync"
    :default "googlesheets_sql_sync.auth.json"]
   [nil "--oauth-route" "Route to use in OAuth redirect URL"
    :default "/oauth"]
   [nil "--metrics-route" "Route to serve metrics at"
    :default "/metrics"]
   [nil "--api-rate-limit" "Max interval calling Google API in ms"
    :default 1000
    :parse-fn #(Integer/parseInt %)
    :validate [pos-int? "Must be a positive integer"]]
   [nil "--user-oauth-url" "URL to prompt user with"
    :default "https://accounts.google.com/o/oauth2/v2/auth"
    :validate [valid-url? "Must be a valid URL"]]
   [nil "--server-oauth-url " "URL to prompt user with"
    :default "https://www.googleapis.com/oauth2/v4/token"
    :validate [valid-url? "Must be a valid URL"]]
   [nil "--init" "Initialize a new config file"]
   [nil "--auth-only" "Setup authentication, then quit, don't sync"]
   [nil "--no-server" "Disable server, disables authentication and metrics"]
   [nil "--no-metrics" "Disable metrics"]
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
  [system]
  (signal/with-handler :term (core/stop system))
  (signal/with-handler :int (core/stop system))
  (signal/with-handler :alrm (core/trigger-sync system)))

(defn -main
  "Handles args parsing and does the appropriate action."
  [& args]
  (let [opts      (parse-opts args cli-options)
        options   (:options opts)
        errs      (:errors opts)
        flag-errs (invalid-flags options)]
    (cond
      errs            (do (print-list errs) (System/exit 1))
      flag-errs       (do (print-list flag-errs) (System/exit 1))
      (:help options) (print-usage opts)
      (:init options) (try (config/generate options)
                           (catch Exception e (do (println (.getMessage e))
                                                  (System/exit 1))))
      :else           (let [system (core/start options)]
                        (handle-signals system)
                        (when (= :not-ok (core/wait system)) (System/exit 1))))))
