(ns googlesheets-sql-sync.web
  (:require
   [clojure.core.async :as async]
   [mount.core :as mount]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.params :refer [wrap-params]]
   [googlesheets-sql-sync.system :as system]))

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
            (println "Got code")
            (async/put! work> [:code code]))
          (println "Got bad params" params))
        ok))))

(defn start
  "Start a web server and return it."
  [{:keys [oauth-route port work>]}]
  (try
    (println "Starting server")
    (let [app (wrap-params (make-handler oauth-route work>))
          server (run-jetty app {:port port
                                  ; For only auth token it's unlikely we need more threads.
                                 :min-threads 1
                                 :join? false})]
      (println "Server listening on port" port)
      server)
    (catch Exception e (throw (Exception. (str "Failed to start server: " (.getMessage e)))))))

(defn stop [server]
  (.stop server))

(mount/defstate server
  :start (start (merge (mount/args)
                       (select-keys system/state [:work>])))
  :stop (stop server))
