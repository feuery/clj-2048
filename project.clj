(defproject clj-2048 "0.1.0"
  :description "A clone of the famous game 2048 (http://git.io/2048)"
  :url "https://github.com/feuery/clj-2048"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[seesaw "1.4.4"]
                 [org.clojure/clojure "1.6.0"]]
  :aot [clj-2048.core]
  :main clj-2048.core)
