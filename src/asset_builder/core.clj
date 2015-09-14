(ns asset-builder.core
  (:require [asset-builder.builders
             [cljs :as cljs]
             [minify :as minify]]))

;; ## Default Builders

(def default-builders
  [[:cljs   cljs/build]
   [:assets minify/build]])

;; ## Logic

(def ^:dynamic *internal-print-fn* println)

(defn- generate-build-steps
  [builders opts]
  (for [[k f] builders
        :when (contains? opts k)]
    #(try
       (f opts)
       (catch Throwable _
         ::error))))

(defn- generate-all-build-steps
  [builders asset-opts]
  (-> (mapcat #(generate-build-steps builders %) asset-opts)
      (interleave (repeat *internal-print-fn*))))

;; ## Build

(defn build*
  [builders asset-opts]
  (when (seq asset-opts)
    (let [steps (generate-all-build-steps builders asset-opts)]
      (when-not (seq steps)
        (*internal-print-fn* "WARN: no builders matched your options (typo?)."))
      (dorun
        (for [step steps
              :let [result (step)]
              :while (not= ::error result)]
          result)))))

(defn build
  "Build CLJS/JS/CSS assets. The following options are allowed:
   - `:cljs-path`: path to CLJS sources,
   - `:cljs`: CLJS compiler options,
   - `:asset-path`: path to assets (optional, defaults to current directory),
   - `:assets`: map of asset source -> minifcation target (relative to
     `:asset-path` or absolute).
   Assets will be minified according to their filename."
  [& asset-opts]
  (build* default-builders asset-opts))
