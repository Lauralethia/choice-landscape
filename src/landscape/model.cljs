(ns landscape.model
  (:require
   [landscape.key]
   [landscape.utils :as utils]
   [landscape.sound :as snd]
   [landscape.key :as key]
   [landscape.settings :refer [BOARD]]
   [landscape.instruction :as instruction]
   [landscape.model.wells :as wells]
   [landscape.model.avatar :as avatar]
   [landscape.model.water :as water]
   [landscape.model.phase :as phase]
   [landscape.model.survey :as survey]
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


;;  state
(defn step-task
  "does heavy lifting for state changes. update state with next step
   e.g. trigger feedback. move avatar. stop animations
  run by next-step from loop/time-update after updating state:time-cur"
  [state time]
  (-> state
      read-keys
      avatar/move-avatar
      wells/wells-check-collide     ; start animation
      wells/wells-turn-off          ; stop animations
      wells-fire-hits               ; update not well stuff on well hit
      water/water-pulse             ; ossilate water size: additional score/win indication
      phase/phase-update            ; discrete phases
      ; well update handled by phase update
      wells/wells-update-which-open ; set random wells to be used. clear when not using
      ;; wells-update-prob
      ;; check-timeout
      ;; keys-set-want -- not needed, get from well :open state
      ))

(defn next-step
  "disbatch to instruction or task
  run by loop/time-update after updating state:time-cur "
  [{:keys [phase] :as state} time]
  (case (:name phase)
    :instruction (instruction/step state time)
    :survey (survey/step state time)
    ;; :done

    ;:chose, :waiting, :feedback
    (step-task state time)))


(defn add-key-to-state
  "updates state with keypush. :key {:have :time}
  used in core for simulated keypress on button push"
  [state keypress]
  (let [key (landscape.key/keynum keypress)]
    (if keypress
             ;(= :chose (get-in state [:phase :name])))
      (-> state
          (assoc-in [:key :have] key)
          (assoc-in [:key :time] (utils/now)))
      state)))

(defn state-fresh
  "initial state. empty timing"
  []
  {
   :running? true
   :trial 1
   :start-time 0
   :time-cur 0
   :key (key/key-state-fresh)
   :well-list []
   :wells (wells/wells-state-fresh nil) ; replaced by phase/update-next-trial
   :water (water/water-state-fresh)
   :phase (phase/set-phase-fresh :chose nil)
   :sprite-picked :astro
   :record [] ; what we'll send to server to record onset times, choice, score. see [phase/phone-home]
   :avatar {:pos {:x 245 :y 0}
            :active-at 0
            :direction :down
            :last-move 0
            :move-count 0
            :destination (:avatar-home BOARD)}})

(def STATE (atom (state-fresh)))
