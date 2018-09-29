(ns googlesheets-sql-sync.http
  (:refer-clojure :exclude [get])
  (:require
   [clj-http.client :as http]
   [mount.core :as mount]
   [googlesheets-sql-sync.throttle :as throttle]))

(mount/defstate throttler
  :start (throttle/make (:api-rate-limit (mount/args))))

(defn- try-http [msg f]
  (try
    (f)
    (catch Exception e
      (let [d (ex-data e)]
        (throw (ex-info (str "failed to " msg ": " (:status d) "\n" (:body d))
                        (select-keys d [:status :body])))))))

(defn get [msg url req]
  (throttle/wait throttler)
  (try-http msg #(http/get url req)))

(defn post [msg url req]
  (throttle/wait throttler)
  (try-http msg #(http/post url req)))
