(ns googlesheets-sql-sync.sheets
  (:require
   [googlesheets-sql-sync.http :as http]
   [googlesheets-sql-sync.log :as log]
   [org.httpkit.client :refer [url-encode]]))

(def sheets-url "https://sheets.googleapis.com/v4/spreadsheets/")

(defn get-rows
  "Fetch a sheet's data from the Google Spreadsheet API"
  [sheet token throttler]
  (let [id      (:spreadsheet_id sheet)
        url     (str sheets-url id "/values/" (url-encode (:range sheet)))
        headers {"Authorization" (str "Bearer " token)}]
    (log/info "Fetching data for" id)
    (let [resp (http/get url {:headers headers} throttler)
          rows (:values resp)]
      {:sheet sheet
       :rows rows})))
