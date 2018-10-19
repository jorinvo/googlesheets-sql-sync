(ns googlesheets-sql-sync.web
  (:require
   [clojure.core.async :as async]
   [org.httpkit.server :refer [run-server]]
   [ring.middleware.params :refer [wrap-params]]
   [googlesheets-sql-sync.log :as log]
   [googlesheets-sql-sync.metrics :as metrics]
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
  [{:as ctx :keys [metrics-route oauth-route work>]}]
  (log/info "Waiting for oauth codes at" oauth-route)
  (log/info "Serving metrics at" metrics-route)
  (fn [{:keys [params request-method uri]}]
    (cond
      (not= :get request-method)
      not-found

      (= oauth-route uri)
      (do
        (if-let [code (get params "code")]
          (do
            (log/info "Got code")
            (async/put! work> [:code code]))
          (log/warn "Got bad params" params))
        ok)

      (and (= metrics-route uri) (metrics/enabled? ctx))
      (metrics/response ctx)

      :else
      not-found)))

(defn start
  "Start a web server and return it."
  [{:as ctx :keys [port]}]
  (try
    (log/info "Starting server")
    (let [app (wrap-params (make-handler ctx))
          server (run-server app {:port port
                                  ; For only auth token it's unlikely we need more threads.
                                  :thread 1})]
      (log/info "Server listening on port" port)
      server)
    (catch Exception e (fail "Failed to start server: " (.getMessage e)))))
