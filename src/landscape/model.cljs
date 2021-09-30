(ns landscape.model
  (:require
   [landscape.key]
   [landscape.utils :as utils ]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]])
  (:require-macros [devcards.core :refer [defcard]]))


(def BOARD "bounds and properties of the background board"
  {:center-x 250
   :bottom-y 260
   :avatar-home {:x 250 :y 250}
   :step-sizes [100 50 25 25 25 25]
   })

(defn which-dir [cur dest]
  (cond
    (= cur dest) :none
    (> (:x cur) (:x dest)) :left
    (< (:x cur) (:x dest)) :right
    (< (:y cur) (:y dest)) :down
    (> (:y cur) (:y dest)) :up
    :else :none))

(defn move-closer [cur dest step dir]
  (case dir
    :none  cur
    :left  (merge cur {:x (max (- (:x cur) step) (:x dest))})
    :right (merge cur {:x (min (+ (:x cur) step) (:x dest))})
    :up    (merge cur {:y (max (- (:y cur) step) (:y dest))})
    :down  (merge cur {:y (min (+ (:y cur) step) (:y dest))})))

(defn move-active-time [a b now time]
        (cond
            (= a b) 0                     ;; stop animating
            (and (= time 0) (not= a b)) now ;; start now
            (and (not= time 0) (not= a b)) time ;; maintain prev starting time
            true 0))

