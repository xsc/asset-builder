(ns asset-builder.builders.minify
  (:require [asset-builder.reporting :refer [with-reporting ->kb]]
            [asset-minifier.core :as assets]
            [clojure.java.io :as io]))

;; ## Helpers

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

(defn- throw!
  [source-paths opts msg e]
  (throw
    (IllegalStateException.
      (format "when processing %s with options %s: %s"
              (pr-str source-paths)
              opts
              msg)
      e)))

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
  [opts source-base target-base source-paths target-path]
  (let [source-dir (io/file source-base)
        target-dir (io/file target-base)
        sources    (->base-vec source-dir source-paths)
        target     (->base target-dir target-path)
        minify-fn  (match-minifier-for-all sources)
        result     (minify-fn sources target opts)
        {:keys [warning errors original-size compressed-size]} result]
    (printf "* %s (%s) -> %s (%s)%n"
            source-paths
            (->kb original-size)
            target-path
            (if (seq errors) "ERROR!" (->kb compressed-size)))
    (when (seq errors)
      (throw! source-paths opts (first errors) nil))
    result))

;; ## Concatenation

(defn- concat-asset
  [source-base target-base source-paths target-path]
  (let [source-dir (io/file source-base)
        target-dir (io/file target-base)
        sources    (->base-vec source-dir source-paths)
        target     (->base target-dir target-path)
        error      (try
                     (with-open [out (io/output-stream target)]
                       (doseq [source sources]
                         (with-open [in (io/input-stream source)]
                           (io/copy in out))))
                     (catch Throwable t
                       (.delete (io/file target))
                       t))]
    (let [original-size (apply + (map (comp #(.length %) io/file) sources))
          final-size (.length (io/file target))]
      (printf "* %s (%s) -> %s (%s)%n"
              source-paths
              (->kb original-size)
              target-path
              (if error "ERROR!" (->kb final-size))))
    (some->> error (throw! source-paths {} (.getMessage error)))))

;; ## Build

(defn build
  [assets]
  (let [source-base (:source-path assets "resources")
        target-base (:target-path assets source-base)
        opts (dissoc assets :source-path :target-path :minify)]
    (when-let [assets-to-minify (:minify assets)]
      (with-reporting [(format "Minifying Assets [%s -> %s] ..."
                               source-base
                               target-base)
                       "Assets have been minified."]
        (doseq [[source-paths target-path] (:minify assets)]
          (minify-asset
            opts
            source-base
            target-base
            source-paths
            target-path))))
    (when-let [assets-to-concat (:concat assets)]
      (with-reporting [(format "Concatenating Assets [%s -> %s] ..."
                               source-base
                               target-base)
                       "Assets have been concatenated."]
        (doseq [[source-paths target-path] (:concat assets)]
          (concat-asset
            source-base
            target-base
            source-paths
            target-path))))))
