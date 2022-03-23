(ns landscape.key
  (:require [landscape.settings :refer [KEYCODES]]))
(defn keypress-init [] {:key nil
                        :first nil
                        :up nil
                        :callback-up nil
                        :callback-first nil
                        :callback-hold nil
                        :reset #'keypress-init
                        :count 0
                        :max-wait 0
                        :waiting []})

(def KEYPRESSTIME (atom (keypress-init)))
(defn run-if [fnc & rest] (when fnc (apply fnc rest)))
(defn keypress-callback [keystate cbname key] (run-if (cbname keystate) key))
(defn keypress-up! [key time]
  ;(println "key up" key @KEYPRESSTIME)
  ; if callback is specified, send key to function
  (swap! KEYPRESSTIME assoc :up time)
  (keypress-callback @KEYPRESSTIME :callback-up key)
  ;(println "reset?" (:reset @KEYPRESSTIME))
  (reset! KEYPRESSTIME ((:reset @KEYPRESSTIME)))
  ;(println "reset")
)

(defn waiting-key? [key] (some #(= key %) (:waiting @KEYPRESSTIME)))

(defn keypress-key! [key time]
  (when (waiting-key? key)
    (swap! KEYPRESSTIME assoc :first time :key key :count 1)
    (keypress-callback @KEYPRESSTIME :callback-first key)))

(defn keypress-down! [key time]
  (let [prev (:key @KEYPRESSTIME)
        count (:count @KEYPRESSTIME)
        new? (or (not prev) (not= prev key)) ]
    ; missed a keyup (lost focus)
    (when (and prev new?)
      (keypress-up! prev time))
    ; hit a key we wanted
    (when (waiting-key? key)
      (if new?
          (keypress-key! key time)
          (do
              (swap! KEYPRESSTIME assoc :count (inc count))
              (keypress-callback @KEYPRESSTIME :callback-hold key))))))


(defn keynum [e] (.. e -keyCode))

(defn keypress-updown! [direction e]
  "passthrough function. partial used on listener wont get repl updates.
  so this intermidate exists and dispatches to approprate up or down"
  (let [key (.. e -keyCode)
        time (.getTime (js/Date.))]
    (case direction
      :up   (keypress-up! key time)
      :down (keypress-down! key time)
      :key  (keypress-key! key time)
      nil)))


;; want to have buttons that simulate keypress
;; will be useful for wells too
;; https://stackoverflow.com/questions/596481/is-it-possible-to-simulate-key-press-events-programmatically
;; document.addEventListener("keydown", x=>console.log(x))
;; document.dispatchEvent(new KeyboardEvent('keydown', {keyCode: 38}));
(defn sim-key
  "simulate keypress using keyCode"
  [keysym]
  (let [keycode (keysym KEYCODES)
        jskey (clj->js {:keyCode keycode})
        el js/document
        event-down (new js/KeyboardEvent "keydown" jskey)
        ;; event-press (new js/KeyboardEvent "keypress" jskey)
        ;; event-up (new js/KeyboardEvent "keyup" jskey)
        ]
    (do (.dispatchEvent el event-down))
    ;; (.dispatchEvent el event-press)
    ;; (.dispatchEvent el event-up)
    ))

(defn key-state-fresh
  "clean slate for keys.
  20211007 - currently not using util, want, or next"
  []
  {:until nil :want [] :next nil :have nil :time nil :touch nil})

;; 20211007 - just assoc w/key-state-fresh
;; (defn remove-key
;;   "remove any keypress from state"
;;   [{:key [key] :as state}]
;;   (-> state
;;       (assoc-in state [:key :have] nil)
;;       (assoc-in state [:key :time] nil)))
