(ns googlesheets-sql-sync.sheets
  (:require
   [googlesheets-sql-sync.http :as http]
   [googlesheets-sql-sync.log :as log]))

(def sheets-url "https://sheets.googleapis.com/v4/spreadsheets/")

(def default-sheet-range "A:ZZ")

(defn get-rows
  "Fetch a sheet's data from the Google Spreadsheet API"
  [sheet token]
  (let [id      (:spreadsheet_id sheet)
        url     (str sheets-url id "/values/" default-sheet-range)
        headers {"Authorization" (str "Bearer " token)}]
    (log/info "Fetching data for" id)
    (let [resp (http/get url {:headers headers})
          rows (:values resp)]
      {:sheet sheet
       :rows rows})))
