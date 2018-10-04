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

(deftest timeout>
  (let [x :x
        out> (async/chan)
        timeout> (interval/create-timeout> out> x)]
    (with-duration
      (fn []
        (async/>!! timeout> {:seconds 0.1})
        (is (= x (async/<!! out>))))
      (fn [t] (prn t) (is (< 100 t))))
    (async/close! timeout>)
    (is (= nil (async/<!! out>)))))
