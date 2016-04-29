(defproject xsc/asset-builder "0.2.2"
  :description "Utility for CLJS + Asset Building"
  :url "https://github.com/xsc/asset-builder"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.8.51" :scope "provided"]
                 [asset-minifier "0.1.8"]]
  :pedantic? :abort)
