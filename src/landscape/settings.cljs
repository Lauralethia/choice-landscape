(ns landscape.settings)

(def BOARD "bounds and properties of the background board"
  {:center-x 250
   :bottom-y 400
   :avatar-home {:x 250 :y 400}
   :step-sizes [70 0] ; currently only first 2 are used 20211005. originally 70 150
   :top-scale .66 ; perspective makes up seem farther away. scale move and disntace by this much
   :bar-pos {:x 50 :y 500} ; where to position the progress bar
   :wait-time 500 ; TODO: force movement at this speed? currently used only by well animation
   :prob {:low 20 :mid 50 :high 100}
   :nTrials
     {:pairsInBlock 24 ; see left-right pair 24 times in a block. likely 2 blocks
      :devalue 10 ; pairs when lower prob wells are updated to 100%. last block
      }
   })
(def tweaks ""
   {:skip-captcha false
    :vis-type :desert
    :show-instructions true
    :use-photodiode? false
    :enforce-timeout true
    :debug false
})

; desert or mountain
; set by anchor of url eg http://localhost:9500/#mountain
; see core.cljs
(def current-settings (atom (merge BOARD tweaks)))

; not used everywhere!
; TODO: replace all hard coded numbers
(def KEYCODES {:left  37
              :up     38
              :right  39
              :down   40})

(def ITIDUR 1000)

(def TIMES "settings for time constraints"
  {
   :choice-timeout 2000 ; ms allowed to make a choice
   :timeout-dur    1000 ; ms how long to show timeout
})
