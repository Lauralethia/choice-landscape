(ns landscape.core
  (:require
   [landscape.loop :as lp]
   [landscape.key]
   [landscape.view :as view]
   [landscape.sound :as sound]
   [landscape.model.timeline :as timeline]
   [landscape.model :as model :refer [STATE]]
   [landscape.settings :as settings :refer [BOARD current-settings]]
   [goog.string :refer [unescapeEntities]]
   [goog.events :as gev]
   [cemerick.url :as url]
   [cljsjs.react]
   [cljsjs.react.dom]
   [sablono.core :as sab :include-macros true]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
   )
  (:import [goog.events EventType KeyHandler]))

;; boilerplate
(enable-console-print!)
(set! *warn-on-infer* false) ; maybe want to be true

(defn gen-well-list []

  ;; TODO: might want to get fixed timing
  ;;       look into javascript extern (and supply run or CB) to pull from edn/file
  (let [best-side (first (shuffle [:left :right])) ; :up  -- up is too different 20211029
        prob-low (get-in BOARD [:prob :low] ) ; initially 20
        prob-mid (get-in BOARD [:prob :mid] ) ; initially 50
        prob-high (get-in BOARD [:prob :high] ) ; originally 100, then 90 (20211104)
       ]
    (vec (concat
       ;; first set of 16*(3 lowhigh + 3 highlow + 3 lowhigh):
       ;; two close are meh on rewards
       (->
        ; ({:left 100 :right 30 :up 20}, {... :up 30 :right 20})
        (timeline/side-probs prob-low prob-mid best-side)

        ; ({:resp-each-side 24 :left 100...}, {:reps-each-side ...}
        (timeline/add-reps-key 24) ; 24*3 trials per block

        ; 20211216 - remove. now only 2 blocks before deval
        ; ({..:up 20}, {.. :up 30}, {.. :up 20})
        ; timeline/add-head-to-tail

        vec

        ; 20211216 - remove. all equal
        ; prev more trial for first block (19-16 *3 = 9 more)
        ;(assoc-in [0 :reps-per-side] 19)
        
        ;; make it look like what well drawing funcs want
        ((partial mapcat #'timeline/gen-prob-maps))
        ((partial mapv #'timeline/well-trial)))

        ;; removed 2021-12-09
        ; ;; add 4 forced trials where we cant get to the good
        ; ;; far away well. encourage exploring
        ; (filter #(not (-> % best-side :open))
        ;         (timeline/gen-wells
        ;          {:prob-low prob-high
        ;           :prob-high prob-high
        ;           :reps-each-side 2
        ;           :side-best best-side}))

       ;; 20211216 - one set to ensure everything is seen equally at the start
       ;; 3 trials total: lr, lu, ru ... but random
       (timeline/gen-wells
        {:prob-low prob-high
         :prob-high prob-high
         :reps-each-side 1
         :side-best best-side})


       ;; all wells are good:  4 reps of 6 combos
       (timeline/gen-wells
        {:prob-low prob-high
         :prob-high prob-high
         :reps-each-side 9
         ; 20211104 increased from 4 to 8; 8*(2 high/low swap, h=l)*(3 per perm)
         ;; 20211216 inc to 9 (+1 above)
         :side-best best-side})))))

(defn vis-class-to-body []
  (let [vis-class (-> @current-settings :vis-type name)]
    (.. js/document -body (setAttribute "class" vis-class))))
(defn vis-type-from-url [u]
  "desert or mountain from url :anchor. default to desert"
  (let [anchor (get u :anchor "desert")]
    (if (re-find #"mountain" (or anchor "desert")) :mountain :desert)))

(defn -main []
  (gev/listen (KeyHandler. js/document)
              (-> KeyHandler .-EventType .-KEY) ; "key", not same across browsers?
              (partial swap! STATE model/add-key-to-state))
  (add-watch STATE :renderer (fn [_ _ _ state] (lp/renderer (lp/world state))))
  (reset! STATE  @STATE) ;; why ? maybe originally to force render. but we do other updates

  (println "preloading sounds")
  (doall (sound/preload-sounds))

  (let [well-list (gen-well-list)]
    (swap! STATE assoc :well-list well-list)
    ;; update well so well in insturctions matches
    (swap! STATE assoc :wells (first well-list)))

  ;; TODO: fixed iti durations

  ; start with instructions
  ; where lp/time-update will call into model/next-step and disbatch to
  ; instruction/step (and when phase name changes, will redirect to model/step-task)
  ; phase name change handled by phase/set-phase-fresh
  (swap! STATE assoc-in [:phase :name] :instruction)

  ; grab any url parameters and store with what's submited to the server
  (let [u (-> js/window .-location .-href url/url)
        vis (vis-type-from-url u)]
    (swap! STATE assoc-in [:record :url] u)
    (swap! settings/current-settings assoc :vis-type vis))

  (vis-class-to-body) 
  ; start
  (lp/run-start STATE))

(-main)
