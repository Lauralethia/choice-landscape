(ns landscape.model.floater
  (:require
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   ))

(defrecord pos [x y])
(defrecord floater [alpha size step-cur step-off body ^pos pos])
(defn floater-new
  "defaults for a floater. probably 'z' text with 20 steps"
  ([^pos start-pos]
   (floater-new
    start-pos 50
    (html [:p {:style {:font-size "32px" :font-weight "bold"}} "z"])))
  ([start-pos step-off body]
   ;; alpha 1.0 size 100% at step 0
   (->floater 1 100 0 step-off body start-pos)))

(defn rand-init
  "set initial realitive to position. min alpha is .5. min size is 50%"
  [^floater f]
  (-> f
      (update-in [:pos :x] #(+ % (rand-int 30) -15))
      (update-in [:pos :y] #(+ % (rand-int 15) -7))
      (assoc :alpha (+ .5 (/ (rand-int 15) 30)))
      (assoc :size (* 100 (+ .5 (/ (rand-int 15) 30))))))

(defn move-up
  "move closer to top of screen. reduce alpha and random horz jitter"
  [^floater f]
  (-> f
      (update-in [:pos :x] #(+ % (rand-int 5) -2.5))
      (update-in [:pos :y] #(- % (rand-int 9) 1))
      (update :step-cur inc)
      (update :alpha #(- % (/ 1 (:step-off f))))
      (update :size #(- % (/ 100 (:step-off f))))))

(defn keep?
  "should remove when has more steps than asked for"
  [^floater f]
  (<= (:step-cur f) (:step-off f)))

(defn zzz-new [^pos pos num]
  (map #(rand-init (floater-new pos)) (range num)))

(defn update-state
        "move floaters up. remove if have for long enough"
        [{:keys [zzz] :as state}]
        (assoc state :zzz
               (->> (map #(move-up %) zzz)
                    (filter #'keep?))))



;; coins
(defn move-down
  "intended for coin/water drops. move them down to the"
  [^floater f y-dest & {:keys [step-sz] :or {step-sz 10}}]
  (-> f
       (update-in [:pos :x] #(+ % (rand-int 5) -2.5))
       ;; move down on average step-sz but jitter by .5
       ;; useful so everything doesn't end up in the same place
       (update-in [:pos :y] #(+ % (- (* 1.5 step-sz) (rand-int step-sz))))
       (update :step-cur inc)))

(defn reached?
  "reached bottom OR ran out of time to get there"
  [^floater f y-end]
  (or
   (>= (get-in f [:pos :y]) y-end)
   (>= (:step-cur f) (:step-off f))))

;; TODO: this could maybe be water too. not just a coin
(defn coin-new [^pos pos]
  (floater-new pos 15 (html [:img {:style {:scale "20%"} :src "imgs/starcoin.png"} ])))

(defn coin-addto-state
  "update state with a new floating coin. only add a new coin if there is a destination for it (defined in settings)"
  [{:keys [coins] :as state}
   & {:keys [pos] :or
            {pos {:x 300 :y 0}}}]
  (let [y-pos (get-in state [:record :settings :pile-y])]
    (if (not y-pos)
      state
      (update-in
       state
       [:coins :floating]
       #(concat % [(coin-new pos)])))))

(defn coin-update-state
  "move coins down. add to pile if done"
  [{:keys [coins] :as state}]
  (let [y-pos (get-in state [:record :settings :pile-y] 0)
        new-pos (map #(move-down % y-pos) (:floating coins))
        coins-reached (group-by #(reached? % y-pos) new-pos)]
    (-> state
        (assoc-in [:coins :floating] (get coins-reached false))
        (update-in [:coins :pile]   #(concat (get coins-reached true) %)))))
