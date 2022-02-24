(ns landscape.model.wells
(:require [landscape.settings :refer [current-settings]]
          [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
          [landscape.utils :as utils]
          [landscape.sound :as snd]))

(defn well-pos
  "{:x # :y #} for a number of steps/count to a well"
  [side step]
  (let [center-x (:center-x @current-settings)
        bottom-y (- (:bottom-y @current-settings) 5)
        move-by (reduce + (take step (:step-sizes @current-settings)))]
    (case side
      :left  {:x (- center-x move-by) :y bottom-y}
      :up    {:x center-x             :y (- bottom-y
                                            (* move-by (:top-scale @current-settings)))}
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
                 {:left  {:step 2 :open true :active-at 0 :prob 90 :color :red}
                  :up    {:step 1 :open true :active-at 0 :prob 20 :color :green}
                  :right {:step 1 :open true :active-at 0 :prob 50 :color :blue}})]
    (reduce #(update %1 %2 (partial well-add-pos %2)) wells (keys wells))))

(defn wells-set-open-or-close
  ^{:doc "set if we can go to a well (and if we show a bucket)"
    :test (fn[] (assert (=
                {:wells {:left {:open false}}}
                (wells-set-open-or-close {:wells {:left {:open true}}} [:left] false)
                )))}
 [{:keys [wells] :as state} sides open?]
 (reduce #(assoc-in %1 [:wells %2 :open] open?) state sides))

(defn wells-close [{:keys [wells] :as state}]
  (wells-set-open-or-close state [:left :up :right] false))

(defn wells-open-rand [{:keys [wells] :as state}]
  (wells-set-open-or-close state (take 2 (shuffle [:left :up :right])) true))

(defn fire-empty [well time-cur]
  (-> well
      (assoc :active-at time-cur)
      (assoc :score nil)))

(defn all-empty
  "all wells animated as empty. used for timeout"
  [{:keys [wells time-cur] :as state}]
  (let [wells-empty (-> wells
                        (update :left  #(fire-empty %1 time-cur))
                        (update :up    #(fire-empty %1 time-cur))
                        (update :right #(fire-empty %1 time-cur)))]
    (-> state
        (assoc :wells wells-empty)
        (wells-close))))
;; TODO: this should be subsumed by phase-next
;; update-next-trial also updates well. dont need to do here
;; but that only updates :phase
(defn wells-update-which-open
        "when just came into chose state, set wells "
        [{:keys [time-cur phase trial well-list] :as state}]
        (let [phasechange? (= (:start-at phase) time-cur)
              phasename (:name phase)]
          (if (not phasechange?)
            state
            (case phasename
              ;; get trial0 well-list
              ;; previously chose used random pick: (wells-open-rand state)
              :chose (assoc state :wells  (get well-list (dec trial)))
              ;; when waiting close all wells
              :waiting (wells-close state)
              :timeout (all-empty state)
              ;; :feedback state
              state))))

(defn hit-now
  [wells time-cur]
  (filter some? (map  #(if (= time-cur (-> wells % :active-at)) % nil) (keys wells))))

(defn activate-well
  "when collision with 'apos' check prob and set score.
  NB. see any :active-at == :time-cur to trigger other things"
  [apos now well]
  (if (and (= 0 (:active-at well))
           (utils/collide? (:pos well) apos))
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


(defn well-off
  "stop well animation after given time
  determined by settings/@current-settings:wait-time"
  [time well]
  (update-in well [:active-at] #(if (> (- time %) (:wait-time @current-settings)) 0 %)))

(defn wells-turn-off [{:keys [wells time-cur] :as state}]
  (assoc state :wells
    (reduce #(update %1 %2 (partial well-off time-cur)) wells (keys wells))))


;; but we probably want to express wells as wide instead of nested
;; and want to be able to easily get at picked and avoided
;; like
;;   :pick-prob # :picked-far? ?
;;   :avoid-prob # :avoid-far? ?
;;   :left-prob # :up-prob # :right-prob #
;;   :left-on ? :up-on ? :right-on ?
;;   :left-far ? :up-far ? :right-far ?
;; this could be fn as wells/wide-info
(defn zipmap-fn [keys fnx] (zipmap keys (mapv fnx keys)))
(defn side-wide [wells side]
  (let [items [:prob :step :open]
        keys  (mapv #(keyword (str (name side) "-" (name %))) items)
        info  (mapv #(get-in wells [side %]) items)]
    (zipmap keys info)))

;; these functions could be better composed
(defn well-str
  "is a well close or far? whats the payout probability?
  only used for encoding. output like Rc80"
  [prefix {:keys [step prob] :as  well}]
  (str prefix
       (if (> step 1) "f" "c")
       prob))
(defn block-encode
 "want block string like Lc10Uf100Rc80"
  [{:keys [left up right] :as wells}]
  {:blockstr
   (str  (well-str "L" left)
         (well-str "U" up)
         (well-str "R" right))})

(defn wide-info
  "'wide' format info for well side x well info. for http post"
  [wells]
  (merge (block-encode wells)
         (reduce #'merge (mapv #(side-wide wells %) [:left :up :right]))))

(defn which-open [wells]
  (let [sides (keys wells)
        open-wells (filter (fn [side] (get-in wells [side :open])) sides)]
    (clojure.string/join "-" (mapv #'name open-wells))))
(defn avoided
  "find the well we didn't pick that was open.
  returns list but should only have one output"
  [wells picked]
  (filter #(and (not= picked %) (get-in wells [% :open])) (keys wells)))
(defn wide-info-picked
  "after picking wide info (avoided and picked). useful for http post"
  [wells picked]
  (let [avoided (first (avoided wells picked))]
    {
     :trial-choices (which-open wells)
     :avoided avoided
     :picked-prob (get-in wells [picked :prob])
     :picked-step (get-in wells [picked :step])
     :avoided-prob (get-in wells [avoided :prob])
     :avoided-step (get-in wells [avoided :step])}))
