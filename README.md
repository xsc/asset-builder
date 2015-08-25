# asset-builder

A small utility to help with CLJS/JS/CSS asset building and minification.

## Usage

#### Artifact Coordinates

[![Clojars Project](http://clojars.org/xsc/asset-builder/latest-version.svg)](http://clojars.org/xsc/asset-builder)

#### Inclusion

I recommend adding a new profile containing all the things necessary for
building - like ClojureScript dependencies - and activating it together with
the `:dev` profile (for availability in the REPL):

```clojure
:profiles
{:build {:dependencies [[org.clojure/clojurescript "1.7.107"]
                        [xsc/asset-builder "x.y.z"]]}
 :dev   [:build
         {:dependencies ...}]}
```

#### Builder

You can then create a new namespace `build` (e.g. at `src/build.clj`) and add
the build logic:

```clojure
(ns build
  (:require [asset-builder.core :as assets]))

(defn run
  []
  (assets/build
    {:cljs-path "src/cljs"
     :cljs {:output-to "resources/js/main.js"
            :output-dir "target/out"
            :optimizations :advanced}
     :asset-path "resources"
     :assets {"js/external-script.js" "js/external-script.min.js"}}))

(defn -main
  []
  (run)
  (System/exit 0))
```

In the REPL, you can now run:

```clojure
(require 'build)
(build/run)
```

Note that adding the `-main` function is optional but allows you to add an
alias as described in the following section.

#### Aliases

For automatic building (outside the REPL) you can add the following aliases
(according to your requirements, of course):

```clojure
:aliases
{"build"   ["with-profile" "+build" "run" "-m" "build"]
 "jar"     ["do" "build," "jar"]
 "uberjar" ["do" "build," "uberjar"]
 "install" ["do" "build," "install"]
 "deploy"  ["do" "build," "deploy"]}
```

## Contributing

Contributions are always welcome!

## Shoulders/Giants

- [asset-minifier](https://github.com/yogthos/asset-minifier)
- And someone will inevitably say: "Why don't you just use [boot][boot]?"

[boot]: https://github.com/boot-clj/boot

## License

```
The MIT License (MIT)

Copyright (c) 2015 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
