(ns landscape.settings)

(def BOARD "bounds and properties of the background board"
  {:center-x 250
   :bottom-y 260
   :avatar-home {:x 250 :y 250}
   :step-sizes [100 50 25 25 25 25]
   :wait-time 500 ; TODO: force movement at this speed? currently used only by well animation
   })
