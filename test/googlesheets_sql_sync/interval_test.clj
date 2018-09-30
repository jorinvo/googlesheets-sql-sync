(ns googlesheets-sql-sync.interval-test
  (:require
   [clojure.core.async :as async]
   [clojure.test :refer [deftest testing is]]
   [googlesheets-sql-sync.interval :as interval]
   [googlesheets-sql-sync.util :refer [with-duration]]))

(deftest interval-to-ms
  (is (= 182000 (interval/->ms {:minutes 3 :seconds 2})))
  (is (= 0 (interval/->ms {}))))

(deftest interval-to-string
  (is (= "2 hours 20 minutes" (interval/->string {:hours 2 :minutes 20})))
  (is (= "" (interval/->string {})))
  (is (= "" (interval/->string nil))))

(deftest timeout-chan
  (let [a {:timeout> (async/chan) :work> (async/chan)}
        b (interval/connect-timeouts a)]
    (with-duration
      (fn []
        (async/put! (:timeout> a) 1000)
        (is (= [:sync] (async/<!! (:work> a)))))
      (fn [t] (is (>= t 1000))))))
