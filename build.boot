(set-env!
  :dependencies  '[[adzerk/bootlaces                "0.1.12"  :scope "test"]
                   [org.clojure/clojure             "1.7.0"   :scope "provided"]
                   [org.clojure/data.json           "0.2.6"]
                   [commons-codec/commons-codec     "1.10"]
                   [org.bovinegenius/exploding-fish "0.3.4"]
                   ]
  :target-path "target"
  :resource-paths #{"src"})
(require '[adzerk.bootlaces :refer :all])
(def +version+ "0.1.6")
(task-options!
 pom  {:project     'rowtr/google-maps-web-api
       :version     +version+
       :description "Clojure wrapper for google maps web api including signing for premier"
       :url         "https://github.com/rwillig/google-maps-wep-api"
       :scm         {:url "https://github.com/rwillig/google-maps-web-api"}
       :license     {"MIT License" "http://opensource.org/licenses/mit-license.php"}}
 push {:repo        "clojars-upload"})
