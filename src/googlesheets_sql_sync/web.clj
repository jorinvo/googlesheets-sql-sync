(ns googlesheets-sql-sync.web
  (:require
    [clj-http.client :as http]
    [clojure.core.async :as async]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.params :refer [wrap-params]]))

(def oauth-route "/oauth")

(def not-found
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body "not found"})

(def ok
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "all good"})

(defn- make-handler
  [code-chan]
  (fn [req]
    (if-not (and
             (= :get (:request-method req))
             (= oauth-route (:uri req)))
      not-found
      (let [params (:params req)]
        (if-let [err (get params "error")]
          (println "got error" err)
          (let [code (get params "code")]
            (println "got code")
            (async/put! code-chan code)))
        ok))))

(defn start [config code-chan]
  (let [app (-> (make-handler code-chan) wrap-params)
        server (run-jetty app {:port (:port config)
                               :join? false})]
    #(.stop server)))
