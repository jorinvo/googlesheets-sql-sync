(ns googlesheets-sql-sync.interval
  (:require
   [clojure.core.async :as async :refer [<! >! close! go pipeline-async timeout]]))

(defn to-ms [interval]
  (let [t interval
        d (-> t (get :days    0)       (* 24))
        h (-> t (get :hours   0) (+ d) (* 60))
        m (-> t (get :minutes 0) (+ h) (* 60))
        s (-> t (get :seconds 0) (+ m) (* 1000))]
    s))

(defn timeout-fn [t out>]
  (if t
    (go (<! (timeout t))
        (>! out> [:sync])
        (close! out>))
    (close! out>)))

(defn connect-timeouts [{:keys [timeout> work>]}]
  (pipeline-async 1 work> timeout-fn timeout>))

(comment
  (let [a {:timeout> (async/chan) :work> (async/chan)}
        b (connect-timeouts a)
        {:keys [timeout> work>]} b]
    (async/put! timeout> 1000)
    (prn (async/<!! work>))))
