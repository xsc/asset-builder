(ns asset-builder.builders.cljs
  (:require [asset-builder.reporting :refer [with-reporting]]))

(defonce cljs-build!
  (or
    (try
      (require 'cljs.build.api)
      (some-> (resolve 'cljs.build.api/build) deref)
      (catch Throwable _))
    (fn [& _]
      (throw
        (IllegalStateException.
          "dependency 'org.clojure/clojurescript' missing (or incompatible).")))))

(defn build
  [{:keys [source-path]
    :or {source-path "src/cljs"}
    :as cljs}]
  (with-reporting [(format "Compiling ClojureScript [%s] ..." source-path)
                   (format "ClojureScript [%s] has been compiled." source-path)]
    (->> (merge
           {:output-dir "target/out"}
           (dissoc cljs :source-path)
           {:verbose true})
         (cljs-build! source-path))))
