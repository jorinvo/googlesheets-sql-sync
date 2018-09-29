(ns googlesheets-sql-sync.throttle)

(defn- now []
  (.getTime (java.util.Date.)))

(defn- sleep [ms]
  (Thread/sleep ms))

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
                         (sleep (- ms diff)))
                       (now)))))

(comment
  (let [t (make 2000)]
    (wait t)
    (prn "hi")
    (wait t)
    (prn "ha")
    (wait t)
    (prn "ho")))
