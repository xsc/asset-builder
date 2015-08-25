(defproject xsc/asset-builder "0.1.1-SNAPSHOT"
  :description "Utility for CLJS + Asset Building"
  :url "https://github.com/xsc/asset-builder"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.107" :scope "provided"]
                 [asset-minifier "0.1.6"]]
  :pedantic? :abort)
