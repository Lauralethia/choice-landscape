(ns landscape.model.avatar
  (:require [landscape.model.wells :as wells]
            [landscape.utils :as utils]
            [landscape.settings :refer [BOARD]]))

;;avatar
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

(defn avatar-dest
  "set new avatar destitionation. used by readkeys on keypush"
  [{:keys [wells avatar] :as state} dir]
        (let[cx (-> avatar :pos :x)
             cy (-> avatar :pos :y)]
          (case dir
            :left  (-> wells :left :pos) ;{:x 0   :y cy}
            :right (-> wells :right :pos) ;{:x 400 :y cy}
            :up    (-> wells :up :pos) ;{:x cx :y 100}
            :down (:avatar-home BOARD)
            (-> avatar :pos))))

(defn avatar-home? [state]
  (utils/collide? (get-in state [:avatar :pos]) (:avatar-home BOARD)))
