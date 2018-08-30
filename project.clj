(defproject googlesheets-sql-sync "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.postgresql/postgresql "42.2.4"]]
  :main ^:skip-aot googlesheets-sql-sync.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
