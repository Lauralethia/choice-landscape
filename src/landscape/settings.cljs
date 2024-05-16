(ns landscape.settings)

(def arrow-keycodes
  "keycodes for arrow keys. default responses.
  but also used for instructions even if using MRI glove"
  {:left  37
   :up    38
   :right 39
   :down  40})

(def RT-EXPECTED
  "average reaction time (ms) measured from mtruk w/o MR timing.
used by MR to adjust ITI dur"
  580)

(def SAMPLERATE "how often to animate a new frame (ms)" 30)

(def WALKTIME
  "ms to walk to choice before given feedback. not seen if timeout.
   should calculate this based on distance and animation ticks
  used in current-settings, but reset based on avatar and wells by url-tweaks/update-walktime

  hardcoded value for MR timing. needed to calculate ideal iti onset

  intially 70px total/10px avatar * 30ms sampling (210ms).
  ocean is 140/15*30  (280ms)
  but time-diff caled at 534 (walk there and back) set to 210 for original

  20221026 after 150 trials, off by about a minute.
           only 15s of that can be attributed to 420 vs 520
           but might as well match what afni's 3dDeconvolve used to model
  "
  ;; (* 2 210)
  (+ 270 250)
  )

(def ITIDUR 1000)
(def MR-FIRST-ITI "enough time to get a stable hrf" 3000)
(def MR-END-ITI "enough time to get a stable hrf" 6000)
(def mri-glove-keys
  "button glove at MR uses 1-thumb 2-index .... 5-pinky
  key to num: ! is 16 where as 1 is 49"
  {:left   50  ; index
   :up     51  ; middle
   :right  52  ; ring
   :down   49  ; thumb
   :trigger 187  ; '=' is 187; '6' is 54. cant get '^'?
})

(def BOARD "bounds and properties of the background board"
  {:center-x 250
   :bottom-y 400
   :avatar-home {:x 250 :y 400}
   :avatar-step-size 10 ; 20220428 - add here instead of hardcoded in avatar/move-avatar
   :step-sizes [70 0] ; 20211005 70 0; originally 70 150. see nofar/yesfar url tweaks
   :top-scale .66 ; perspective makes up seem farther away. scale move and disntace by this much
   :bar-pos {:x 50 :y 500}        ; where to position the progress bar
   :wait-time 500 ; TODO: force movement at this speed? currently used only by well animation
   ;; 20220519 - if not random, should be in fixed_timing.cljs
   :timing-method :random ;; :debug :mrA1 :mrB2
   ;; 20220314 - 4th devalue block gets its own probalbities
   ;; initiall either 75/75/75 or 100/100/80 (see url_tweaks)
   :prob {:low 20 :mid 50 :high 100 :devalue-good {:good 75 :other 75}}
   :nTrials
   {:pairsInBlock 12 ; see left-right pair 24 times in a block. likely 2 blocks
    :devalue 12 ; pairs when lower prob wells are updated to 100%. after first 2. orginal 10 reps (30 total)
    :devalue-good 12            ; 20220314=10 (init) devalue good well
    }

   :keycodes arrow-keycodes
   ;; 20220527 - location to send ttl for placing event info in recorded data
   ;; NB. timing will probably be terrible
   :local-ttl-server nil

   ;; 20230110
   ;; show flash white every phase or hold specified color per phase type
   :pd-type :whiteflash  ; vs :phasecolor
   ;; 20220606 where coins/water will accumulate
   :pile-y nil
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
    ;; 20240515 - options are 'en' or 'es'. for instructions and and of task summary
    :lang :en
    :step-wise false        ;; make far well additional clicks
    :post-back-url false    ;; mturk (or maybe prolific) past back url
    :reversal-block true    ;; what is usually the 2nd block. reversal
    :where :online          ;; :mri :eeg :seeg :practice
    :iti+end   0            ;; probably want e.g. 6000 for MRI
})


(def TIMES "settings for time constraints"
  {
   :choice-timeout 2000 ; ms allowed to make a choice
   ;; prev 1000 (1sec). should match walktime for consitant timing
   :walk-dur       WALKTIME ; ms how long to show timeout
   :timeout-dur    WALKTIME ; ms how long to show timeout
   :iti-dur        ITIDUR ; ms - show white cross. used by gen-wells. MR will be different
})

(def current-settings (atom (merge BOARD tweaks {:times TIMES})))
