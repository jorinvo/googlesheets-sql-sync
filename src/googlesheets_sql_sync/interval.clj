(ns googlesheets-sql-sync.interval
  (:require
    [clojure.core.async :refer [alt! chan close! go-loop timeout]]))

(defn to-ms [interval]
  (let [t interval
        d (-> t (get :days    0)       (* 24))
        h (-> t (get :hours   0) (+ d) (* 60))
        m (-> t (get :minutes 0) (+ h) (* 60))
        s (-> t (get :seconds 0) (+ m) (* 1000))]
    s))

(defn start
  "see https://stackoverflow.com/a/28370388/986455"
  [f]
  (let [stop (chan)]
    (go-loop [t (f)]
      (alt!
        (timeout t) (recur (f))
        stop (do
               (println "stopped interval")
               :stop)))
    #(close! stop)))
