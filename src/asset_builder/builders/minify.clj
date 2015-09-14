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

(defn- minify-asset
  [base-path source-paths target-path]
  (let [dir (io/file base-path)
        ->path #(let [f (io/file %)]
                  (if (.isAbsolute f)
                    %
                    (.getPath (io/file dir %))))
        sources (->> (if (sequential? source-paths)
                       source-paths
                       (vector source-paths))
                     (map ->path))
        target (->path target-path)
        minify-fn (match-minifier-for-all sources)
        result (minify-fn sources target)
        {:keys [warning errors original-size compressed-size]} result]
    (printf "* %s (%s) -> %s (%s)%n"
            source-paths
            (->kb original-size)
            target-path
            (or (seq errors) (->kb compressed-size)))
    result))

;; ## Build

(defn build
  [{:keys [asset-path assets]}]
  (with-reporting ["Minifying Assets ..."
                   "Assets have been minified."]
    (doseq [[source-paths target-path] assets]
      (minify-asset asset-path source-paths target-path))))
