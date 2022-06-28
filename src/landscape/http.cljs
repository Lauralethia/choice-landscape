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
(def HTTP-DEBUG nil) ;; eg. (set! landscape.http/HTTP-DEBUG "0.0.0.0:3001")
(defn get-url
  "return just the input value unless HTTP-DEBUG"
  [rest]
  (str (if
       HTTP-DEBUG
       (str "http://" HTTP-DEBUG "/iddebug/testtask/1/1/")
       "")
   rest))

(defn send-local-ttl
  "Want indicator of trial and event in recorded output of s/EEG.
  But the browser doesn't have access to hardware and libaries are mostly python.
  So send TTLs to separate python http server.
  When there's not local-ttl-server, nothing will happen"
  [local-ttl-server ttl]
  (when local-ttl-server
    (GET (str local-ttl-server "/" ttl))))

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

(defn set-mturk-code-opener [code]
  ;; "taskCompleteCode" defined by psiclj mturk.js
  ;; window.opener.taskCompleteCode("4fcdb")
  (if (.. js/window -opener)
   (do (console.log "looking for taskCompleteCode in" (.. js/window -opener))
       (console.log "that is " (.. js/window -opener -taskCompleteCode))
       (when-let [code-func (.. js/window -opener -taskCompleteCode)]
         (console.log "opener func to set code:" code-func)
         ;; true if success. can close the window
         (when (code-func code)
           (. js/window close))))
    (console.log "WARNING: no opener window to send competion code to" (.. js/window -opener))))

(defn set-mturk-code
  "try to set the opener pages complete code and submit"
  [resp STATE]
  (let [code (or (:code resp) "COMPLETE")]
    (println "code:" code)
    (println "resp:" resp)
    (swap! STATE assoc-in [:record :mturk :code] code)
    (println "state record mturk:" (-> @STATE :record :mturk :code))
    (set-mturk-code-opener code)
    ))

(defn send-finished-state-code-and-close [STATE]
        (let [finish-url (get-url "finish")] 
          (POST finish-url {:handler (fn [resp] (set-mturk-code resp STATE))
                            :response-format :json :keywords? true})))
