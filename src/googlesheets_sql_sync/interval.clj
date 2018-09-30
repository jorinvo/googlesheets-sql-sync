(ns googlesheets-sql-sync.interval
  (:require
   [clojure.core.async :refer [<! >! close! go pipeline-async timeout]]
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

(defn- timeout-fn [t out>]
  (if t
    (go (<! (timeout t))
        (>! out> [:sync])
        (close! out>))
    (close! out>)))

(defn connect-timeouts [{:keys [timeout> work>]}]
  (pipeline-async 1 work> timeout-fn timeout>))
