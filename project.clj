(defproject nabble_tools "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clj-time "0.5.0"]
                 [enlive "1.0.0-SNAPSHOT"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojars.shriphani/warc-clojure "0.2.3-SNAPSHOT"]
                 [org.clojure/tools.cli "0.2.2"]]
  :main nabble-tools.nabble-index-pages
  :aot :all)
