(ns googlesheets-sql-sync.sheets
  (:require
    [clj-http.client :as http]))

(def sheets-url "https://sheets.googleapis.com/v4/spreadsheets/")

(def default-sheet-range "A:ZZ")

(defn fetch-rows [sheet token]
  (let [id (:spreadsheet_id sheet)
        url (str sheets-url id "/values/" default-sheet-range)
        headers {"Authorization" (str "Bearer " token)}]
    (println "fetching data for" id)
    (-> (http/get url {:headers headers :as :json})
        :body
        :values
        (as-> rows
          {:sheet sheet
           :rows rows}))))
