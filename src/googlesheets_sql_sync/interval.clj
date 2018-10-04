(ns googlesheets-sql-sync.interval
  (:require
   [clojure.core.async :refer [<! >! chan close! sliding-buffer go pipeline-async timeout]]
   [clojure.string :as string]))

(defn ->ms [interval]
  (let [t interval
        d (-> t (get :days    0)       (* 24))
        h (-> t (get :hours   0) (+ d) (* 60))
        m (-> t (get :minutes 0) (+ h) (* 60))
        s (-> t (get :seconds 0) (+ m) (* 1000))]
    s))

(defn ->string [interval]
  (->> [:days :hours :minutes :seconds]
       (map #(when-let [t (get interval %)] (str t " " (name %))))
       (remove nil?)
       (string/join " ")))

(defn- timeout-fn [interval out-chan payload]
  (if interval
    (go (<! (timeout (->ms interval)))
        (>! out-chan payload)
        (close! out-chan))
    (close! out-chan)))

(defn create-timeout>
  "Returns a channel which takes timeouts of type interval
  and writes payload to out-chan once timeout passed.
  If next timeout is passed to channel while last is still blocking,
  next timeout out is dropped.
  out-chan is closed once timeout> is closed."
  [out-chan payload]
  (let [timeout> (chan (sliding-buffer 1))]
    (pipeline-async 1 out-chan #(timeout-fn %1 %2 payload) timeout>)
    timeout>))
