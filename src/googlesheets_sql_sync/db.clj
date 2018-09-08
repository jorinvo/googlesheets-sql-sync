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

(defn- create-table [db headers table]
  (println "Creating table")
  (let [cols (->> headers
                  (map #(str % " text"))
                  (string/join ", "))
        s (str "create table " table " ( " cols " )")]
    (jdbc/execute! db s)))

(defn- get-headers [db table]
  (println "Getting table headers")
  (let [s (str "select * from \"" (escape table "\"") "\" limit 1")]
    (try
      (-> (jdbc/query db s {:identifiers identity
                            :keywordize? false})
          first
          keys)
      (catch java.sql.SQLException e (if (.contains (.getMessage e) (str "ERROR: relation \"" table "\" does not exist"))
                                       nil
                                       (throw e))))))

(comment
  (let [db {:dbtype "postgresql"
            :dbname "bi"
            :host "localhost"
            :user "bi"}]
    (get-headers db "hi")))

(defn throw-db-err [target table e]
  (throw (Exception. (str "There was a problem with table \"" table "\" on target \"" target "\": " (.getMessage e)))))

(defn update-table [config sheet]
  (let [target (-> sheet :sheet :target)
        db (get-in config [:targets (keyword target)])
        table (-> sheet :sheet :table)
        rows (:rows sheet)
        headers (->> rows
                     first
                     (map #(escape % "\"")))
        escaped-headers (map #(str "\"" % "\"") headers)
        data (map #(concat % (repeat (- (count headers) (count %)) "")) (rest rows))]
    (try
      (println "Updating table" table)
      (let [new-headers (get-headers db table)]
        (if new-headers
          (when (not= headers new-headers)
            (throw (ex-info (str "Conflicting old and new table headers")
                            {:table table}
                            :old-headers headers
                            :new-headers new-headers)))
          (create-table db escaped-headers table)))
      (println "Clearing table")
      (jdbc/execute! db (str "truncate table " table))
      (println "Writing " (count data) "rows to table")
      (jdbc/insert-multi! db table escaped-headers data)
      sheet
      (catch Exception e (throw-db-err target table e)))))
