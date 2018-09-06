(ns googlesheets-sql-sync.oauth
  (:require
   [clj-http.client :as http]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.util :refer [try-http]]))

(def user-oauth-url   "https://accounts.google.com/o/oauth2/v2/auth")
(def server-oauth-url "https://www.googleapis.com/oauth2/v4/token")
(def scope            "https://www.googleapis.com/auth/spreadsheets.readonly")

(def default-params-user {:scope          scope
                          :access_type    "offline"
                          :response_type  "code"})

(def default-params-code {:grant_type "authorization_code"})

(def defaul-params-refresh {:grant_type "refresh_token"})

(defn get-url [c]
  (->> (select-keys (:google_credentials c) [:client_id :redirect_uri])
       (merge default-params-user)
       (http/generate-query-string)
       (str user-oauth-url "?")))

(defn- fetch-access-token [creds params]
  (let [p (merge (select-keys creds [:client_id :client_secret :redirect_uri])
                 params)]
    (try-http
     "fetch access token"
     (-> (http/post server-oauth-url {:form-params p :as :json})
         :body
         (select-keys [:access_token :expires_in :refresh_token])))))

(defn handle-code [config-file code]
  (when code
    (println "handle auth code")
    (config/merge-file
     config-file
     :google_credentials
     #(fetch-access-token % (merge default-params-code {:code code})))))

(defn refresh-token [config-file]
  (println "refresh access token")
  (if (-> config-file config/read-file :google_credentials :refresh_token)
    (try (-> (config/merge-file
              config-file
              :google_credentials
              (fn [creds]
                (->> (merge defaul-params-refresh
                            (select-keys creds [:refresh_token])
                            (fetch-access-token creds)))))
             :google_credentials :access_token)
         (catch Exception e (println "errror handling code" (.getMessage e))))
    (println "No refresh_token found.")))
