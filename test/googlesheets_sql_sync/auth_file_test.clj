(ns googlesheets-sql-sync.auth-file-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [googlesheets-sql-sync.auth-file :as auth-file]
   [googlesheets-sql-sync.util :as util]))

(deftest auth-file
  (util/with-tempfile "auth" ".json"
    (fn [file]
      (.delete file)
      (auth-file/save file {:access_token "abc"
                            :expires_in 1000
                            :refresh_token "ghi"})
      (is (= "abc"
             (:access_token (auth-file/get file)))))))

