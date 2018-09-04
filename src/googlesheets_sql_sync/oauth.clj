(ns googlesheets-sql-sync.oauth
  (:require
   [clj-http.client :as http]
   [googlesheets-sql-sync.config :as config]))

(def user-oauth-url   "https://accounts.google.com/o/oauth2/v2/auth")
(def server-oauth-url "https://www.googleapis.com/oauth2/v4/token")
(def scope            "https://www.googleapis.com/auth/spreadsheets.readonly")

(def default-params-user {:scope          scope
                          :access_type    "offline"
                          :response_type  "code"})

(def default-params-code   {:grant_type "authorization_code"})

(def defaul-params-refresh {:grant_type "refresh_token"})

(defn get-url [config]
  (->> (select-keys (:google_credentials config) [:client_id :redirect_uri])
       (merge default-params-user)
       (http/generate-query-string)
       (str user-oauth-url "?")))

(defn- fetch-access-token [creds params]
  (println "fetching access token")
  (let [p (merge (select-keys creds [:client_id :client_secret :redirect_uri])
                 params)]
    (-> (http/post server-oauth-url {:form-params p :as :json})
        :body
        (select-keys [:access_token :expires_in :refresh_token]))))

(defn handle-code [config-file-path c]
  (when c
    (config/update-file
     config-file-path
     :google_credentials
     #(fetch-access-token % (merge default-params-code {:code c})))))

(defn handle-refresh-token [config-file-path]
  (println "refresh access token")
  (config/update-file
   config-file-path
   :google_credentials
   (fn [creds]
     (->> (merge defaul-params-refresh
                 (select-keys creds [:refresh_token]))
          (fetch-access-token creds)))))
