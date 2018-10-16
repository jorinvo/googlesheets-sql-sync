(ns googlesheets-sql-sync.cli
  (:require
   [clojure.string :as string]
   [clojure.tools.cli :refer [parse-opts]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.core :as core]
   [googlesheets-sql-sync.options :as options]
   [signal.handler :as signal])
  (:gen-class))

(def usage "Keep your SQL database in sync with Google Sheets.

Use this to let users manually insert data using Google Sheets
while having the power of all available SQL tooling for further processing.

For more, have a look in the README at https://github.com/jorinvo/googlesheets-sql-sync
")

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
  (let [opts      (parse-opts args (options/cli))
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
