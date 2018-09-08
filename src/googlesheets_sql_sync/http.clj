(ns googlesheets-sql-sync.http
  (:refer-clojure :exclude [get])
  (:require
   [clj-http.client :as http]))

(def api-rate-limit 1000)

(defn- try-http [msg f]
  (try
    (f)
    (catch Exception e#
      (let [d# (ex-data e#)]
        (throw (ex-info (str "failed to " msg ": " (:status d#) "\n" (:body d#))
                        (select-keys d# [:status :body])))))))

(defn- now []
  (.getTime (java.util.Date.)))

(defn- sleep [ms]
  (Thread/sleep ms))

(defn- throttle [f ms]
  (let [t (atom 0)]
    (fn [& args]
      (let [t2 (now)
            diff (- t2 @t)]
        (when (< diff ms)
          (sleep (- ms diff)))
        (swap! t (fn [_] (now))))
      (apply f args))))

(comment
  (time (let [h (throttle #(prn "hey" %) 2000)] (dotimes [n 3] (h n)))))

(def throttled-try-http (throttle try-http api-rate-limit))

(defn get [msg url req]
  (throttled-try-http msg #(http/get url req)))

(defn post [msg url req]
  (throttled-try-http msg #(http/post url req)))
