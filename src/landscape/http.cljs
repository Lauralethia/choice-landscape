(ns landscape.http
  "interface with psiclj API. routes are
  POST resoponse - send json with current state. cumulative responses
  POST info      - info about e.g browser agent. screen size
  POST finished  - all done. disable from overwriting responses again
  "
  (:require
   [ajax.core :refer [POST GET]]))

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
  (POST (get-url "response")
        {               ;:params (.stringify js/JSON (clj->js @STATE))
         :params state
         :format :json
         }))
(defn send-finished [] (POST (get-url "finish")))

(defn set-mturk-code
  "try to set the opener pages complete code and submit"
  [resp STATE]
  (let [code (:code resp)
        opener-func (.. js/window -opener -taskCompleteCode)]
    (println code)
    (println resp)
    (println opener-func)
    (swap! STATE assoc-in [:record :mtruk :code] code)
    (println (-> @STATE :record :mturk))
    ;; "taskCompleteCode" defined by psiclj mturk.js
    ;; window.opener.taskCompleteCode("4fcdb")
    (when (opener-func code)
      (. js/window close))))

(defn send-finished-state-code-and-close [STATE]
        (let [finish-url (get-url "finish")] 
          (println "posting finish to" finish-url)
          (POST finish-url {:handler (fn [resp] (set-mturk-code resp STATE))
                            :response-format :json :keywords? true})))
