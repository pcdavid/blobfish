(defproject blobfish "0.1.0-SNAPSHOT"
  :description "Serve raw content of Git repos over HTTP"
  :url "http://github.com/pcdavid/blobfish"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.5"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [me.raynes/conch "0.7.0"]
                 [org.eclipse.jgit/org.eclipse.jgit  "3.4.1.201406201815-r"]]
  :plugins [[lein-ring "0.8.7"]]
  :ring {:handler blobfish.core/handler}
  :main ^:skip-aot blobfish.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
