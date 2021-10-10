(ns landscape.http
  "interface with psiclj API. routes are
  POST resoponse - send json with current state. cumulative responses
  POST info      - info about e.g browser agent. screen size
  POST finished  - all done. disable from overwriting responses again
  "
  (:require [ajax.core :refer [POST GET]]))

;; http will fail silently when no server is running
;; but if we're running interatively/REPL, HTTP-DEBUG can be set to locally running psiclj
(def HTTP-DEBUG nil) ;; eg. "0.0.0.0:3001"
(defn get-url
  "return just the input value unless HTTP-DEBUG"
  [rest]
  (str (if
       HTTP-DEBUG
       (str "http://" HTTP-DEBUG "/iddebug/testtask/nover/1/1/")
       "")
   rest))


(defn send-info
  []
  "TODO: send system info (eg browser, system, resolution, window size"
  (POST (get-url "info") {:params {:info "TODO!"} :response-format :json}))

(defn send-resp
  "send cummulative object to API to store in DB."
  [state]
  (println "sending state!")
  (POST (get-url "response")
        {               ;:params (.stringify js/JSON (clj->js @STATE))
         :params state
         :format :json
         })
  (println "state sent!"))
(defn send-finished [] (POST (get-url "finished")))
