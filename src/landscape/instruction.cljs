(ns landscape.instruction
  (:require
   [landscape.sprite :as sprite]
   [landscape.key :as key]
   [landscape.utils :as utils]
   [landscape.settings :as settings :refer [BOARD]]
   [landscape.model.water :as water]
   [landscape.model.wells :as wells]
   [landscape.model.phase :as phase]
   [landscape.model.avatar :as avatar]
   [landscape.model.phase :as phase]
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]))

(defn find-far-well [{:keys [wells] :as state}]
  (apply max-key #(:step (val %)) wells))
(defn find-close-well [{:keys [wells] :as state}]
  (apply min-key #(:step (val %)) wells))

;; Idea is to present sequential instructions using only 3 or 4 keys
;; instruction boxes should be positioned close to the thing they explain
;;
;; we are likely to want to animate a thing. so having :start and :stop
;; functions that can manipualte state will be useful
;; To that end, instruction has it's own ~step~ function that will not include
;; the functions that update phase or react to water-well+avatar "hits"
;;
;; instruction phase should have weither or not we can proceed
;; and we'll only pass onto readkeys if that's true
;; otherwise keys fn handles it
;; if key is nil, default to okay to proceed
(defn avatar-example [{:keys [time-cur sprite-picked] :as state} avatar-name]
  (html [:div
         {:key avatar-name
          :style {:margin "auto"  :margin-bottom "10px"
                  :background (if (= sprite-picked avatar-name) "blue" "gray")}}
         (sprite/avatar-disp {:time-cur time-cur :sprite-picked avatar-name}
                             {:direction :down :active-at 1})]))
(defn next-sprite [dir cur]
        (let [names (keys sprite/avatars)
              fncng (if (= dir :down) inc dec)
              ntot  (count names)
              cur-i (or (.indexOf names cur) -1)
              next-i (mod (fncng cur-i) ntot)]
          (nth names next-i)))

(defn position-next-to-well
  "move position over by "
  [well]
  (-> well :pos (update :x #(+ (:width sprite/well) 5 %))))
(def INSTRUCTION
  [
   {:text (fn[state]
            (html
             [:div [:h1  "Welcome to our game!"]
              [:br]
              "Push the keyboard's " [:b "right arrow key"] " to get to the next instruction. "
              [:br] [:br]
              "You can also click the \">\" button below"
              ]))
    :start identity
    ;; NB. fullscreen in firefox removes background and centering
    ;;  on either .-body "main-container"
    :stop (fn[state] ;; (try (-> js/document
                    ;;          (.getElementById "main-container")
                    ;;          .requestFullscreen))
            state)
    :key nil}
   {:text (fn[state]
            (html
             [:div  "Before we start, pick a character!"
              [:br]
              "In the game, all characters are equal"
              [:ul
               [:li "Use the " [:b "up arrow"] " to " [:u "change"] " your selection."]
               [:li "Use the " [:b "right arrow"] " to continue " [:br]
                " when done choosing"]]
              [:div#pick-avatars
               (map (partial avatar-example state) (keys sprite/avatars))]]))
    :start identity
    :stop (fn[state] (assoc-in state [:record :avatar] (:sprite-picked state)))
    :key {:down (fn[state]
                  (update state :sprite-picked (partial next-sprite :down)))
          :up (fn[state]
                (update state :sprite-picked (partial next-sprite :up)))}}
   {:text (fn[_] "You want to fill this oasis with water as fast as you can")
    :pos (fn[_] {:x 50 :y 250})
    :start (fn[{:keys [water time-cur] :as state}]
             (assoc-in state [:water :active-at] time-cur))
    :stop (fn [{:keys [water time-cur] :as state}]
            (assoc-in state [:water] (water/water-state-fresh)))} 
   {:text (fn[_] "And get the water as full as possible!")
    :pos (fn[_] {:x 50 :y 250})
    :start (fn[{:keys [water time-cur] :as state}]
             (assoc-in state [:water :level] 100))
    :stop (fn [{:keys [water time-cur] :as state}]
            (assoc-in state [:water] (water/water-state-fresh)))}

   {:text (fn[state]
            (html [:div "The oasis is fed by the three wells."
                   [:br]
                   "You will choose which well to get water from."
                   [:br]
                   "Pick a well by walking to it!"
                   [:br]
                   "Use the arrow keys on the keyboard: left, up, and right"
                   ]))}
   {:text (fn[state]
            (html [:div "You can only get water from wells"
                   [:br] "when they have a bucket."
                   [:br]
                   [:br] "All three buckets carry"
                   [:br] "the same amount of water."
                   ]))
    :pos (fn[state] (-> state find-close-well val position-next-to-well))
    :start (fn[state]
             (let [well-side (find-close-well state)]
               (-> state
                   (assoc-in [:avatar :destination] (-> well-side val :pos))
                   wells/wells-close
                   (assoc-in [:wells (key well-side) :open] true))))
    :stop (fn[state]
            (-> state
                (wells/wells-set-open-or-close [:left :up :right] true)
                (assoc-in [:avatar :destination] (:avatar-home settings/BOARD))))
    }

   {:text (fn[state]
            (html [:div
                   "Wells will not always have water."
                   [:br]
                   "Sometimes the well will be dry."]))
    :pos (fn[state] (-> state find-close-well val position-next-to-well))
    :start (fn[{:keys [time-cur wells] :as  state}]
             (let [side (-> state find-close-well key)]
               (-> state
                   (assoc-in [:wells side :active-at] time-cur)
                   (assoc-in [:wells side :score] false))))

    :stop (fn[state]
             (assoc-in state [:wells (-> state find-close-well key) :active-at] 0))
   }
   {:text (fn[state]
            (html [:div
                   "Othertimes, the well will be full of water"]))
    :pos (fn[state] (-> state find-close-well val position-next-to-well))
    :start (fn[{:keys [time-cur wells] :as  state}]
             (let [side (-> state find-close-well key)]
               (-> state
                   (assoc-in [:wells side :active-at] time-cur)
                   (assoc-in [:wells side :score] true))))

    :stop (fn[state]
             (assoc-in state [:wells (-> state find-close-well key) :active-at] 0))
    }
   {:text (fn[state]
            (html [:div "Don't wait too long to choose."
                   [:br]
                   "If you're too slow, all the wells will be empty!"]))
    :pos (fn[state] {:x 0 :y 100})
    :start wells/all-empty
    :stop wells/wells-turn-off
    }
   {:text (fn[state] (html [:div "This well is far away." [:br] " It'll take longer to get than the other two."]))
    :pos (fn[state] (-> state find-far-well val position-next-to-well))
    :start (fn[{:keys [time-cur wells] :as  state}]
             (let [side (-> state find-far-well key)]
               (-> state
                   (assoc-in [:wells side :active-at] time-cur)
                   (assoc-in [:wells side :score] 1))))
    :stop (fn[state]
             (-> state
                 ;; wells/wells-turn-off
                 (assoc-in [:wells (-> state find-far-well key) :active-at] 0)
                 ))}
  {
    :pos (fn[state] (-> state (get-in [:avatar :pos])
                       (update :y #(- % 150))
                       (update :x #(- % 100))))
   :start (fn[state] (-> state
                        wells/wells-close
                        (assoc-in [:phase :show-cross] true)))
   :stop (fn[state] (-> state
                       (assoc-in [:phase :show-cross] nil)
                       (wells/wells-set-open-or-close [:left :up :right] true)))
    :text (fn[state]
            (html [:div "This white cross means you have to wait. Watch the cross until it disappears"
                   [:br] "When it disappears,"
                   [:br] "you can choose the next well to visit"]))}
   {
    :pos (fn[state] (-> BOARD :bar-pos
                       (update :y #(- % 200))))
    :start (fn[state] (-> state
                        (assoc-in [:water :score] 10)
                        (assoc-in [:trial] 20)))
    :stop (fn[state] (-> state
                        (assoc-in [:water :score] 0)
                        (assoc-in [:trial] 0)))
    :text (fn[state]
            (html [:div "This bar lets you know how far along you are."
                   ;; [:br] "Blue shows how much water you've collected"
                   ;; [:br] "Green shows how many times you have gone to a well"
                   [:br] "You're done when the green bar reaches the end!"
                   ]))}
   {:text (fn[state]
            (html [:div "Each well is different, and has a different chance of having water"
                   [:br] "Over time, a well may get better or worse"]))}
   
   {:text (fn[state] "Ready? Push the right arrow to start!")}])

(defn instruction-goto
  ^{:test (fn[]
            (assert (= 0 (instruction-goto 0 :left)))
            (assert (= 0 (instruction-goto 1 :left)))
            (assert (= 1 (instruction-goto 0 :right)))
            (assert (= 1 (instruction-goto 1 :up))))
    :doc "move given index by keypress direction"}
  [i dir]
  (let [lastidx (dec (count INSTRUCTION))]
    (case dir
        :left (max 0 (dec i))
        :right (min lastidx (inc i))
        ;; :up or :down, no change
        i)))

(defn fn-or-idnt [var fnc] (if fnc (fnc var) var))
(defn update-to-from
  "run INSTRUCTION's stop and start functions on state
  if either is nil, pass along state unchanged"
  [state i-cur i-next]
  (let [stop  (get-in INSTRUCTION [i-cur :stop])
        start (get-in INSTRUCTION [i-next :start])]
    (-> state
        (fn-or-idnt stop)
        (fn-or-idnt start))))

(defn read-keys [{:keys [key phase time-cur] :as state}]
  (let [dir (case (:have key)
              37 :left
              38 :up
              39 :right
              40 :down ;; maybe disallow
              nil)
        i-cur (:idx phase)
        i-keyfn (get-in INSTRUCTION [i-cur :key dir])
        i-next (if dir (instruction-goto i-cur dir) i-cur)
        ]
    (cond
      ;; if instruction has special plans for keypushes
      (and i-keyfn 1)
      (-> state i-keyfn (assoc-in [:key :have] nil))
      ;; changing which what we should see
      (not= i-cur i-next)
      (-> state
          (assoc-in [:phase :idx] i-next)
          (assoc-in [:key :have] nil)
          (update-to-from i-cur i-next))

      ;; we want to go past the end (are "ready")
      (and dir (= i-cur i-next) (= i-cur (dec (count INSTRUCTION))))
      (-> state
          ;; NB. maybe bug?
          ;; we move to "home" at start and dont want to intercept any
          ;; keys until we are there.
          ;; START TASK buy moving :phase :name to iti
          ;; TODO pull iti from somewhere?
          (phase/phase-update)
          ;; (assoc :phase (merge {:iti-dur 2000}
          ;;                       (phase/set-phase-fresh :iti (:time-cur state))))
          ;; save both the time since animation (relative to other onsets)
          ;; and the actual time (according to the browser) to the struct
          ;; that will be sent away by phases/phone-home
          (assoc-in [:record :start-time]
                    {:animation time-cur
                     :browser (utils/now)})
          ;; wells normally turned off on :chose->:waiting flip
          ;; here we skip right over that into the first :iti so explicitly close
          (wells/wells-close)
          (assoc :key (key/key-state-fresh)))

      ;; otherwise no change
      :else
      state)))

(defn step
  "run by model/next-step from loop/time-update"
  [state time]
  (-> state
      water/water-pulse-forever
      avatar/move-avatar
      read-keys))
