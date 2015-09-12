(ns asset-builder.reporting)

;; ## Report Output

(defn- print-colorized
  [color-code args]
  (printf "\u001b[%s%s\u001b[0m%n"
          color-code
          (clojure.string/join " " args))
  (flush))

(defn print-heading
  [& args]
  (print-colorized "1m" args))

(defn print-success
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

(defn print-exception
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

(defmacro with-reporting
  [[heading success-msg error-msg] & body]
  `(let [_#     (print-heading ~heading)
         start# (System/nanoTime)]
     (try
       (let [result# (do ~@body)]
         (print-success start# ~success-msg)
         result#)
       (catch Throwable t#
         (print-exception start# t#)
         (throw t#)))))

;; ## Helpers

(defn ->kb
  [v]
  (format "%.2fKB" (/ v 1024.0)))
