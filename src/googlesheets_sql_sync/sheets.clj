(ns googlesheets-sql-sync.sheets
  (:require
   [googlesheets-sql-sync.http :as http]))

(def sheets-url "https://sheets.googleapis.com/v4/spreadsheets/")

(def default-sheet-range "A:ZZ")

(defn get-rows [sheet token]
  (let [id (:spreadsheet_id sheet)
        url (str sheets-url id "/values/" default-sheet-range)
        headers {"Authorization" (str "Bearer " token)}]
    (println "fetching data for" id)
    (let [resp (http/get "fetch sheet rows" url {:headers headers
                                                 :as :json})
          rows (-> resp :body :values)]
      {:sheet sheet
       :rows rows})))
