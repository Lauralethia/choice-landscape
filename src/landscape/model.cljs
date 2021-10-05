(ns landscape.model
  (:require
   [landscape.key]
   [landscape.utils :as utils]
   [landscape.sound :as snd]
   [landscape.settings :refer [BOARD]]
   [landscape.model.wells :as wells]
   [landscape.model.avatar :as avatar]
   [landscape.model.water :as water]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]])
  (:require-macros [devcards.core :refer [defcard]]))


;; wells
;; uses snd, wells, and avatar. dont want cyclical depends so this goes here
(defn wells-fire-hits
  "if we went to a well. update the parts of the state that aren't a well
  (propagate up into structure)
  increase water
  also should play sound.
  "
  [{:keys [wells time-cur] :as state}]
  (let [hit-side (first (wells/hit-now wells time-cur))
        score (get-in wells [hit-side :score])
        score-state
        (if (some? hit-side)
          (-> state
               (assoc-in [:phase :hit] hit-side)
               (assoc-in [:phase :scored] score)
               (update-in [:phase :sound-at] #(snd/feedback-snd score time-cur %))
               ;; move avatar back home
               (assoc-in [:avatar :destination] (avatar/avatar-dest state :down))
               (assoc-in [:avatar :move-count] 0))
          state)]
    ;; update score
    (if score
      (-> score-state
          (update-in [:water] #(water/water-inc % time-cur))
          )
      score-state)))



;; :key {:until nil :want [] :next nil :have nil}
(defn read-keys
  "read any keypush, clear, and translate to avatar motion"
  [{:keys [key wells] :as state}]
  (let [pushed (:have key)
        dir (case pushed
              37 :left
              38 :up
              39 :right
              ;40 :down   ;; maybe disallow
              nil)]
    (if
      ;; chose phase only time we can pick. and only when we haven't already
      ;; and only when the well choice is aviable (open)
      (and (= :chose (get-in state [:phase :name]))
           (some? dir)
           (get-in wells [dir :open]))
      (-> state
          (assoc-in [:avatar :destination] (avatar/avatar-dest state dir))
          (update-in [:avatar :move-count] inc)
          (assoc-in [:phase :picked] dir)
          (assoc-in [:key :have] nil)
          ;(wells-close)
          )
      state)))


(defn set-phase-fresh [name time-cur]
  {:name name :scored nil :hit nil :picked nil :sound-at nil :start-at time-cur})

(defn phase-update [{:keys [phase time-cur] :as state}]
  (let [pname (get phase :name)
        hit (get phase :hit)
        picked (get phase :picked)
        phase-next (cond
                     ;; as soon as we pick, switch to waiting
                     (and (= pname :chose) (some? picked))
                     (assoc phase :name :waiting)

                     ;; as soon as we hit, switch to feedback (sound)
                     (and (= pname :waiting) (some? hit))
                     (assoc phase :name :feedback :sound-at nil :start-at time-cur)

                     ;; restart at chose when avatar's back home
                     (and (= pname :feedback) (avatar/avatar-home? state))
                     (set-phase-fresh :chose time-cur)

                     ;; no change if none needed
                     :else phase)
        ]
    ;; TODO - push current phase onto stack of events (historical record)
    (assoc state :phase phase-next)))



;;  state
(defn next-step
  "does heavy lifting for state changes. update state with next step
  e.g. trigger feedback. move avatar. stop animations"
  [state time]
  (-> state
      read-keys
      avatar/move-avatar
      wells/wells-check-collide
      wells/wells-turn-off
      wells-fire-hits
      water/water-pulse
      phase-update
      ;wells-update-which-open
      ;; wells-update-prob
      ;; check-timeout
      ;; keys-set-want -- not needed, get from well :open state
      ))

(defn add-key-to-state
  "used in core - keypush listener function"
  [state keypress]
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
   :wells (wells/wells-state-fresh nil)
   :water (water/water-state-fresh)
   :phase (set-phase-fresh :chose nil)
   :sprite-picked :astro
   :avatar {:pos {:x 245 :y 0}
            :active-at 0
            :direction :down
            :last-move 0
            :move-count 0
            :destination (:avatar-home BOARD)}})

(def STATE (atom (state-fresh)))
