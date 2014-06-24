(ns google-maps-web-api.core  
  (:require (org.bovinegenius [exploding-fish :as uri]))
  (:require [clojure.string :as s])
  (:require [clojure.data.json :as json])
  (:import javax.crypto.spec.SecretKeySpec)
  (:import javax.crypto.Mac)
  (:import org.apache.commons.codec.binary.Base64)) 

(def base-url               "https://maps.googleapis.com/maps/api/")
(def geocode-url            (str base-url "geocode/"))
(def directions-url         (str base-url "directions/"))

(defn- prepare-secret [secret]
  (Base64/decodeBase64 (.getBytes (s/replace (s/replace secret "-" "+") "_" "/"))))

(defn sign [url secret]
  (let [key (prepare-secret secret)
        key-spec (SecretKeySpec. key "HmacSHA1")
        uri (uri/uri url)
        request (str (:path uri) "?" (:query uri))
        mac (Mac/getInstance "HmacSHA1")
        n (. mac init key-spec)
        sig-bytes (. mac doFinal (.getBytes request))
        sig (Base64/encodeBase64URLSafeString sig-bytes)]
    sig))

(defn signed-url [url client-id secret]
  (let [url (uri/param url "client" client-id)
        sig (sign url secret)]
    (uri/param url "signature" sig)))

(defn google-directions
  "get directions object from google maps api expects origin and destination to maps containing lat lng"
  [{:keys [from to client secret output]}]
  (let [out     (or output "json")
        sign    (if (and client secret) true false)
        url     (str directions-url out)
        orig    (str (:lat from) "," (:lng from))
        dest    (str (:lat to) "," (:lng to))
        dir-url (reduce-kv uri/param url {"origin" orig "destination" dest "sensor" "false" })
        final-url (if sign (signed-url dir-url client secret) dir-url)]
    (json/read-json (slurp final-url))))

(defn google-geocode
  "get geocode object from google maps api"
  [{:keys [address client secret output]} ]
  (let [out     (or output "json")
        sign    (if (and client secret) true false)
        url     (str geocode-url out)
        geo-url (reduce-kv uri/param url {"address" address "sensor" "false"})
        final-url (if sign (signed-url geo-url client secret) geo-url)]
  (json/read-json (slurp final-url))))

