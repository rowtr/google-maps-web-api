(ns google-maps-web-api.core
  (:require (org.bovinegenius [exploding-fish :as uri]))
  (:require [clojure.string :as s])
  (:require [clojure.data.json :as json])
  (:import javax.crypto.spec.SecretKeySpec)
  (:import javax.crypto.Mac)
  (:import org.apache.commons.codec.binary.Base64))

(def base-url               "https://maps.googleapis.com/maps/api/")
(def geocode-url            (str base-url "geocode/"))
(def timezone-url           (str base-url "timezone/"))
(def streetview-url         (str base-url "streetview"))
(def directions-url         (str base-url "directions/"))

(def geo.status.map {
		"OK"  "Success."
		"INVALID_REQUEST" "Request was invalid."
		"ERROR" "There was a problem contacting the Google servers."
		"OVER_QUERY_LIMIT"  "The webpage has gone over the requests limit in too short a period of time."
		"REQUEST_DENIED"  "The webpage is not allowed to use the geocoder."
		"UNKNOWN_ERROR"  "A geocoding request could not be processed due to a server error. The request may succeed if you try again."
		"ZERO_RESULTS"  "No result was found for this GeocoderRequest."
		})

(def dir.status.map {
		"INVALID_REQUEST" "The DirectionsRequest provided was invalid.";
		"MAX_WAYPOINTS_EXCEEDED" "Too many DirectionsWaypoints were provided in the DirectionsRequest. The total allowed waypoints is 8, plus the origin and destination.";
		"NOT_FOUND" "At least one of the origin, destination, or waypoints could not be geocoded.";
		"OK" "The response contains a valid DirectionsResult.";
		"OVER_QUERY_LIMIT" "The webpage has gone over the requests limit in too short a period of time.";
		"REQUEST_DENIED" "The webpage is not allowed to use the directions service.";
		"UNKNOWN_ERROR" "A directions request could not be processed due to a server error. The request may succeed if you try again.";
		"ZERO_RESULTS" "No route could be found between the origin and destination.";
		})

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
  [{:keys [from to client secret] :as args}]
  (let [out         "json"
        args        (dissoc args :client :secret :from :to)
        sign        (if (and client secret) true false)
        url         (str directions-url out)
        orig        (str (:lat from) "," (:lng from))
        dest        (str (:lat to) "," (:lng to))
        args        (assoc args :origin orig :destination dest)
        dir-url     (reduce-kv uri/param url (zipmap (map name (keys args)) (vals args)))
        final-url   (if sign (signed-url dir-url client secret) dir-url)
        response    (json/read-json (slurp final-url))]
    response))

(defn google-geocode
  "get geocode object from google maps api"
  [{:keys [address client secret ] :as args} ]
  (let [out         "json"
        args        (dissoc args :client :secret)
        sign        (if (and client secret) true false)
        url         (str geocode-url out)
        geo-url     (reduce-kv uri/param url (merge (zipmap (map name (keys args)) (vals args))))
        final-url   (if sign (signed-url geo-url client secret) geo-url)
        response    (json/read-json (slurp final-url))]
    response))

(defn google-streetview
  "get streetview image for lat/lng"
  [{:keys [location client secret] :as args}]
  (let [args        (dissoc args :client :secret)
        sign        (if (and client secret) true false)
        loc         (str (:lat location) "," (:lng location))
        url         (str streetview-url)
        args        (assoc args :location loc)
        sv-url      (reduce-kv uri/param url (zipmap (map name (keys args)) (vals args)))
        final-url   (if sign (signed-url sv-url client secret) sv-url)
        response    (clojure.java.io/input-stream final-url)]
    response))

(defn google-timezone
  "get timezone info for lat/lng"
  [{:keys [location client secret] :as args}]
  (let [out         "json"
        args        (dissoc args :client :secret)
        sign        (if (and client secret) true false)
        url         (str timezone-url out)
        loc         (str (:lat location) "," (:lng location))
        args        (assoc args :location loc :timestamp (str (quot (System/currentTimeMillis) 1000)))
        tz-url      (reduce-kv uri/param url (zipmap (map name (keys args)) (vals args)))
        final-url   (if sign (signed-url tz-url client secret) tz-url)
        response    (json/read-json (slurp final-url))]
    response))
