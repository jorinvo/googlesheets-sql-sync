(ns googlesheets-sql-sync.oauth
  (:require
   [clj-http.client :refer [generate-query-string parse-url]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.http :as http]))

(def user-oauth-url   "https://accounts.google.com/o/oauth2/v2/auth")
(def server-oauth-url "https://www.googleapis.com/oauth2/v4/token")
(def scope            "https://www.googleapis.com/auth/spreadsheets.readonly")

(def default-params-user {:scope          scope
                          :access_type    "offline"
                          :response_type  "code"})

(def default-params-code {:grant_type "authorization_code"})

(def defaul-params-refresh {:grant_type "refresh_token"})

(defn url [c]
  (->> (select-keys (:google_credentials c) [:client_id :redirect_uri])
       (merge default-params-user)
       generate-query-string
       (str user-oauth-url "?")))

(defn local-redirect? [c]
  (-> c
      :google_credentials
      :redirect_uri
      parse-url
      :server-name
      (= "localhost")))

(defn- get-access-token [creds params]
  (let [p (merge (select-keys creds [:client_id :client_secret :redirect_uri])
                 params)]
    (-> (http/post "fetch access token" server-oauth-url {:form-params p :as :json})
        :body
        (select-keys [:access_token :expires_in :refresh_token]))))

(defn handle-code [{:keys [config-file]} code]
  (println "Handling auth code")
  (try
    (config/merge-file
     config-file
     :google_credentials
     #(get-access-token % (merge default-params-code {:code code})))
    (catch Exception e (println "errror handling code" (.getMessage e)))))

(defn refresh-token [config-file]
  (println "Refreshing access token")
  (if (-> config-file config/read-file :google_credentials :refresh_token)
    (try (-> (config/merge-file
              config-file
              :google_credentials
              (fn [creds]
                (let [params (merge defaul-params-refresh
                                    (select-keys creds [:refresh_token]))]
                  (get-access-token creds params))))
             :google_credentials :access_token)
         (catch Exception e (println "errror handling code:" (.getMessage e))))
    (println "No refresh_token found")))
