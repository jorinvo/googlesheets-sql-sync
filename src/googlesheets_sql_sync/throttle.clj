(ns googlesheets-sql-sync.throttle
  (:require
   [googlesheets-sql-sync.util :refer [now]]))

(defn make
  "Create throttler blocking for at a duration of ms."
  [ms]
  {:last-time (atom 0)
   :ms ms})

(defn wait
  "Wait for passed throttler.
  Sleeps for left duration when last call to wait is more recent than ms."
  [{:keys [last-time ms] :or {last-time (atom 0) ms 0}}]
  (swap! last-time (fn [t]
                     (let [t2 (now)
                           diff (- t2 t)]
                       (when (< diff ms)
                         (Thread/sleep (- ms diff)))
                       (now)))))

(comment
  (wait nil)
  (let [t (make 1000)]
    (wait t)
    (prn "one")
    (wait t)
    (prn "two")
    (wait t)
    (prn "three")))
