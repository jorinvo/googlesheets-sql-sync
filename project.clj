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
                 [http-kit "2.3.0"]
                 [metosin/jsonista "0.2.0"]
                 [mount "0.1.13"]
                 [ring/ring-core "1.7.0-RC2"]
                 [spootnik/signal "0.2.1"]]
  :main ^:skip-aot googlesheets-sql-sync.cli
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[cljfmt "0.5.1"]
                                  [expound "0.7.1"]]
                   :source-paths ["src" "test" "dev"]}}
  :jvm-opts ["-Xmx200m"]
  :repl-options {:init-ns dev}
  :omit-source true)
