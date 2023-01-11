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
   (let [trialdesc (flatten (mapv #(rep-block % (* 3 reps-each-side)) phases))
         default-iti-dur (get-in @current-settings [:times :iti-dur])]
     (mapv #(merge (well-trial (merge % {:side-best side-best}))
                   {:iti-dur default-iti-dur})
           trialdesc))))


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


;; 20221025 use with landscape.mr-times
;; (defn fill-mr-times-block [seq probs]
;;   (for seq (map #(assoc % :prob (% probs) :step 1) (keys probs))))
(defn reduce-time-fill [trial p goodnogood-side]
  (-> (reduce
       (fn [acc side]
         (assoc acc (get goodnogood-side side side)
                (if-let [sideprob (side p)]
                  (assoc (side trial) :prob sideprob :step 1)
                  (side trial))))
       trial (keys trial))
      (dissoc :good :nogood)))
;; (reduce-time-fill {:itidur 500 :good {:open true}, :nogood {:open false}, :up {:open true}} {:good 100 :nogood 20 :up 50} {:good :left :nogood :right})
;; {:itidur 500,
;; ;;  :up {:open true, :prob 50, :step 1},
;; ;;  :left {:open true, :prob 100, :step 1},
;; ;;  :right {:open false, :prob 20, :step 1}}
(defn abs [x] (max x (- x)))
(defn mean [x] (/ (apply + x) (count x)))
(defn max-from-mean [x] (let [m (mean x)] (apply max (map #(abs (- m %)) x))))
(defn count-open [trials] (map (fn[side] (count (filter (fn[t](get-in t [side :open])) trials))) [:good :nogood :up]))
(defn shuffle-blocks
  "generated blocks with 50 trials.
  blocks cannot have equal options for all 3 choices.
  make sure when we combined, we don't compound differences"
  [tdict n]
  (loop [blocks []
         seeds []]
    (let [diffcnt (max-from-mean (count-open (flatten blocks)))
          seeds (take n (shuffle (keys tdict)))]
      (println diffcnt)
      (if (or (empty? blocks) (>= diffcnt 2))
        (recur (map #(% tdict) seeds) seeds)
        [seeds blocks]))))

(defn default-probabilities
  "for MR. probability defines default blocks: init, switch, devalue.
  random if up or nogood side is higher (50) or lower (20) initially"
  [& up-ng-init]
  (let [up-ng-init (or up-ng-init (shuffle [50 20]))
        up-init (first up-ng-init)
        ng-init (second up-ng-init)]
    [{:good 100 :nogood  ng-init :up  up-init }
     {:good 100 :nogood  up-init :up  ng-init }
     {:good 100 :nogood 100 :up 100 }]))


(defn fill-mr-times
  "mk_edn_goodnogood.bash created dict of seeded timing info in mr_times.cljs
  values are vector (ordered) open wells and itidur
  {:seed_12345 [{:good{:open true} :up{:open false} :nogood {:open true} :itidur 1.5}]}
  TODO: should :up always start as the worst?
  "
  [tdict & {:keys [goodnogood probs] :or
            {goodnogood (zipmap [:good :nogood] (shuffle [:left :right]))
             probs nil}}]
  (let [probs (or probs (default-probabilities))
        [seeds blocks] (shuffle-blocks tdict (count probs))]
    ;; for each bseq-p pair, add prob to each side in each trial
    (println "# MR using seeds:" seeds)
    (flatten
     (map (fn [bseq p]
            (map (fn[trial] (reduce-time-fill trial p goodnogood))
                 bseq))
          blocks probs))))
