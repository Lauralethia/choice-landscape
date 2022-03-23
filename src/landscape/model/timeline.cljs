(ns landscape.model.timeline
  (:require
   [landscape.model.wells :as wells]
   [landscape.settings :as settings :refer [current-settings]]))

(defrecord well [step open prob active-at pos])

(defn well-steps
  "best side is more steps away than others
  step size defined in settings/@current-settings"
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
    [{:left 50 :right 20 :up 90 :side-best :up}
     {:left 20 :right 50 :up 90 :side-best :up}]"
  ([high low side-best]
   (side-probs (get-in @current-settings [:prob :high]) high low side-best))
  ([best high low side-best]
   (let [others (filter #(not= side-best %) [:left :up :right])
         fst (first others)
         snd (second others)]
     [{fst high snd low side-best best :side-best side-best}
      {fst low snd high side-best best :side-best side-best}])))
;; 2. then shuffle which is disabled for all sides
(defn rep-block [prob-map ntrials]
  (let [disabled-map (mapv #(merge prob-map {:side-disabled %}) [:left :up :right])
        nrep (/ ntrials 3)]
    (shuffle (flatten (repeat nrep disabled-map)))))
;; 3. then recombine
(defn gen-wells
  "2 args if we want to specify good well probability. 1 with keys otherwise"
  ([{:keys [prob-low prob-high reps-each-side side-best] :as props}]
   (let [phases (shuffle (side-probs prob-high prob-low side-best))]
     (gen-wells phases side-best reps-each-side )))
  ([prob-goodwell {:keys [prob-low prob-high reps-each-side side-best] :as props}]
   (let [phases (shuffle (side-probs prob-goodwell prob-high prob-low side-best))]
     (gen-wells phases side-best reps-each-side)))
  ([phases side-best reps-each-side ]
   (let [trialdesc (flatten (mapv #(rep-block % (* 3 reps-each-side)) phases))]
     (mapv #(well-trial (merge % {:side-best side-best})) trialdesc))))


;; 20211203 - redo for more eplict stucture
(defn gen-prob-maps
  " expand prob map into array of structs well-trial can use
  that is: add side-disabled and remove reps-per-side
  (make-reps {:left 100 :right 80 :up 20 :reps-per-side 2})"
  [{:keys [left right up reps-per-side] :as prob-map}]
  (let [disabled-map (mapv #(-> prob-map
                                (merge {:side-disabled %})
                                (dissoc :reps-per-side))
                           [:left :up :right])]
    (shuffle (flatten (repeat reps-per-side disabled-map)))))

(defn add-head-to-tail "want aditional reversal. append first to last" [l]
  (concat l (list  (nth l 0))))

(defn add-reps-key "so (make-reps) can look into :reps-per-side"
  [prob-map reps-per-side]
  (mapv #(merge {:reps-per-side reps-per-side} %) prob-map))

(defn gen-example []
  (gen-wells {:prob-low (-> @current-settings :prob :low)
              :prob-high (-> @current-settings :prob :mid)
              :reps-each-side 1 ;; # trials before switch high and low
              :side-best :left}))

(defn gen-example-more []
  (-> (side-probs 20 30 :left)         ; ({:left 100 :right 30 :up 20}, {... :up 30})
      (add-reps-key 2)                 ; ({:resp-each-side 2 :left 100...}, {:reps-each-side ...}
      add-head-to-tail                 ; ({..:up 20}, {.. :up 30}, {.. :up 20})
      vec
      (assoc-in [0 :reps-per-side] 3)
      ((partial mapcat #'gen-prob-maps))
      ((partial mapv #'well-trial))))

