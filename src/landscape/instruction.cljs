(ns landscape.instruction
  (:require
   [landscape.sprite :as sprite]
   [landscape.key :as key]
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
(defn next-sprite [cur]
        (let [names (keys sprite/avatars)
              ntot  (count names)
              cur-i (or (.indexOf names cur) -1)
              next-i (mod (dec cur-i) ntot)]
          (nth names next-i)))

(def INSTRUCTION
  [
   {:text (fn[state]
            (html
             [:div [:h1  "Welcome to our game!"]
              [:br]
              "Push the right arrow key to get to the next instruction. "
              "You can also click the \">\" button below"
              ]))
    :start identity
    :stop identity
    :key nil}
   {:text (fn[state]
            (html
             [:div  "Before we start, pick a character!"
              [:br]
              "In the game, all characters are equal"
              [:ul
               [:li "Use the up arrow to change your selection."]
               [:li "Use the right arrow to continue when done choosing"]]
              [:div#pick-avatars
               (map (partial avatar-example state) (keys sprite/avatars))]]))
    :start identity
    :stop identity
    :key {:up (fn[state] (update state :sprite-picked next-sprite))}}
   {:text (fn[_] "You want fill this oasis with water as fast as you can")
    :pos (fn[_] {:x 50 :y 250})}
    ;; :start (fn[{:keys [water time-cur] :as state}]
    ;;          (assoc-in state [:water :active-at] time-cur))

   {:text (fn[state] "This well is far away. but it's always good")
    :pos (fn[state] (-> state find-far-well val :pos
                       (update :x #(+ (:width sprite/well) 5 %))))
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
   {:text (fn[state] "the closer wells wont always have water")
    :start (fn[state] state)}
   {:text (fn[state] "Ready?")}
   ])

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

(defn read-keys [{:keys [key phase] :as state}]
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
          (assoc :phase (phase/set-phase-fresh :chose nil))
          (assoc :key (key/key-state-fresh)))

      ;; otherwise no change
      :else
      state)))

(defn step
  "run by model/next-step from loop/time-update"
  [state time]
  (-> state
      read-keys))
