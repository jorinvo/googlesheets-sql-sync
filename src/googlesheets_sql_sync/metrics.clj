(ns googlesheets-sql-sync.metrics)

(defn init
  [ctx]
  (assoc ctx ::sync-counter (atom 0)))

(defn count-sync
  [{:keys [::sync-counter]}]
  (when sync-counter
    (swap! sync-counter inc)))

(defn ->string
  [{:keys [::sync-counter]}]
  (str
    "# HELP googlesheets_sql_sync_count The total number of syncs completed.\n"
    "# TYPE googlesheets_sql_sync_count counter\n"
    (format "googlesheets_sql_sync_count %d\n" @sync-counter)))

(defn response [ctx]
  {:status 200
   :headers {"Content-Type" "text/plain; version=0.0.4"}
   :body (-> ctx ->string)})

(defn enabled? [ctx]
  (contains? ctx ::sync-counter))

(comment
  (let [ctx (init nil)]
    (count-sync ctx)
    (count-sync ctx)
    (->string ctx))

    (enabled? nil)
    (enabled? (init nil))

    )
