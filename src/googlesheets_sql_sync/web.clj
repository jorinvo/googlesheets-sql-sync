(ns googlesheets-sql-sync.web
  (:require
   [clj-http.client :as http]
   [clojure.core.async :as async]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.params :refer [wrap-params]]))

(def default-port 9955)

(def oauth-route "/oauth")

(def not-found
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body "not found"})

(def ok
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "All good! Close this window and have a look at your terminal."})

(defn- make-handler
  [{:keys [work>]}]
  (fn [req]
    (if-not (and
             (= :get (:request-method req))
             (= oauth-route (:uri req)))
      not-found
      (let [params (:params req)]
        (if-let [code (get params "code")]
          (do
            (println "got code")
            (async/put! work> [:code code]))
          (println "got bad params" params))
        ok))))

(defn start
  [ctx]
  (println "start server")
  (let [port (:port ctx)
        app (-> (make-handler ctx) wrap-params)
        server (run-jetty app {:port port
                               ; For only auth token it's unlikely we need more threads.
                               :min-threads 1
                               :join? false})]
    (println "Server listening on port" port)
    (assoc ctx :stop-server #(.stop server))))
