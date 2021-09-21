(ns landscape.model)

(defn next-step [state time]
  (println "model update at" time)
  state)

(defn state-fresh
  "initial state. empty timing"
  []
  {
   :running? true
   :time-cur 0
   :start-time 0
   :time-flip 0})

(def STATE (atom (state-fresh)))
