(ns landscape.loop
  (:require
   [landscape.view :as view]
   [landscape.model :as model]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]])
   (:require-macros  [cljs.core.async.macros :refer [go-loop go]]))

(defn renderer
  "used by watcher. triggered by animation step's change to time"
  [full-state]
  (view/change-dom (view/display-state full-state)))

(defn world
  "function for anything used to wrap state.
   currelty doesn't do anything. could phone home, etc
   changes to state here do not get saved/updated with (reset!)"
  [state]
  (-> state))

(defn time-update
  "what to do at each timestep of AnimationFrame (run-loop).
   first call will be on unintialized time values"
  [time {:keys [start-time time-flip] :as state}]
  (-> state
      (assoc :time-cur time
             :time-delta (- time start-time)
             :time-since (- time time-flip))
      (model/next-step time)))

(defn run-loop [state-atom time]
  "recursive loop to keep the task running.
   only changes time, which is picked up by watcher that runs render"
  (let [new-state (swap! state-atom (partial time-update time))]
  (when (:running? new-state)
    (go (<! (timeout 30))
            (.requestAnimationFrame js/window (partial run-loop state-atom))))))

(defn run-start
  "kick of run-loop. will stop if not :running? in run-loop"
  [state-atom]
  (.requestAnimationFrame
   js/window
   (fn [time]
     ;(if (= (-> @state-atom :time-cur) 0)
     ;  (reset! state-atom (model/state-fresh @state-atom time)))
     (run-loop state-atom time))))
