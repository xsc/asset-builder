(ns asset-builder.cljs
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
  [{:keys [cljs-path cljs]}]
  (with-reporting ["Compiling ClojureScript ..."
                   "ClojureScript has been compiled."]
    (->> (merge
           {:output-dir "target/out"}
           cljs
           {:verbose true})
         (cljs-build! cljs-path))))
