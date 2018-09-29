(defproject googlesheets-sql-sync "0.1.0"
  :description "Keep your SQL database in sync with Google Sheets"
  :url "https://github.com/jorinvo/googlesheets-sql-sync"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.postgresql/postgresql "42.2.4"]
                 [org.clojure/tools.cli "0.3.7"]
                 [cheshire "5.8.0"]
                 [clj-http "3.9.1"]
                 [mount "0.1.13"]
                 [ring/ring-core "1.7.0-RC2"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [spootnik/signal "0.2.1"]]
  :main ^:skip-aot googlesheets-sql-sync.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[cljfmt "0.5.1"]
                                  [expound "0.7.1"]]
                   :source-paths ["src"]}}
  :jvm-opts ["-Xmx200m"]
  :repl-options {:init-ns googlesheets-sql-sync.dev}
  :omit-source true)
