(ns googlesheets-sql-sync.oauth
  (:require
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.http :as http]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.util :refer [query-string hostname]]))

(def user-oauth-url   "https://accounts.google.com/o/oauth2/v2/auth")
(def server-oauth-url "https://www.googleapis.com/oauth2/v4/token")
(def scope            "https://www.googleapis.com/auth/spreadsheets.readonly")

(def default-params-user {:scope          scope
                          :access_type    "offline"
                          :response_type  "code"})

(def default-params-code {:grant_type "authorization_code"})

(def defaul-params-refresh {:grant_type "refresh_token"})

(defn url [c]
  (let [q (->> (select-keys (:google_credentials c) [:client_id :redirect_uri])
               (merge default-params-user)
               query-string)]
    (str user-oauth-url "?" q)))

(defn local-redirect? [cfg]
  (-> cfg
      :google_credentials
      :redirect_uri
      hostname
      (= "localhost")))

(defn- get-access-token [creds params]
  (let [p (merge (select-keys creds [:client_id :client_secret :redirect_uri])
                 params)]
    (-> (http/post server-oauth-url {:form-params p})
        (select-keys [:access_token :expires_in :refresh_token]))))

(defn handle-code
  [{:keys [code config-file auth-file]}]
  (log/info "Handling auth code")
  (try
    (let [creds (:google_credentials (config/get config-file))
          params (merge default-params-code {:code code})
          auth (get-access-token creds params)]
      (config/save-auth auth-file auth))
    (catch Exception e (log/error "Errror handling code:" (.getMessage e)))))

(defn refresh-token
  "Refreshes access token, saves the new one and returns it."
  [{:keys [config-file auth-file]}]
  (log/info "Refreshing access token")
  (let [{:keys [refresh_token] :as auth} (config/get-auth auth-file)]
    (if refresh_token
      (try
        (let [creds (:google_credentials (config/get config-file))
              params (merge defaul-params-refresh
                            {:refresh_token refresh_token})
              new-auth (get-access-token creds params)]
          (config/save-auth auth-file (merge auth new-auth))
          (:access_token new-auth))
        (catch Exception e (do (log/error "Errror handling code:" (.getMessage e))
                               (log/info "Please reauthorize app"))))
      (log/info "No refresh_token found"))))
