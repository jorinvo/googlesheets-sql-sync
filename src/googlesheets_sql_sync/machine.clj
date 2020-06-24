(ns googlesheets-sql-sync.machine)

(def defaul-params-refresh {:grant_type "refresh_token"})

(defn process [state]
  (cond
    (not (contains? state :config))
    [[:config :get-config (:config-file state)]]

    (not (contains? state :auth))
    [[:auth :get-auth-data (:auth-file state)]]

    (and (not (contains? state :new-auth-data))
         (:config state)
         (-> state :auth :refresh_token))
    [[nil :log "Refreshing access token"]
     [:new-auth-data :talk-to-google {:url (:server-oauth-url state)
                                      :form-params (-> (-> state :config :google_credentials)
                                                       (select-keys [:client_id :client_secret :redirect_uri])
                                                       (merge defaul-params-refresh
                                                              {:refresh_token (-> state :auth :refresh_token)}))}
      ]]

    (and (:auth state)
         (not (-> state :auth :refresh_token)))
    (if (:no-server state)
      [[nil :log "No refresh_token found"]
       [nil :log "Cannot authenticate when server is disabled"]
       [nil :fail]])

    ; TODO handle error in new auth data
    ; (catch Exception e (do (log/error "Errror handling code:" (.getMessage e))
                                   ; (log/info "Please reauthorize app")))

    (and (:auth state)
         (:new-auth-data state)
         (not (contains? state :saved-new-auth-data)))
    [[:saved-new-auth-data :save-auth-data (:auth-file state) (merge (:auth state)
                                                                     (:new-auth-data state))]]

    (and (:new-auth-data state)
         (contains? state :saved-new-auth-data)
         (not (-> state :new-auth-data :access_token))
         (not (:no-server state)))
    ;TODO
    []

    ))

(defn execute [handlers request]
  (loop [state request]
    (if-let [actions (process state)]
      (recur (->> actions
                  (map (fn [[field action & args]]
                         (if-let [handler (get handlers action)]
                           (let [res (apply handler args)]
                             (if (nil? field)
                               {}
                               {field res}))
                           (throw (Exception. (str "no handler for " action))))))
                  (reduce merge state)))
      state)))

(defn execute-and-capture-actions
  "Wraps execute fn and captures all executed actions in result meta"
  [handlers request]
  (let [actions (atom [])
        wrapped-handlers (reduce (fn [x [k v]]
                                   (assoc x k (fn [& args]
                                                (swap! actions #(conj % [k args]))
                                                (apply v args))))
                                 {}
                                 handlers)]
    (with-meta (execute wrapped-handlers request)
               {:actions @actions})))

(comment

  (let [result (execute-and-capture-actions
                 {:get-config (fn [_] :this-is-the-config)
                  :get-auth-data (fn [_] {:refresh_token :refresh-token})
                  :talk-to-google (fn [x] x)
                  :save-auth-data (fn [_ _] :saved)
                  :log (fn [_] nil)}
                 nil)]
    [result (meta result)])

)
