(ns googlesheets-sql-sync.options
  (:require
    [clojure.string :as string]
    [googlesheets-sql-sync.util :refer [valid-port? valid-url?]]))

(def settings [:port             {:desc     "Port number"
                                  :default  9955
                                  :validate [valid-port? "Must be an integer between 0 and 65536"]
                                  :parse-fn #(Integer/parseInt %)}
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
                                  :validate [pos-int? "Must be a positive integer"]
                                  :parse-fn #(Integer/parseInt %)}
               :user-oauth-url   {:desc     "URL to prompt user with"
                                  :default  "https://accounts.google.com/o/oauth2/v2/auth"
                                  :validate [valid-url? "Must be a valid URL"]}
               :server-oauth-url {:desc     "URL to prompt user with"
                                  :default  "https://www.googleapis.com/oauth2/v4/token"
                                  :validate [valid-url? "Must be a valid URL"]}
               :auth-only        {:desc     "Setup authentication, then quit, don't sync"}
               :single-sync      {:desc     "Sync once only, then quit"}
               :no-server        {:desc     "Disable server, disables authentication and metrics"}
               :no-metrics       {:desc     "Disable metrics"}])

(defn cli []
  (vec (concat
     [[nil "--init" "Initialize a new config file, don't start system"]
      ["-h" "--help" "Print help"]]
     (map (fn [[k v]]
            (vec (apply concat
                        (let [value (when (:default v)
                                      (str " " (string/upper-case (name k))))
                              flag (str "--" (name k) value)]
                          [nil flag (:desc v)])
                        (dissoc v :desc))))
          (partition 2 settings)))))

(defn defaults []
  (->> settings
       (partition 2)
       (map (fn [[k {v :default}]] {k v}))
       (into {})))

(defn validate [opts]
  (let [validators (->> settings
                        (partition 2)
                        (map (fn [[k {v :validate}]] {k v}))
                        (into {}))]
    (->> opts
         (map (fn [[k v]]
                (if-let [[validator err] (validators k)]
                  (when-not (validator v)
                    {k err}))))
         (into {})
         (#(if (empty? %) nil %)))))

