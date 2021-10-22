(ns landscape.settings)

(def BOARD "bounds and properties of the background board"
  {:center-x 250
   :bottom-y 400
   :avatar-home {:x 250 :y 400}
   :step-sizes [70 150 50 25 25 25 25] ; currently only first 2 are used 20211005
   :top-scale .66 ; perspective makes up seem farther away. scale move and disntace by this much
   :bar-pos {:x 50 :y 500} ; where to position the progress bar
   :wait-time 500 ; TODO: force movement at this speed? currently used only by well animation
   })

; not used everywhere!
; TODO: replace all hard coded numbers
(def KEYCODES {:left  37
              :up     38
              :right  39
              :down   40})

(def ITIDUR 1000)
