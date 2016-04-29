(ns asset-builder.builders.cljs
  (:require [asset-builder.reporting :refer [with-reporting]]))

(defonce cljs-build!
  (or
    (try
      (require 'cljs.build.api)
      (let [build (some-> (resolve 'cljs.build.api/build) deref)
            inputs (some-> (resolve 'cljs.build.api/inputs) deref)]
        (fn [paths options]
          (build (apply inputs paths) options)))
      (catch Throwable _))
    (fn [& _]
      (throw
        (IllegalStateException.
          "dependency 'org.clojure/clojurescript' missing (or incompatible).")))))

(defn build
  [{:keys [source-path source-paths]
    :or {source-paths ["src/cljs"]}
    :as cljs}]
  (let [paths (or (some-> source-path vector)
                  (vec source-paths))]
    (with-reporting [(format "Compiling ClojureScript %s ..." paths)
                     (format "ClojureScript %s has been compiled." paths)]
      (->> (merge
             {:output-dir "target/out"}
             (dissoc cljs :source-path :source-paths)
             {:verbose true})
           (cljs-build! paths)))))
