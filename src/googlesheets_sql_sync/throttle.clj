(ns googlesheets-sql-sync.throttle
  (:require
   [googlesheets-sql-sync.util :refer [now]]))

(defn make [ms]
  {:last-time (atom 0)
   :ms ms})

(defn wait
  ""
  [{:keys [last-time ms] :or {last-time (atom 0) ms 0}}]
  (swap! last-time (fn [t]
                     (let [t2 (now)
                           diff (- t2 t)]
                       (when (< diff ms)
                         (Thread/sleep (- ms diff)))
                       (now)))))

(comment
  (let [t (make 1000)]
    (wait t)
    (prn "one")
    (wait t)
    (prn "two")
    (wait t)
    (prn "three")))
