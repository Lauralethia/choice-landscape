(ns landscape.model)

(defn next-step [state time]
  (println "model update at" time)
  state)

(defn state-fresh
  "initial state. empty timing"
  []
  {
   :running? true
   :start-time 0
   :time-cur 0
   :time-flip 0
   :avatar {:pos {:x 100 :y 100}
            :active-at 0 :direction :left}
   })

(def STATE (atom (state-fresh)))
