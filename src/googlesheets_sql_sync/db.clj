(ns googlesheets-sql-sync.db
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]))

(defn- escape
  "Replace all c in text with \\c"
  [text c]
  (string/replace text c (str "\\" c)))

(comment
  (escape "hey \"you\"!" "\""))

(defn- create [db headers table]
  (println table "- creating table")
  (let [cols (->> headers
                     (map #(str % " text"))
                     (string/join ", "))
        s (str "create table " table " ( " cols " )")]
    (jdbc/execute! db s)))

; TODO use this and compare to data headers
(defn- get-columns [db table]
  (println table "- fetching db columns for table")
  (println "TODO this only works with special permission. use easier way to get columns")
  (let [s (str "select column_name from information_schema.columns where table_name = '" (escape table "'") "';")]
    (jdbc/query db s)))

(defn- table-exists? [db table]
  (println "checking if table exists" table)
  (println "TODO this only works with special permission. use easier way to check this")
  (let [s (str "select 1 from information_schema.tables where table_name = '" (escape table "'") "';")]
    (< 0 (count (jdbc/query db s)))))

(defn update-table [config sheet]
  (let [db (get-in config [:targets (keyword (get-in sheet [:sheet :target]))])
        table (get-in sheet [:sheet :table])
        rows (:rows sheet)
        headers (->> rows
                     first
                     (map #(escape % "\""))
                     (map #(str "\"" % "\"")))
        data (rest rows)]
    (if (table-exists? db table)
      (println "TODO check columns for changes here...")
      (create db headers table))
    (println table "- clearing table")
    (jdbc/execute! db (str "truncate table " table))
    (println table "- writing " (count data) "rows to table")
    (jdbc/insert-multi! db table headers data)
    sheet))