(defn move-avatar [{:keys [avatar time-cur] :as state}]
  (let [step-size 10
        time-step 30
        time-delta (- time-cur (:last-move avatar))
        cur (:pos avatar)
        dest (:destination avatar)
        dir (which-dir cur dest)
        moved (move-closer cur dest step-size dir)]
      (-> state
        (assoc-in [:avatar :pos] moved)
        (assoc-in [:avatar :last-move] time-cur)
        (update-in [:avatar :direction] #(if (= :none dir) % dir))
        (update-in [:avatar :active-at]
                  (partial move-active-time dest moved time-cur)))))

(defn collide?
  "euclidian: sqrt sum of squares"
  [a b]
  (let [thres 10]
    (<  (.pow js/Math
              (+ (.pow js/Math (- (:x b) (:x a)) 2)
                 (.pow js/Math (- (:y b) (:y a)) 2))
              .5)
        thres)))

(defn activate-well
  "when collision with 'apos' check prob and set score.
  NB. see any :active-at == :time-cur to trigger other things"
  [apos now well]
  (if (and (= 0 (:active-at well))
           (collide? (:pos well) apos))
    (assoc well :active-at now :score (utils/prob-gets-points? (:prob well)))
    well))

(defn wells-check-collide
        "use active-well to set active-at (start animation) if avatar is over well"
        [{:keys  [wells avatar time-cur] :as state}]
        (let [apos (:pos avatar)]
          (assoc state :wells
                 (reduce #(update %1 %2 (partial activate-well apos time-cur))
                         wells
                         (keys wells)))))

(defn avatar-dest
  "set new avatar destitionation. used by readkeys on keypush"
  [{:keys [wells avatar] :as state} dir]
        (let[cx (-> avatar :pos :x)
             cy (-> avatar :pos :y)]
          (case dir
            :left  {:x 0   :y cy}
            :right {:x 400 :y cy}
            :up {:x cx :y 100}
            :down (:avatar-home BOARD)
            (-> avatar :pos))))

;; :key {:until nil :want [] :next nil :have nil}
(defn read-keys
  "read any keypush, clear, and translate to avatar motion"
  [{:keys [key] :as state}]
  (let [pushed (:have key)
        dir (case pushed
              37 :left
              38 :up
              39 :right
              40 :down   ;; maybe disallow
              nil)]
    (if (not dir) state
        (-> state
            (assoc-in [:avatar :destination] (avatar-dest state dir))
            (update-in [:avatar :move-count] inc)
            (assoc-in [:key :have] nil)
            ))))

(defn well-off [time well]
  ;; TODO: 1000 should come from sprite total-size?
  (update-in well [:active-at] #(if (> (- time %) 1000) 0 %)))

(defn wells-turn-off [{:keys [wells time-cur] :as state}]
  (assoc state :wells
    (reduce #(update %1 %2 (partial well-off time-cur)) wells (keys wells))))

(defn hit-now
  [wells time-cur]
  (filter some? (map  #(if (= time-cur (-> wells % :active-at)) % nil) (keys wells))))

;;  water

(defn water-state-fresh [] {:level 10 :scale 10 :active-at 0})

(defn water-inc
  "increase water level. should probably only happen when well is hit"
  [water time-cur]
  (-> water
      (update-in [:level] #(+ 1 %))
      (assoc-in [:active-at] time-cur)))

(defn water-pulse-water
  "if active-at is not zero. modulate water level with a sin wave.
  will set active-at to zero when pulsed long enough"
  [water time-cur]
  (let [sin-dur 500                   ; ms
        npulses 1                     ; n times to go up and back down
        dur-total (* npulses sin-dur)
        mag 2                         ; % scale increase
        time-at (:active-at water)
        dur-cur (- time-cur time-at)
        active-at (if (> dur-cur dur-total) 0 (:active-at water))
        level (:level water)
        scale (if (not= active-at 0)
                (+ 10 level (js/Math.sin (* 2 js/Math.PI (/ dur-total dur-cur))))
                level)]
    (-> water
        (assoc-in [:scale] scale)
        (assoc-in [:active-at] active-at))))

(defn water-pulse [state]
  (update state :water #(water-pulse-water % (:time-cur state))))


;;  well
(defn wells-fire-hits
  "if we went to a well. update the parts of the state that aren't a well
  (propagate up into structure)
  increase water
  also should play sound.
  "
  [{:keys [wells time-cur] :as state}]
  (let [hit-side (first (hit-now wells time-cur))
        score (get-in wells [hit-side :score])
        score-state
        (if (some? hit-side)
          (-> state
              ;; (update-in [:events] concat (str time-cur (now)))
              (update-in [:phase :picked] hit-side)
              (assoc-in [:avatar :destination] (avatar-dest state :down))
              (assoc-in [:avatar :move-count] 0))
          state)]
    (if score
      (-> score-state
          (update-in [:water] #(water-inc % time-cur))
          (assoc-in [:phase :scored] true)
          )
      score-state)))

(defn well-pos
  "{:x # :y #} for a number of steps/count to a well"
  [side step]
  (let [center-x (:center-x BOARD)
        bottom-y (- (:bottom-y BOARD) 5)
        move-by (reduce + (take step (:step-sizes BOARD)))]
    (case side
      :left  {:x (- center-x move-by) :y bottom-y}
      :up    {:x center-x             :y (- bottom-y move-by)}
      :right {:x (+ center-x move-by) :y bottom-y}
      {:x 0 :y 0})))

(defn well-add-pos
  "uses :step to calc :pos on well info (e.g. map within [:wells :left]) "
  [side {:keys [step] :as well}]
  (assoc well :pos (well-pos side step)))

(defn wells-state-fresh
  ;; include default settings
  [wells]
  (let [wells (if wells
                 wells
                 {:left  {:step 1 :open true :active-at 0 :prob 50 :color :red}
                  :up    {:step 1 :open true :active-at 0 :prob 50 :color :green}
                  :right {:step 1 :open true :active-at 0 :prob 50 :color :blue}})]
    (reduce #(update %1 %2 (partial well-add-pos %2)) wells (keys wells))))


;;  state
(defn next-step
  "does heavy lifting for state changes. update state with next step
  e.g. trigger feedback. move avatar. stop animations"
  [state time]
  (-> state
      read-keys
      move-avatar
      wells-check-collide
      wells-turn-off
      wells-fire-hits
      water-pulse
      ;; wells-update-prob
      ;; check-timeout
      ;; keys-set-want
      ))

(defn add-key-to-state [state keypress]
  (let [key (landscape.key/keynum keypress)]
    (if keypress
      (-> state
          (assoc-in [:key :have] key)
          (assoc-in [:key :time] (utils/now)))
      state)))

(defn state-fresh
  "initial state. empty timing"
  []
  {
   :running? true
   :start-time 0
   :time-cur 0
   :key {:until nil :want [] :next nil :have nil}
   :wells (wells-state-fresh nil)
   :water (water-state-fresh)
   :phase {:name :chose :scored nil :picked nil}
   :sprite-picked :astro
   :avatar {:pos {:x 245 :y 0}
            :active-at 0
            :direction :down
            :last-move 0
            :move-count 0
            :destination (:avatar-home BOARD)}})

(def STATE (atom (state-fresh)))
