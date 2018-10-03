(ns googlesheets-sql-sync.web
  (:require
   [clojure.core.async :as async]
   [mount.core :as mount]
   [org.httpkit.server :refer [run-server]]
   [ring.middleware.params :refer [wrap-params]]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.system :as system]
   [googlesheets-sql-sync.util :refer [fail]]))

(def not-found
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body "not found"})

(def ok
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "All good! Close this window and have a look at your terminal."})

(defn- make-handler
  "Make a request handler that takes code from params
  and writes it to the work> channel.
  Returns handler."
  [oauth-route work>]
  (fn [req]
    (if-not (and (= :get (:request-method req))
                 (= oauth-route (:uri req)))
      not-found
      (let [params (:params req)]
        (if-let [code (get params "code")]
          (do
            (log/info "Got code")
            (async/put! work> [:code code]))
          (log/warn "Got bad params" params))
        ok))))

(defn start
  "Start a web server and return it."
  [{:keys [oauth-route port work>]}]
  (try
    (log/info "Starting server")
    (let [app (wrap-params (make-handler oauth-route work>))
          server (run-server app {:port port
                                  ; For only auth token it's unlikely we need more threads.
                                  :thread 1})]
      (log/info "Server listening on port" port)
      server)
    (catch Exception e (fail "Failed to start server: " (.getMessage e)))))

(mount/defstate server
  :start (start (merge (mount/args)
                       (select-keys system/state [:work>])))
  :stop (server))
