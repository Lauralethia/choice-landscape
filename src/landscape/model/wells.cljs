(ns landscape.model.wells
(:require [landscape.settings :refer [BOARD]]
          [landscape.utils :as utils]
          [landscape.sound :as snd]))

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

(defn wells-update-which-open
  "when just came into chose state, set wells
  TODO: maybe not random but set before"
  [{:keys [time-cur phase] :as state}]
 (if (and (= (:start-at phase) time-cur)
          (= :chose (:name phase)))
   (wells-open-rand state)
   state))

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


(defn well-off [time well]
  ;; TODO: 1000 should come from sprite total-size?
  (update-in well [:active-at] #(if (> (- time %) (:wait-time BOARD)) 0 %)))

(defn wells-turn-off [{:keys [wells time-cur] :as state}]
  (assoc state :wells
    (reduce #(update %1 %2 (partial well-off time-cur)) wells (keys wells))))
