(defproject googlesheets-sql-sync "0.1.0"
  :description "Keep your SQL database in sync with Google Sheets"
  :url "https://github.com/jorinvo/googlesheets-sql-sync"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[lein-tools-deps "0.4.1"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}
  :main ^:skip-aot googlesheets-sql-sync.cli
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :jvm-opts ["-Xmx200m"]
  :omit-source true)
