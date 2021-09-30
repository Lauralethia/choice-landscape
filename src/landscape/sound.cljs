(ns landscape.sound
(:require 
   [cljs-bach.synthesis :as syn]))

;; audio
(defonce audio-context (syn/audio-context))
(def SOUNDS {:reward [{:url "audio/cash.mp3"    :dur .5}
                      ;{:url "audio/trumpet.mp3" :dur .5}
                     ]
             :empty  [{:url "audio/buzzer.mp3"  :dur .5}
                      ;{:url "audio/alarm.mp3"   :dur .5}
                      ]})

; NB. could use e.g. (syn/square 440) for beeeeep
(defn play-audio
  [{:keys [url dur]} gain]
  (syn/run-with
   (syn/connect->
    (syn/sample url)
    (syn/gain gain) ; 0 for reploading
    syn/destination)
   audio-context
   (syn/current-time audio-context)
   dur))

(defn preload-sounds
  "load all the sounds, playing at 0 volume"
  []
    (for [sound (mapcat #(% SOUNDS) (keys SOUNDS))]
      (do (println sound)
      (play-audio sound 0))))


(defn play-sound
  [rew-key]
  "play sound for a given rew-key (:reward or :empty)"
  (let [snd (first (shuffle (rew-key SOUNDS)))]
  (play-audio snd 1)))
