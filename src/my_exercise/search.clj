(ns my-exercise.search
  (:require [hiccup.page :refer [html5]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [clojure.string :refer [join blank?]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [ring.util.codec :as codec]
            [clojure.edn :as edn]
            ))

(def config {
             :GOOGLE_API_KEY "AIzaSyDKilEjnqlN_KuTzkFYhzNtxHy3xqqmuaQ"
             }
  )


(defn get_address_components [params components]
  (filter #(not (blank? %)) (map (fn [component] (component params)) components))
  )

(defn get_address [params]
  (join " " (get_address_components params [:street :street2 :city :state :zip])))

(defn lookup_divisions [address]
  (let [response (client/get "https://www.googleapis.com/civicinfo/v2/representatives"
          {:query-params {:address (codec/form-encode address) :includeOffices false :key (:GOOGLE_API_KEY config)}
          :throw-exceptions false})
        divisions (map #(subs(str %) 1) (keys (:divisions (json/read-str (:body response) :key-fn keyword))))

        ]

    (if (not= (:status response) 200)
      (throw (Exception. (str "Address not found. Status: " (:status response))))
      divisions
      )
    )
  )

(defn lookup_elections [divisions]
  ;
  (let [response (client/get "https://api.turbovote.org/elections/upcoming"
                             {:query-params {:district-divisions (join "," divisions)} :throw-exceptions false
                              })

        ]
    (if (not= (:status response) 200)
      (throw (Exception. (str "Divisions not found. Status: " (:status response))))
      (edn/read-string (:body response) )
      )

    )
  )


(defn header [_]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
   [:title "Your next election"]
   [:link {:rel "stylesheet" :href "default.css"}]])

(defn election_results [request]
  (try
    (let [election_info (lookup_elections (lookup_divisions (get_address (:params request))))]
      election_info
      ; this is as far as I got; I ran out of time and I don't have experience reading edn
      )
    (catch Exception e (str e)) ;TODO better exception handling
    )
  )



(defn page [request]
  (html5
    (header request)
    (election_results request)
    )

  )
