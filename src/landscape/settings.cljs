(ns landscape.settings)

(def BOARD "bounds and properties of the background board"
  {:center-x 250
   :bottom-y 400
   :avatar-home {:x 250 :y 400}
   :step-sizes [70 0] ; 20211005 70 0; originally 70 150. see nofar/yesfar url tweaks
   :top-scale .66 ; perspective makes up seem farther away. scale move and disntace by this much
   :bar-pos {:x 50 :y 500} ; where to position the progress bar
   :wait-time 500 ; TODO: force movement at this speed? currently used only by well animation
   ;; 20220314 - 4th devalue block gets its own probalbities
   ;; initiall either 75/75/75 or 100/100/80 (see url_tweaks)
   :prob {:low 20 :mid 50 :high 100 :devalue-good {:good 75 :other 75}}
   :nTrials
     {:pairsInBlock 12 ; see left-right pair 24 times in a block. likely 2 blocks
      :devalue 12 ; pairs when lower prob wells are updated to 100%. after first 2. orginal 10 reps (30 total)
      :devalue-good 12 ; 20220314=10 (init) devalue good well
      }
   })

; set by anchor of url. eg http://localhost:9500/#mountain
; see url_tweak.cljs
(def tweaks ""
   {:skip-captcha false
    :vis-type :desert       ;; alernative: mountain
    :show-instructions true ;; NB. not working when false (20220314)
    :use-photodiode? false  ;; for sEEG (maybe, not tested on hardware)
    :enforce-timeout true 
    :debug false
    :step-wise false        ;; make far well additional clicks
    :post-back-url false    ;; mturk (or maybe prolific) past back url
    :reversal-block true    ;; what is usually the 2nd block. reversal
    :where :online          ;; :mri :eeg :seeg
})

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
