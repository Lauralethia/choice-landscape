(ns landscape.model.timeline
  (:require
   [landscape.model.wells :as wells]
   [landscape.settings :as settings :refer [BOARD]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]))

(defrecord well [step open prob active-at pos])

(defn well-steps
  "best side is more steps away than others
  step size defined in settings/BOARD"
  [side side-best] (if (= side side-best) 2 1))
(defn well-side
  "use defrecord to creat a well"
  [side side-best side-disabled prob]
  (let [open (not= side side-disabled)
        step (well-steps side side-best)
        start-at 0]
    (->well
     step open prob start-at (wells/well-pos side step))))
(defn well-trial
  [{:keys [left right up side-disabled side-best]}]
  {:left (well-side :left side-best side-disabled left)
   :up  (well-side :up side-best side-disabled up)
   :right (well-side :right side-best side-disabled right)})

;; side-best is far side. stays constant
;; 1. want to gen set w/flip which not-side-best gets high/low prob
(defn side-probs
  "create array of different side-probabilty pairings. like
    [{:left 50 :right 20 :up 90}
     {:left 20 :right 50 :up 90}]"
  [high low side-best]
  (let [others (filter #(not= side-best %) [:left :up :right])
        fst (first others)
        snd (second others)
        prob-high (get-in BOARD [:prob :high])]
    [{fst high snd low side-best prob-high}
     {fst low snd high side-best prob-high}]))
;; 2. then shuffle which is disabled for all sides
(defn rep-block [prob-map ntrials]
  (let [disabled-map (mapv #(merge prob-map {:side-disabled %}) [:left :up :right])
        nrep (/ ntrials 3)]
    (shuffle (flatten (repeat nrep disabled-map)))))
;; 3. then recombine
(defn gen-wells
  [{:keys [prob-low prob-high reps-each-side side-best] :as props}]
  (let [phases (shuffle (side-probs prob-high prob-low side-best))
        trialdesc (flatten (mapv #(rep-block % (* 3 reps-each-side)) phases))]
    (mapv #(well-trial (merge % {:side-best side-best})) trialdesc)))


(defn gen-example []
  (gen-wells {:prob-low (-> BOARD :prob :low)
              :prob-high (-> BOARD :prob :mid)
              :reps-each-side 1 ;; # trials before switch high and low
              :side-best :left}))
