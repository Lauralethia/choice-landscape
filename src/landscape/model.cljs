(ns landscape.model
  (:require
   [landscape.key]
   [landscape.utils :as utils ]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]])
  (:require-macros [devcards.core :refer [defcard]]))

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

(defn activate-well [state]
  (if nil (assoc-in state [:wells :left :active-at ] 1)) state)

(defn avatar-dest [{:keys [wells avatar] :as state} dir]
        (let[cx (-> avatar :pos :x)
             cy (-> avatar :pos :y)]
          (case dir
            :left  {:x 0   :y cy}
            :right {:x 400 :y cy}
            :up {:x cx :y 100}
            :down {:x 245 :y 260}
            (-> avatar :pos))))

;; :key {:until nil :want [] :next nil :have nil}
(defn read-keys [{:keys [key] :as state}]
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
            (assoc-in [:key :have] nil)
            ))))

(defn next-step
  "TODO: heavy lifting. update state with next step
  e.g. trigger feedback. move avatar. stop animations"
  [state time]
  (-> state
      read-keys
      move-avatar
      activate-well))

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
   :wells {:left  {:step 1 :open true :active-at 0 :color :red}
           :up    {:step 1 :open true :active-at 0 :color :green}
           :right {:step 1 :open true :active-at 0 :color :blue}}
   :avatar {:pos {:x 245 :y 0}
            :active-at 0
            :direction :down
            :last-move 0
            :destination {:x 245 :y 260}}})

(def STATE (atom (state-fresh)))
