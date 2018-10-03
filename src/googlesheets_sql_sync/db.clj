(ns googlesheets-sql-sync.db
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as string]
   [googlesheets-sql-sync.log :as log]))

(defn- escape
  "Replace all c in text with \\c"
  [text c]
  (string/replace text c (str "\\" c)))

(comment
  (escape "hey \"you\"!" "\""))

(defn- create-table [db headers table]
  (log/info "Creating table")
  (let [cols (->> headers
                  (map #(str % " text"))
                  (string/join ", "))
        s (str "create table " table " ( " cols " )")]
    (jdbc/execute! db s)))

(defn- get-headers
  "Get headers of a table from DB.
  Used technique will only work if table is not empty.
  Doing this to keep it DB and permission independent.
  Returns nil if table is empty or does not exist."
  [db table]
  (log/info "Getting table headers")
  (let [s (str "select * from \"" (escape table "\"") "\" limit 1")]
    (try
      (-> (jdbc/query db s {:identifiers identity
                            :keywordize? false})
          first
          keys)
      (catch java.sql.SQLException e (when-not (.contains
                                                (.getMessage e)
                                                (str "ERROR: relation \"" table "\" does not exist"))
                                       (throw e))))))

(comment
  (let [db {:dbtype "postgresql"
            :dbname "bi"
            :host "localhost"
            :user "bi"}]
    (get-headers db "hi")))

(defn- throw-db-err [target table e]
  (throw (Exception. (str "There was a problem with table \"" table "\" on target \"" target "\": " (.getMessage e)))))

(defn- empty-strings->nil [xs]
  (map #(when-not (= "" %) %) xs))

(comment
  (= '(nil 1 "a" nil nil " ")
     (empty-strings->nil ["" 1 "a" "" "" " "])))

(defn- ensure-size
  "Append nil to xs until it is of size n."
  [n xs]
  (concat xs (repeat (- n (count xs)) nil)))

(comment
  (= [1 2 nil]
     (ensure-size 3 [1 2])))

(defn- check-header-conflicts [a b]
  (when (not= a b)
    (throw (ex-info (str "Conflicting old and new table headers")
                    {:old-headers a
                     :new-headers b}))))

(defn- clear-table [db table]
  (log/info "Clearing table")
  (jdbc/execute! db (str "truncate table " table)))

(defn- write-rows [db table headers rows]
  (log/info "Writing " (count rows) "rows to table")
  (jdbc/insert-multi! db table headers rows))

(defn update-table [config sheet]
  (let [target (-> sheet :sheet :target)
        db (get-in config [:targets (keyword target)])
        table (-> sheet :sheet :table)
        rows (:rows sheet)
        headers (->> rows
                     first
                     (map #(escape % "\"")))
        escaped-headers (map #(str "\"" % "\"") headers)
        header-count (count headers)
        data (->> (rest rows)
                  (map #(map string/trim %))
                  (map empty-strings->nil)
                  (map #(ensure-size header-count %)))]
    (try
      (log/info "Updating table" table)
      (if-let [new-headers (get-headers db table)]
        (check-header-conflicts headers new-headers)
        (create-table db escaped-headers table))
      (clear-table db table)
      (write-rows db table escaped-headers data)
      sheet
      (catch Exception e (throw-db-err target table e)))))
