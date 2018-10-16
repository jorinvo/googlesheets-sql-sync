(ns googlesheets-sql-sync.http
  (:refer-clojure :exclude [get])
  (:require
   [org.httpkit.client :as http-client]
   [jsonista.core :as json]
   [googlesheets-sql-sync.throttle :as throttle]))

(defn- json-or-throw [f]
  (let [{:keys [error status body] :as r} @(f)]
    (when error
      (throw error))
    (when (>= status 400)
      (throw (ex-info (str "bad status: " status "\n" body) r)))
    (json/read-value body (json/object-mapper {:decode-key-fn true}))))

(defn get
  "Get JSON from url or throw error"
  [url req throttler]
  (throttle/wait throttler)
  (json-or-throw #(http-client/get url req)))

(defn post
  "Post as JSON to url or throw error"
  [url req throttler]
  (throttle/wait throttler)
  (json-or-throw #(http-client/post url req)))
