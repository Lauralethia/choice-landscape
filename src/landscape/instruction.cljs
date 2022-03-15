(ns landscape.instruction
  (:require
   [landscape.sprite :as sprite]
   [landscape.key :as key]
   [landscape.utils :as utils]
   [landscape.settings :as settings :refer [current-settings]]
   [landscape.key :refer [sim-key]]
   [landscape.model.records :as records]
   [landscape.model.water :as water]
   [landscape.model.wells :as wells]
   [landscape.model.phase :as phase]
   [landscape.model.avatar :as avatar]
   [landscape.model.phase :as phase]
   [landscape.sound :refer [play-sound]]
   [clojure.string]
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
  (if (> (-> well :pos :x) 300)
    (-> well :pos (update :y #(+ (:height sprite/well) 5 %))
        (update :x #(- % 200)))
    (-> well :pos (update :x #(+ (:width sprite/well) 5 %)))))

(def items {
            :desert   {:pond "pond" :water "water" :well "well" :fed "fed"    :bucket "bucket"}
            :mountain {:pond "pile" :water "gold"  :well "mine" :fed "filled" :bucket "axe"}})
(defn item-name [item]
  "use current-settings state to determine what words to use"
  (let [vis (or (get @settings/current-settings :vis-type) "desert")]
    (get-in items [(keyword vis) (keyword item)])))


(declare INSTRUCTION)
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
        ;; if we should skip move one more in the current direction
        ;; before getting i-next's stat function
        skip (if-let [skipfn (get-in INSTRUCTION [i-next :skip])]
                     (skipfn state)
                     false)
        dir (if (> 0 (- i-cur i-next)) 1 -1)
        i-next (if skip (+ i-next dir) i-next)
        start (get-in INSTRUCTION [i-next :start])
        ]
    (-> state
        (fn-or-idnt stop)
        (fn-or-idnt start)
        (assoc-in [:phase :idx] i-next))))

(defn pass-captcha
 ([state]
  (pass-captcha state
                (if-let [target (. js/document (querySelector "#captcha"))]
                  (.-value target)
                  "FAIL")))
  ([state pass]
  (or (-> state :record :settings :skip-captcha)
      (= "cat" pass))))


(def INSTRUCTION
  [
   {:text (fn[state]
            (html
             [:div [:h1  "Welcome to our game!"]
              [:br]
              "Push the keyboard's " [:b "right arrow key"] " to get to the next instruction. "
              [:br] [:br]
              "You can also click the \"->\" button below"
              ]))
    :start identity
    ;; NB. fullscreen in firefox removes background and centering
    ;;  on either .-body "main-container"
    :stop (fn[state] ;; (try (-> js/document
                    ;;          (.getElementById "main-container")
                    ;;          .requestFullscreen))
            state)
    :key nil}
   {:text (fn [state]
            (html
             [:div [:h2 "This game uses sound!"]
              [:br] "Type the word you hear in the box below."
              [:br]
              [:input
               {:id "captcha" :type :input :size 10
                :on-change (fn[e]
                             (let [word (-> e .-target .-value)]
                               (if (pass-captcha state word) (sim-key :right))))
                }]
              [:br] [:br]
              [:input
               {:type :button :value "Play Sound Again"
                :on-click (fn [e] (play-sound :word))}]]))
    ;; when we start out, we can skip this if we dont need to pass-captcha
    ;; ie settings/:skip-catcha == true
    :start (fn[state] (do (play-sound :word) state))

    ;; NB. presented second b/c if first,
    ;; going back another goes to min. which is this. endless loop
    :skip pass-captcha
    :stop identity
    ;; only advance if captcha is passed
    ;; will likely be here after sim-key from on-change textbox
    :key {:right (fn[state]
                   (if (pass-captcha state)
                     (-> state
                         ;; dont have to do captcha again
                         (assoc-in [:record :settings :skip-captcha] true)
                         ;; go to next instruction
                         (update-to-from 1 2))
                     state))}
   }
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
   {:text (fn[_] (str "You want to fill this " (item-name :pond) " with " (item-name :water) " as fast as you can"))
    :pos (fn[_] {:x 50 :y 250})
    :start (fn[{:keys [water time-cur] :as state}]
             (assoc-in state [:water :active-at] time-cur))
    :stop (fn [{:keys [water time-cur] :as state}]
            (assoc-in state [:water] (water/water-state-fresh)))}
   {:text (fn[_] (str "And get as much " (item-name :water) " as possible!"))
    :pos (fn[_] {:x 50 :y 250})
    :start (fn[{:keys [water time-cur] :as state}]
             (assoc-in state [:water :level] 100))
    :stop (fn [{:keys [water time-cur] :as state}]
            (assoc-in state [:water] (water/water-state-fresh)))}

   {:text (fn[state]
            (html [:div "The " (item-name :pond) " is " (item-name :fed) " by the three " (item-name :well) "s."
                   [:br]
                   "You will choose which " (item-name :well) " to get " (item-name :water) " from."
                   [:br]
                   "Pick a " (item-name :well) " by walking to it!"
                   [:br]
                   "Use the arrow keys on the keyboard: left, up, and right"
                   ]))}
   {:text (fn[state]
            (html [:div "You can only get " (item-name :water) " from " (item-name :well)  "s"
                   [:br] "when they have a " (item-name :bucket) "."
                   [:br]
                   [:br] "All three " (item-name :bucket) "s carry"
                   [:br] "the same amount of " (item-name :water) "."
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
                (assoc-in [:avatar :destination] (:avatar-home @current-settings))))
    }

   {:text (fn[state]
            (html [:div
                   (clojure.string/capitalize (item-name :well)) "s will not always have " (item-name :water) "."
                   [:br]
                   "Sometimes the " (item-name :well) " will be dry."]))
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
                   "Othertimes, the "(item-name :well)" will be full of " (item-name :water)]))
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
                   "If you're too slow, all the " (item-name :well)"s  will be empty!"]))
    :pos (fn[state] {:x 0 :y 100})
    :start wells/all-empty
    :stop wells/wells-turn-off
    }
   {:text (fn[state] (html [:div "This " (item-name :well) " is far away." [:br] " It'll take longer to get than the other two."]))
    :pos (fn[state] (-> state find-far-well val position-next-to-well))

    ;; skip instruction if far is same as close.
    ;; causes recursion if done in start:(update-to-from state i-cur (inc i-cur))
    :skip (fn[state]
            (-> @settings/current-settings (get-in [:step-sizes 1]) (= 0)))
    :start (fn[{:keys [time-cur wells] :as  state}]
             (let [side (-> state find-far-well key)]
               ;; otheriwse show this note about far taking longer
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
                   [:br] "you can choose the next " (item-name :well)" to visit"]))}
   {
    :pos (fn[state] (-> @current-settings :bar-pos
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
            (html [:div "Each "(item-name :well)" is different, and has a different chance of having "(item-name :water)
                   [:br] "Over time, a "(item-name :well)" may get better or worse"]))}
   {:text (fn[state] [:div  {:style {:text-align "left"}}
      "Ready? Push the right arrow to start!"
      [:ul
       [:li "Fill the " (item-name :pond)
        " by visiting " (item-name :well) "s that give " (item-name :water)
        ". Try to avoid empty " (item-name :well) "s."]
       [:li "Some " (item-name :well) "s give " (item-name :water) " more often than others."]
       [:li "How often a " (item-name :well) " has " (item-name :water) " might change."]
       [:li "The amount of " (item-name :water)
        " when there is " (item-name :water)
        " is the same for all " (item-name :well) "s."]
       [:li "Respond faster to finish sooner."]
       (when (-> @settings/current-settings (get-in [:step-sizes 1]) (> 0))
         [:li "The far " (item-name :well) " takes more time to use. You will finish slower when using it."])
       [:li "How often you visit a " (item-name :well)
        " does not change how often it gives " (item-name :water) ]]])}])

(defn instruction-finished [state time-cur]
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
      (assoc-in [:record :start-time] (records/make-start-time time-cur))
      ;; wells normally turned off on :chose->:waiting flip
      ;; here we skip right over that into the first :iti so explicitly close
      (wells/wells-close)
      (assoc :key (key/key-state-fresh))))

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
          (assoc-in [:key :have] nil)
          (update-to-from i-cur i-next))

      ;; we want to go past the end (are "ready")
      (and dir (= i-cur i-next) (= i-cur (dec (count INSTRUCTION))))
      (instruction-finished state time-cur)

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
