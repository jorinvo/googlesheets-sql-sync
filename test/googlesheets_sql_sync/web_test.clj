(ns googlesheets-sql-sync.web-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [clojure.core.async :refer [<!! chan]]
   [org.httpkit.client :as http-client]
   [googlesheets-sql-sync.web :as web]
   [googlesheets-sql-sync.util :refer [get-free-port]]))

(defn- with-server [f]
  (let [ctx {:port (get-free-port)
             :work> (chan)
             :oauth-route "/"}
        server (web/start ctx)]
    (f ctx)
    (server)))

(deftest web
  (testing "get code"
    (with-server
      (fn [ctx]
        (let [{:keys [status]} @(http-client/get (str "http://localhost:" (:port ctx) "?code=123"))]
          (is (= status 200)))
        (is (= [:code "123"] (<!! (:work> ctx)))))))

  (testing "404"
    (with-server
      (fn [ctx]
        (let [{:keys [status]} @(http-client/get (str "http://localhost:" (:port ctx) "/nope"))]
          (is (= status 404)))))))
