(defproject control-surface "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core          "1.9.1"]
                 [ring/ring-jetty-adapter "1.9.1"]
                 [compojure               "1.6.2"]
                 [org.clojure/tools.logging "1.3.0"]
                 [org.slf4j/slf4j-api "2.0.9"]
                 [ch.qos.logback/logback-classic "1.4.11"]
                 [net.logstash.logback/logstash-logback-encoder "7.4"]
                 [ch.codesmith/logger "0.7.108"]
                 [org.clojure/data.json "2.5.0"]
                 [ring/ring-json "0.5.1"]
                 [clojure.java-time "1.4.2"]
                 [org.clojure/core.async "1.6.673"]
                 [clj-http "3.13.0"]
                 [morse "0.4.3"]
                 [org.xerial/sqlite-jdbc "3.20.0"]
                 [org.clojure/java.jdbc "0.7.0"]
                 [com.layerware/hugsql "0.4.7"]
                 [mount "0.1.11"]]
  :main ^:skip-aot control-surface.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
