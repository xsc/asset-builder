(ns asset-builder.builders.minify
  (:require [asset-builder.reporting :refer [with-reporting ->kb]]
            [asset-minifier.core :as assets]
            [clojure.java.io :as io]))

;; ## Minifiers

(def ^:private minifiers
  {#".*\.js"  assets/minify-js
   #".*\.css" assets/minify-css})

(defn match-minifier
  [path]
  (some
    (fn [[pattern f]]
      (when (re-matches pattern path)
        f))
    minifiers))

(defn- match-minifier-for-all
  [paths]
  (let [fs (map match-minifier paths)]
    (assert (seq fs) (str "no minifier found for: " paths))
    (assert (apply = fs) (str "conflicting files for minification: " paths))
    (first fs)))

;; ## Minification

(defn- ->base
  [base path]
  (let [f (io/file path)]
    (if (.isAbsolute f)
      path
      (.getPath (io/file base path)))))

(defn- ->base-vec
  [base paths]
  (->> (if (sequential? paths) paths [paths])
       (map #(->base base %))))

(defn- minify-asset
  [source-base target-base source-paths target-path]
  (let [source-dir (io/file source-base)
        target-dir (io/file target-base)
        sources    (->base-vec source-dir source-paths)
        target     (->base target-dir target-path)
        minify-fn  (match-minifier-for-all sources)
        result     (minify-fn sources target)
        {:keys [warning errors original-size compressed-size]} result]
    (printf "* %s (%s) -> %s (%s)%n"
            source-paths
            (->kb original-size)
            target-path
            (or (seq errors) (->kb compressed-size)))
    result))

;; ## Build

(defn build
  [assets]
  (let [source-base (:source-path assets "resources")
        target-base (:target-path assets source-base)]
    (with-reporting [(format "Minifying Assets [%s -> %s] ..."
                             source-base
                             target-base)
                     "Assets have been minified."]
      (doseq [[source-paths target-path] (:minify assets)]
        (minify-asset
          source-base
          target-base
          source-paths
          target-path)))))
