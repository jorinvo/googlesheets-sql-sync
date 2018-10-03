(ns googlesheets-sql-sync.util-test
  (:require
   [clojure.test :refer [deftest testing are is]]
   [clojure.string :as string]
   [googlesheets-sql-sync.util :as util]))

(deftest get-free-port
  (dotimes [_ 10]
    (is (util/valid-port? (util/get-free-port)))))

(deftest valid-port?
  (are [y x] (= y (util/valid-port? x))
    false -1
    false 0
    true 1
    true 65535
    false 65536))

(deftest valid-url?
  (are [y x] (= y (util/valid-url? x))
    true "http://localhost:80"
    false "http//localhost:80"))

(deftest query-string
  (are [y x] (= y (util/query-string x))
    "" nil
    "" {}
    "a=1&b=2" {:a 1 :b 2}))

(deftest hostname
  (are [y x] (= y (util/hostname x))
    nil "localhost"
    "localhost" "http://localhost"
    "example.com" "https://example.com"
    "example.com" "https://example.com:1234/path?query=string"))

(deftest timing
  (util/with-duration
    #(Thread/sleep 100)
    #(is (<= 100 % 120))))

(deftest tempfile
  (util/with-tempfile "myfile" ".txt"
    (fn [f]
      (is (string/starts-with? (.getName f) "myfile"))
      (is (string/ends-with? (.getName f) ".txt"))
      (is (.exists f))
      (is (.canRead f))
      (is (.canWrite f)))))
