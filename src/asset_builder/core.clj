(ns asset-builder.core
  (:require [clojure.java.io :as io]
            [asset-minifier.core :as assets]) )

;; ## Reporting

(defn- print-colorized
  [color-code args]
  (printf "\u001b[%s%s\u001b[0m%n"
          color-code
          (clojure.string/join " " args))
  (flush))

(defn- print-heading
  [& args]
  (print-colorized "1m" args))

(defn- print-success
  [start-time & args]
  (let [delta-str (format "[%.3fs]" (/ (- (System/nanoTime) start-time) 1e9))]
    (->> (concat args [delta-str])
         (print-colorized "32m"))))

(defn- location
  [exception]
  (if-let [s (first (.getStackTrace exception))]
    (format "(%s:%d)" (.getFileName s) (.getLineNumber s))
    "(unknown)"))

(defn- format-exception
  [exception]
  (str (.getMessage exception) " " (location exception)))

(defn- print-exception
  [start-time exception]
  (let [delta-str (format "[%.3fs]" (/ (- (System/nanoTime) start-time) 1e9))]
    (if (instance? clojure.lang.ExceptionInfo exception)
      (.printStackTrace exception)
      (.printStackTrace exception *out*))
    (flush)
    (->> (vector "!"
                 (->> (iterate #(.getCause %) exception)
                      (take-while identity)
                      (rest)
                      (map format-exception)
                      (map #(str "+-- " %))
                      (cons (str (format-exception exception) " " delta-str))
                      (clojure.string/join "\n")))
         (print-colorized "31m"))))

(defmacro ^:private with-reporting
  [[heading success-msg error-msg] & body]
  `(let [_#     (print-heading ~heading)
         start# (System/nanoTime)]
     (try
       (let [result# (do ~@body)]
         (print-success start# ~success-msg)
         result#)
       (catch Throwable t#
         (print-exception start# t#)
         ::error))))

;; ## CLJS

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

(defn- build-cljs
  [{:keys [cljs-path cljs]}]
  (with-reporting ["Compiling ClojureScript ..."
                   "ClojureScript has been compiled."]
    (->> (merge
           {:output-dir "target/out"}
           cljs
           {:verbose true})
         (cljs-build! cljs-path))))

;; ## Assets

(defn- ->kb
  [v]
  (format "%.2fKB" (/ v 1024.0)))

(defn- minify-asset
  [source-path target-path]
  (let [f (cond (re-matches #".*\.js" source-path) assets/minify-js
                (re-matches #".*\.css" source-path) assets/minify-css
                :else (throw
                        (IllegalArgumentException.
                          (format "Can't infer file type (CSS/JS) from: %s"
                                  source-path))))
        result (f source-path target-path)
        {:keys [warning errors original-size compressed-size]} result]
    (printf "* %s (%s) -> %s (%s)%n"
            source-path
            (->kb original-size)
            target-path
            (or (seq errors) (->kb compressed-size)))
    result))

(defn- minify-assets
  [{:keys [assets asset-path]
    :or {asset-path "."}}]
  (with-reporting ["Minifying Assets ..."
                   "Assets have been minified."]
    (let [directory (io/file asset-path)
          ->path #(let [n (io/file %)]
                    (if (.isAbsolute n)
                      (.getPath n)
                      (->> n (io/file directory) (.getPath))))]
      (doseq [[source-path target-path] assets]
        (minify-asset
          (->path source-path)
          (->path target-path))))))

;; ## Build

(defn build
  "Build CLJS/JS/CSS assets. The following options are allowed:
   - `:cljs-path`: path to CLJS sources,
   - `:cljs`: CLJS compiler options,
   - `:asset-path`: path to assets (optional, defaults to current directory),
   - `:assets`: map of asset source -> minifcation target (relative to
     `:asset-path` or absolute).
   Assets will be minified according to their filename."
  [{:keys [cljs assets] :as opts}]
  (let [build-steps (->> [[build-cljs    cljs]
                          [minify-assets assets]]
                         (remove (comp empty? second)))]
    (dorun
      (for [[f arg] (interleave build-steps (repeat [#(do %& (println))]))
            :let [result (f opts)]
            :while (not= ::error result)]
        result))))
