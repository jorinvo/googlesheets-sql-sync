(ns googlesheets-sql-sync.web-test
  (:require
   [clojure.test :refer :all]
   [clojure.core.async :refer [<!! chan]]
   [clj-http.client :as http]
   [googlesheets-sql-sync.web :refer :all]))

(defn- get-free-port
  "thanks https://gist.github.com/apeckham/78da0a59076a4b91b1f5acf40a96de69"
  []
  (let [socket (java.net.ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn- with-server [f]
  (let [s (start {:port (get-free-port)
                  :work> (chan)})]
    (f s)
    ((:stop-server s))))

(deftest web
  (testing "get code"
    (with-server
      (fn [s]
        (http/get (str "http://localhost:" (:port s) "/" oauth-route "?code=123"))
        (is (= [:code "123"] (<!! (:work> s)))))))

  (testing "404"
    (with-server
      (fn [s]
        (try
          (http/get (str "http://localhost:" (:port s)))
          (catch Exception e (is (= 404 (-> e ex-data :status)))))))))
