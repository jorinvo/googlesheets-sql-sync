(ns googlesheets-sql-sync.web-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [clojure.core.async :refer [<!! chan]]
   [org.httpkit.client :as http-client]
   [googlesheets-sql-sync.web :refer :all]))

(defn- get-free-port
  "thanks https://gist.github.com/apeckham/78da0a59076a4b91b1f5acf40a96de69"
  []
  (let [socket (java.net.ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn- with-server [f]
  (let [ctx {:port (get-free-port)
             :work> (chan)
             :oauth-route "/"}
        server (start ctx)]
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
