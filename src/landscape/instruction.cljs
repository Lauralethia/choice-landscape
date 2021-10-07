(ns landscape.instruction
  (:require
   [landscape.sprite :as sprite]
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]))

(defn find-far-well [{:keys [wells] :as state}]
  (apply max-key :steps wells))

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
(def INSTRUCTION
  [
   {:text "Welcome to our game!"
    :pos #({:x 100 :y 100})
    :start identity
    :stop identity
    :key nil}
   {:text "This well is far away. but it's always good" 
    :pos (fn[state] (-> state find-far-well first val :pos))
    :start (fn[state] state)}
   {:text "the closer wells wont always have water" 
    :pos #({:x 100 :y 100})
    :start (fn[state] state)}
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
        i-next (if dir (instruction-goto i-cur dir) i-cur)
        ]
    (if (not= i-cur i-next)
      (-> state
          (assoc-in [:phase :idx] i-next)
          (assoc-in [:key :have] nil)
          (update-to-from i-cur i-next))
      state)))
  
(defn step
  "run by model/next-step from loop/time-update"
  [state time]
  (-> state
      read-keys))
