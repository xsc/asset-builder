(ns asset-builder.core
  (:require [asset-builder
             [cljs :as cljs]
             [minify :as minify]]))

;; ## Builders

(def ^:private builders
  [[:cljs   cljs/build]
   [:assets minify/build]])

;; ## Logic

(defn- generate-build-steps
  [opts]
  (for [[k f] builders
        :when (contains? opts k)]
    #(try
       (f opts)
       (catch Throwable _
         ::error))))

(defn- generate-all-build-steps
  [asset-opts]
  (-> (mapcat generate-build-steps asset-opts)
      (interleave (repeat println))))

;; ## Build

(defn build*
  [asset-opts]
  (let [steps (generate-all-build-steps asset-opts)]
    (dorun
      (for [step steps
            :let [result (step)]
            :while (not= ::error result)]
        result))))

(defn build
  "Build CLJS/JS/CSS assets. The following options are allowed:
   - `:cljs-path`: path to CLJS sources,
   - `:cljs`: CLJS compiler options,
   - `:asset-path`: path to assets (optional, defaults to current directory),
   - `:assets`: map of asset source -> minifcation target (relative to
     `:asset-path` or absolute).
   Assets will be minified according to their filename."
  [& asset-opts]
  (build* asset-opts))
