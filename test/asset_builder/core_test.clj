(ns asset-builder.core-test
  (:require [clojure.test :refer :all]
            [asset-builder.core :as assets]
            [clojure.java.io :as io])
  (:import [org.apache.commons.io FileUtils]))

;; ## Helpers

(defn- setup
  [m]
  (let [state (atom (zipmap (keys m) (repeat ::idle)))]
    (with-meta
      (vec
        (for [[k v] (sort-by key (seq m))]
          [k (fn [_]
               (when (= v ::error)
                 (swap! state assoc k v)
                 (throw (Exception.)))
               (swap! state assoc k ::built))]))
      {:state state})))

(defn- built?
  [builders k]
  (= (-> builders meta :state deref k) ::built))

(defn- error?
  [builders k]
  (= (-> builders meta :state deref k) ::error))

(defn- unbuilt?
  [builders k]
  (= (-> builders meta :state deref k) ::idle))

;; ## Tests

(deftest t-build*
  (testing "successful builds."
    (binding [assets/*internal-print-fn* (constantly nil)]
      (testing "no builders used."
        (let [builders (setup {:a true, :b true})]
          (is (nil? (assets/build* builders [])))
          (is (unbuilt? builders :a))
          (is (unbuilt? builders :b))))
      (testing "no builders match."
        (let [builders (setup {:a true, :b true})]
          (is (nil? (assets/build* builders [{:c {}}])))
          (is (unbuilt? builders :a))
          (is (unbuilt? builders :b))))
      (testing "all builders used."
        (let [builders (setup {:a true, :b true})]
          (is (nil? (assets/build* builders [{:a {}, :b {}}])))
          (is (built? builders :a))
          (is (built? builders :b))))
      (testing "all builders used, multiple option groups."
        (let [builders (setup {:a true, :b true})]
          (is (nil? (assets/build* builders [{:a {}} {:b {}}])))
          (is (built? builders :a))
          (is (built? builders :b))))
      (testing "some builders used."
        (let [builders (setup {:a true, :b true})]
          (is (nil? (assets/build* builders [{:a {}}])))
          (is (built? builders :a))
          (is (unbuilt? builders :b))))))
  (testing "failed builds."
    (binding [assets/*internal-print-fn* (constantly nil)]
      (testing "first builder throws exception."
        (let [builders (setup {:a ::error, :b true})]
          (is (nil? (assets/build* builders [{:a {}, :b {}}])))
          (is (error? builders :a))
          (is (unbuilt? builders :b))))
      (testing "last builder throws exception."
        (let [builders (setup {:a true, :b ::error})]
          (is (nil? (assets/build* builders [{:a {}, :b {}}])))
          (is (built? builders :a))
          (is (error? builders :b)))))))

(deftest t-build
  (testing "actual two-phase build."
    (let [out-dir (doto (io/file "target/out")
                    (FileUtils/deleteDirectory))
          exists? #(.isFile (io/file out-dir %))]
      (is (not (.exists out-dir)))
      (->> (assets/build
             {:assets {:source-path "test/data"
                       :target-path "target/out"
                       :minify {"style.css"  "style.min.css"
                                "support.js" "support.min.js"}}}
             {:cljs {:source-path   "test"
                     :output-to     "target/out/test.js"
                     :output-dir    "target/out"
                     :optimizations :whitespace}})
           (nil?)
           (is))
      (is (exists? "style.min.css"))
      (is (exists? "support.min.js"))
      (is (exists? "test.js")))))
