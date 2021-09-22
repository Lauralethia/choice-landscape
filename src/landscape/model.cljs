(ns landscape.model)

(defn next-step
  "TODO: heavy lifting. update state with next step
  e.g. trigger feedback. move avatar. stop animations"
  [state time]
state)

(defn state-fresh
  "initial state. empty timing"
  []
  {
   :running? true
   :start-time 0
   :time-cur 0
   :time-flip 0
   :wells {:left  {:step 1 :open true :active-at 0}
           :up    {:step 1 :open true :active-at 0}
           :right {:step 1 :open true :active-at 0}}
   :avatar {:pos {:x 245 :y 260}
            :active-at 0 :direction :down}
   })

(def STATE (atom (state-fresh)))
