(ns googlesheets-sql-sync.options-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [clojure.tools.cli :refer [parse-opts]]
   [googlesheets-sql-sync.options :as options]))

(deftest cli
  (is (= nil (:errors (parse-opts [] (options/cli))))))

(deftest defaults
  (is (map? (options/defaults))))

(deftest validate
  (is (= nil (options/validate nil)))
  (is (= nil (options/validate {})))
  (let [x (options/validate {:port 0
                             :oauth-route ""
                             :api-rate-limit 1
                             :server-oauth-url ""})]
    (is (= 2 (count x)))
    (is (contains? x :port))
    (is (contains? x :server-oauth-url))))

(deftest validate-defaults
  (is (= nil (options/validate (options/defaults)))))
