(ns googlesheets-sql-sync.config-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [googlesheets-sql-sync.config :as config]
   [googlesheets-sql-sync.util :as util]))

(deftest config
  (util/with-tempfile "cfg" ".json"
    (fn [file]
      (.delete file)
      (config/generate {:config-file (.getAbsolutePath file)
                        :port 1234
                        :oauth-route "/"})
      (is (= "http://localhost:1234/"
             (-> file  config/get :google_credentials :redirect_uri))))))

(deftest auth
  (util/with-tempfile "auth" ".json"
    (fn [file]
      (.delete file)
      (config/save-auth file {:access_token "abc"
                              :expires_in 1000
                              :refresh_token "ghi"})
      (is (= "abc"
             (:access_token (config/get-auth file)))))))
