(ns landscape.instruction
  (:require
   [landscape.sprite :as sprite]
   [landscape.key :as key]
   [landscape.utils :as utils]
   [landscape.settings :as settings :refer [current-settings]]
   [landscape.key :refer [sim-key]]
   [landscape.model.records :as records]
   [landscape.model.water :as water]
   [landscape.model.wells :as wells]
   [landscape.model.phase :as phase]
   [landscape.model.avatar :as avatar]
   [landscape.model.floater :as floater]
   [landscape.http :as http]
   [landscape.sound :refer [play-sound]]
   [clojure.string]
   ;; [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
   [sablono.core :as sab :include-macros true :refer-macros [html]]))

(defn find-far-well [{:keys [wells] :as state}]
  (apply max-key #(:step (val %)) (select-keys wells [:left :up :right])))
(defn find-close-well [{:keys [wells] :as state}]
  (apply min-key #(:step (val %)) (select-keys wells [:left :up :right])))

;; Idea is to present sequential instructions using only 3 or 4 keys
;; instruction boxes should be positioned close to the thing they explain
;;
;; we are likely to want to animate a thing. so having :start and :stop
;; functions that can manipualte state will be useful
;; To that end, instruction has it's own ~step~ function that will not include
;; the functions that update phase or react to water-well+avatar "hits"
;;
;; instruction phase should have weither or not we can proceed
;; and we'll only pass onto readkeys if that's true
;; otherwise keys fn handles it
;; if key is nil, default to okay to proceed
(defn avatar-example [{:keys [time-cur sprite-picked] :as state} avatar-name]
  (html [:div
         {:key avatar-name
          :style {:margin "auto"  :margin-bottom "10px"
                  :background (if (= sprite-picked avatar-name) "blue" "gray")}}
         (sprite/avatar-disp {:time-cur time-cur :sprite-picked avatar-name}
                             {:direction :down :active-at 1})]))
(defn next-sprite [dir cur]
        (let [names (keys sprite/avatars)
              fncng (if (= dir :down) inc dec)
              ntot  (count names)
              cur-i (or (.indexOf names cur) -1)
              next-i (mod (fncng cur-i) ntot)]
          (nth names next-i)))

(defn position-next-to-well
  "move position over by "
  [well]
  (if (> (-> well :pos :x) 300)
    (-> well :pos (update :y #(+ (:height sprite/well) 5 %))
        (update :x #(- % 200)))
    (-> well :pos (update :x #(+ (:width sprite/well) 5 %)))))

(def items {
            :desert   {:en {:pond "pond" :water "water" :well "well" :fed "fed"    :bucket "bucket" :dry "dry" :carry "carry"}
                       :es {:pond "estanque" :water "agua" :well "pozo" :fed "llena" :bucket "balde" :dry "seco" :carry "llevan"}}

            :mountain {:en {:pond "pile" :water "gold"  :well "mine" :fed "filled" :bucket "axe" :dry "empty" :carry "dig"}}
            :wellcoin {:en {:pond "pile" :water "gold"  :well "well" :fed "filled" :bucket "chest" :dry "empty" :carry "carry"}}
            :ocean    {:en {:pond "DNE" :water "gold"  :well "chest" :fed "filled" :bucket "key" :dry "empty" :carry "unlock"}}
            })
(defn item-name [item]
  "use current-settings state to determine what words to use"
  (let [vis (or (get @settings/current-settings :vis-type) "desert")
        lang (or (get @settings/current-settings :lang) "en")]
    (get-in items [(keyword vis) (keyword lang) (keyword item)])))


(declare INSTRUCTION)
(defn instruction-goto
  ^{:test (fn[]
            (assert (= 0 (instruction-goto 0 :left)))
            (assert (= 0 (instruction-goto 1 :left)))
            (assert (= 1 (instruction-goto 0 :right)))
            (assert (= 1 (instruction-goto 1 :up))))
    :doc "move given index by keypress direction"}
  [i dir]
  (let [lastidx (dec (count INSTRUCTION))]
    (case dir
        :left (max 0 (dec i))
        :right (min lastidx (inc i))
        ;; :up or :down, no change
        i)))
(defn fn-or-idnt [var fnc] (if fnc (fnc var) var))
(defn should-skip
  "look at :skip in instruction with state. used in loop to skip multiple all
  TODO: might be a problem if first or last is skip"
  [state i-next]
  (if-let [skipfn (get-in INSTRUCTION [i-next :skip])]
    (skipfn state)
    false))
(defn update-to-from
  "run INSTRUCTION's stop and start functions on state
  if either is nil, pass along state unchanged"
  [state i-cur i-next]
  (let [stop  (get-in INSTRUCTION [i-cur :stop])
        ;; if we should skip move one more in the current direction
        ;; before getting i-next's stat function
        dir (if (> 0 (- i-cur i-next)) 1 -1)
        i-next (loop [i i-next] (if (should-skip state i) (recur (+ i dir)) i)) 
        start (get-in INSTRUCTION [i-next :start])
        ]
    (-> state
        (fn-or-idnt stop)
        (fn-or-idnt start)
        (assoc-in [:phase :idx] i-next))))

(defn pass-captcha
  "without args get passphrase from #captcha.
  otherwise check if we already did it, if this is mri, or if passphrase is correct"
  ([state]
   (pass-captcha state
                 (if-let [target (. js/document (querySelector "#captcha"))]
                   (.-value target)
                   "FAIL")))
  ([state pass]
   (or (-> state :record :settings :skip-captcha)
       ;; disable on mri -- no keyboard to type out the passphrase
       (contains? #{:mri} (-> state :record :settings :where))
       (= "cat" pass))))


(defn instruction-finished [state time-cur]
  ; send start trigger
  (if-let [url (:local-ttl-server @settings/current-settings)]
    (http/send-local-ttl url 128))
  (-> state
      ;; an instruction might have set trial to 0.make sure we start at 1
      (assoc-in [:trial] 1)
      (phase/phase-update)
      ;; (assoc :phase (merge {:iti-dur 2000}
      ;;                       (phase/set-phase-fresh :iti (:time-cur state))))
      ;; save both the time since animation (relative to other onsets)
      ;; and the actual time (according to the browser) to the struct
      ;; that will be sent away by phases/phone-home
      (assoc-in [:record :start-time] (records/make-start-time time-cur))
      ;; wells normally turned off on :chose->:waiting flip
      ;; here we skip right over that into the first :iti so explicitly close
      (wells/wells-close)
      ;; might have been walking down still. jump ahead so we start as expect
      ;; only noticebly with 'noinstructions' and when testing
      (assoc-in  [:avatar :pos] (:avatar-home @current-settings))
      (assoc-in  [:avatar :destination] (:avatar-home @current-settings))
      (assoc :key (key/key-state-fresh))))

(defn buttonbox? [state]
  (contains? #{:mri :eeg :seeg}
             (get-in state [:record :settings :where])))
(defn mri? []
  "Is task running in mri? Need to wait for '=' to start if mri.
   Using settings global atom"
  (contains? #{:mri} (get @settings/current-settings :where)))
(defn online? []
  "Is task running online? Used to give stronger wording about responding"
  (not(contains? #{:mri :eeg :practice :seeg}
                 (get @settings/current-settings :where))))
(defn ocean? []
  "Ocean vis-type is special. No pond/treasure to fill."
  (contains? #{:ocean} (get @settings/current-settings :vis-type)))

(defn using-far-well? []
  "Different instructions when we have one well that takes longer to get to than others."
  (-> @settings/current-settings (get-in [:step-sizes 1]) (> 0)))

(defn translations []
  "English and Spanish versions of instruction and 'good job' finish text (used by view.cljs).
  As a function returning giant dict each time b/c (item-name) must be run after @current-settings is updated."
  {:welcome {:en "Welcome to our game!"
             :es "¡Bienvenidos al juego!"}
   :button {:en [:div
                 "You will use buttons to push "
                 [:b {:class "indexfinger"} "left"] ", "
                 [:b {:class "middlefinger"} "up"] ", and "
                 [:b {:class "ringfinger"} "right."] [:br] [:br]
                 "Use your " [:b {:class "ringfinger"} "ring finger" ]
                 " to get the next instruction."
                 ]
            :es [:div
                 "Usarás botones para presionar a la " [:br]
                 [:b {:class "indexfinger"} "izquierda"] ", "
                 [:b {:class "middlefinger"} "arriba"] ", y a la "
                 [:b {:class "ringfinger"} "derecha."] [:br] [:br]
                 "Usa tu  " [:b {:class "ringfinger"} "dedo anular" ]
                 " para recibir la próxima instrucción."
                 ]}
   :avatar-1 {:en [:div "Before we start, pick a character!"
                   [:br] "In the game, all characters are equal"]
              :es [:div "Antes de empezar, ¡elegí un personaje!"
                   [:br] "En el juego, todos los personajes son iguales."]}
   :Use {:en "Use" :es "Usá"}
   :button-up {:en [:span  "your " [:b {:class "middlefinger"} "middle finger"]]
               :es [:span  "tu "   [:b {:class "middlefinger"} "dedo medio"]]}
   :change-sel {:en [:span " to " [:u "change"] " your selection."]
                :es [:span " para " [:u "cambiar"] " tu selección."]}

   :choose-right {:en [:span  "your " [:b {:class "ringfinger"} "ring finger"]]
                  :es [:span  "tu " [:b {:class "ringfinger"} "dedo anular"]]}
   :avatar-continue {:en " to choose and continue."
                     :es " para elegir y continuar."}
   :fill-pond {:en (str "You want to fill this " (item-name :pond) " with " (item-name :water) " as fast as you can.")
               :es (str "Tu objetivo es llenar este " (item-name :pond) " con " (item-name :water) " lo más rápido que puedas.")}
   :much-water {:en (str "And get as much " (item-name :water) " as possible!")
                :es (str "¡Y obtener tanta " (item-name :water) " como sea posible!")}
   :fill-goal {:en (str "The " (item-name :pond) " is " (item-name :fed) " by the three " (item-name :well) "s.")
               :es (str "El " (item-name :pond) " se " (item-name :fed) " con " (item-name :water) " de los tres " (item-name :well) "s.")}
   :choose-walk {:en [:span
                      "You will choose which " (item-name :well) " to get " (item-name :water) " from."
                      [:br]
                      "Pick a " (item-name :well) " by walking to it!"]
                 :es [:span
                      "Vas a elegir de cuál " (item-name :well) " sacar " (item-name :water) "."
                      [:br] "Elegí un " (item-name :well) " caminando hacia él."]}
   :button-box {:en "Use the button box to choose." :es "Usá las flechas para elegir."}
   :quick-random-ocean {:en [:div "You want to get as much " (item-name :water) " as possible as quickly as you can!" [:br]  "Each " (item-name :well) " may or may not have " (item-name :water) " inside." ]
                        :es [:div "TODO"] }
   :single-tap {:en [:span "Make choices with a single tap." [:br]
                     [:b "Do not hold keys down."] [:br] [:br]]
                :es [:span "Hacé elecciones con un solo toque." [:br]
                     [:b "No mantengas las teclas presionadas."] [:br] [:br]]}
   :only-bucket {:en [:div "You can only get " (item-name :water) " from " (item-name :well)  "s"
                      [:br] "when they have a " (item-name :bucket) "."
                      [:br]
                      [:br] "All three " (item-name :bucket) "s " (item-name :carry)
                      [:br] "the same amount of " (item-name :water) "."
                      ]
                 :es [:div "Solo podés sacar " (item-name :water) " de los " (item-name :well)  "s"
                      [:br] "cuando tienen un " (item-name :bucket) "."
                      [:br]
                      [:br] "Los tres " (item-name :bucket) "s " (item-name :carry)
                      [:br] "la misma cantidad de " (item-name :water) "."
                      ]}
   :empty-well {:en [:div
                     "These "(item-name :well) "s will not always have " (item-name :water) "."
                     [:br]
                     "Sometimes the " (item-name :well) " will be " (item-name :dry) "."]
                :es [:div
                     "Los " (item-name :well) "s no siempre tendrán " (item-name :water) "."
                     [:br]
                     "...A veces, el "(item-name :well) " estará " (item-name :dry) "."]}
   :full-well {:en [:div
                    "Othertimes, the "(item-name :well)" will be full of " (item-name :water)]
               :es [:div
                    "Otras veces, el "(item-name :well)"  estará lleno de " (item-name :water)]}
   :must-respond {:en [:div "Don't wait too long to choose."
                       [:br]
                       "If you're too slow, all the " (item-name :well)"s  will be empty!" [:br]
                       ;; dont be harsh about payment when participant is in person
                       ;; (instead RA/RS can give a nice "wake up" reminder
                       (when (online?) [:b "You will not get paid if you do not respond!"])
                       ]
                  :es [:div "No esperes demasiado para elegir."
                       [:br]
                       "Si vas muy lento, todos los " (item-name :well)"s los pozos estarán vacíos" [:br]
                       ;; dont be harsh about payment when participant is in person
                       ;; (instead RA/RS can give a nice "wake up" reminder
                       (when (online?) [:b "You will not get paid if you do not respond!"])
                       ]}
   :white-cross {:en [:div "This white cross means you have to wait." [:br]
                      "Watch the cross until it disappears" [:br]
                      "When it disappears," [:br]
                      "You can choose the next " (item-name :well)" to visit."]
                 :es [:div "Esta cruz blanca significa que tenés que esperar. " [:br]
                      "Observá la cruz hasta que desaparezca." [:br]
                      "Cuando desaparezca," [:br]
                      "podés elegir el próximo " (item-name :well)" para visitar."]}
   :progress-bar {:en [:div "this bar lets you know how far along you are."
                       ;; [:br] "blue shows how much water you've collected"
                       ;; [:br] "green shows how many times you have gone to a well"
                       [:br] "You're done when the green bar reaches the end!"
                       ]
                  :es [:div "Esta barra te indica cuánto has avanzado." [:br]
                       "¡Terminarás cuando la barra verde llegue al final!"]}
   :different-wells {:en [:div "Each "(item-name :well)" is different, and has a different chance of having "(item-name :water) "."
                          [:br] "Over time, a "(item-name :well)" may get better or worse"]
                     :es [:div
                          "Cada "(item-name :well)" es diferente y tiene una probabilidad diferente de tener "(item-name :water) "." [:br]
                          "Con el tiempo, un "(item-name :well)" puede mejorar o empeorar."]}
   :ready {:en [:div  {:style {:text-align "left"}}
                "Ready? "
                (if (not (mri?))
                  " Push the right arrow to start!"
                  [:p " Waiting for scanner."
                   [:span {:style {:font-size "8px" :padding-left "20px"}}
                    "= "
                    [:a {:href "#" :on-click
                         (fn[_] (-> js/window .-location .reload))} "refresh page"]]])
                [:ul
                 (when (not (ocean?))
                   [:li "Fill the " (item-name :pond)
                    " by visiting " (item-name :well) "s that give "
                    (item-name :water)
                    ". Try to avoid empty " (item-name :well) "s."])
                 [:li "Some " (item-name :well) "s give " (item-name :water) " more often than others."]
                 [:li "How often a " (item-name :well) " has " (item-name :water) " might change."]
                 [:li "The amount of " (item-name :water)
                  " when there is " (item-name :water)
                  " is the same for all " (item-name :well) "s."]

                 (when (online?) [:li  [:b "You must respond to be paid"]])
                 [:li "Respond faster to finish sooner."]
                 (when (using-far-well?)
                   [:li "The far " (item-name :well) " takes more time to use. You will finish slower when using it."])
                 [:li "How often you visit a " (item-name :well)
                  " does not change how often it gives " (item-name :water) ]
                 [:li "Make choices with a single tap. Do not hold keys down."]]
                [:input
                 {:type :button :value "Test Sound"
                  :on-click (fn [e] (play-sound :reward))}]
                ]
           :es [:div  {:style {:text-align "left"}}
                "Listo? "
                (if (not (mri?))
                  " ¡Presioná la flecha derecha para comenzar!"
                  [:p " Waiting for scanner."
                   [:span {:style {:font-size "8px" :padding-left "20px"}}
                    "= "
                    [:a {:href "#" :on-click
                         (fn[_] (-> js/window .-location .reload))} "refresh page"]]])
                [:ul
                 (when (not (ocean?))
                   [:li "Llená el " (item-name :pond)
                    " visitando " (item-name :well) "s que den "
                    (item-name :water)
                    ". Tratá de evitar los " (item-name :well) "s vacíos."])
                 [:li " Algunos " (item-name :well) "s dan " (item-name :water) " con más frecuencia que otros."]
                 [:li "La frecuencia con la que un " (item-name :well) " tiene " (item-name :water) " puede cambiar."]
                 [:li "La cantidad de " (item-name :water)
                  " cuando hay " (item-name :water)
                  " es la misma para todos los " (item-name :well) "s."]

                 (when (online?) [:li  [:b "You must respond to be paid"]])
                 [:li "Respondé más rápido para terminar más pronto."]
                 (when (using-far-well?)
                   [:li "The far " (item-name :well) " takes more time to use. You will finish slower when using it."])
                 [:li "Con qué frecuencia visites un " (item-name :well)
                  " no cambia la frecuencia con la que da " (item-name :water) ]
                 [:li "Hacé elecciones con un solo toque. No mantengas las teclas presionadas."]]
                [:input
                 {:type :button :value "Testeo de sonido"
                  :on-click (fn [e] (play-sound :reward))}]
                ]}

   ;; NB. these are used by view.cljs for end of task
   ;;     (this function could be moved out of instructions to reflect it has
   ;;      text for both instructions and finish)
   :great-job {:en [:span [:h1 "Great Job!"] ;[:h3 "You filled the pond!"]; TODO pond might be mine
                    [:br] "Thank you for contributing to our research!!"]
               :es [:span [:h1 "¡Excelente trabajo!"] ;[:h3 "You filled the pond!"]; TODO pond might be mine
                    [:br] "¡Gracias por contribuir a nuestra investigación!"]}
   :download {:en "Download task data." :es "Descargar datos de la tarea."}
   :close-window {:en "Close window." :es "Cerrar ventana"}})

(defn lang [] "get language setting"
  (or (get @settings/current-settings :lang) "en"))
(defn text-for [key] "get text of key using state setting's language"
  (get-in (translations) [key (lang)] (str "NO TRANSLATION for " key " in " (lang) )))

(def INSTRUCTION
  [
   {:text (fn[state]
            (html
             [:div [:h1  (text-for :welcome)]
              [:br]

              (if (buttonbox? state) 
                [:div
                 [:img {:src "imgs/fingers.png"}]
                 [:br]
                 (text-for :button)]
                ;; TODO: translate non-button box keyboard instructions
                [:div 
                 "Push the keyboard's " [:b "right arrow key"]
                 " to get to the next instruction. "
                 [:br] [:br]
                 "You can also click the \"->\" button below"])]))
    :start identity
    ;; NB. fullscreen in firefox removes background and centering
    ;;  on either .-body "main-container"
    :stop (fn[state] ;; (try (-> js/document
            ;;          (.getElementById "main-container")
            ;;          .requestFullscreen))
            state)
    :key nil}
   {:text (fn [state]
            (html
             [:div [:h2 "This game uses sound!"]
              [:br] "Type the word you hear in the box below."
              [:br]
              [:input
               {:id "captcha" :type :input :size 10
                :on-change (fn[e]
                             (let [word (-> e .-target .-value)]
                               (if (pass-captcha state word) (sim-key :right))))
                }]
              [:br] [:br]
              [:input
               {:type :button :value "Play Sound Again"
                :on-click (fn [e] (play-sound :word))}]
              ;; 20220411 - on old chromium 69 (debian) autoadvance did not workg
              [:br] [:br]
              [:span {:style {:font-size "smaller"}}
               "Push the right arrow key when you have entered the correct word"]]))
    ;; when we start out, we can skip this if we dont need to pass-captcha
    ;; ie settings/:skip-catcha == true
    :start (fn[state] (do (play-sound :word) state))

    ;; NB. presented second b/c if first,
    ;; going back another goes to min. which is this. endless loop
    :skip pass-captcha
    :stop identity
    ;; only advance if captcha is passed
    ;; will likely be here after sim-key from on-change textbox
    :key {:right (fn[state]
                   (if (pass-captcha state)
                     (-> state
                         ;; dont have to do captcha again
                         (assoc-in [:record :settings :skip-captcha] true)
                         ;; go to next instruction
                         (update-to-from 1 2))
                     state))}
    }
   {:text (fn[state]
            (html
             [:div (text-for :avatar-1)
              [:ul
               [:li (text-for :Use) " "
                (if (buttonbox? state)
                  (text-for :button-up)
                  "the up arrow")
                (text-for :change-sel)]
               [:li (text-for :Use) " "
                (if (buttonbox? state)
                  (text-for :choose-right)
                  [:span "the " [:b "right arrow"]])
                (text-for :avatar-continue)]]
              [:div#pick-avatars
               (map (partial avatar-example state) (keys sprite/avatars))]]))
    :start identity
    :stop (fn[state] (assoc-in state [:record :avatar] (:sprite-picked state)))
    :key {:down (fn[state]
                  (update state :sprite-picked (partial next-sprite :down)))
          :up (fn[state]
                (update state :sprite-picked (partial next-sprite :up)))}}
   {:text (fn[_] (text-for :fill-pond))
    :pos (fn[_] {:x 50 :y 250})
    :skip (fn[state] (contains? #{:ocean} (get-in state [:record :settings :vis-type])))
    :start (fn[{:keys [water time-cur] :as state}]
             (assoc-in state [:water :active-at] time-cur))
    :stop (fn [{:keys [water time-cur] :as state}]
            (assoc-in state [:water] (water/water-state-fresh)))}
   {:text (fn[_] (text-for :much-water))
    :pos (fn[_] {:x 50 :y 250})
    :skip (fn[state] (contains? #{:ocean} (get-in state [:record :settings :vis-type])))
    :start (fn[{:keys [water time-cur] :as state}]
             (assoc-in state [:water :level] 100))
    :stop (fn [{:keys [water time-cur] :as state}]
            (assoc-in state [:water] (water/water-state-fresh)))}
   {:text (fn[_] (html (text-for :quick-random-ocean)))
    :skip (fn[state] (not (contains? #{:ocean} (get-in state [:record :settings :vis-type]))))
    }

   {:text (fn[state]
            (html [:div
                   (if  (contains? #{:ocean} (get-in state [:record :settings :vis-type]))
                     "" ;; nothing to say if no pond/gold pile
                     (text-for :fill-goal))
                   [:br] (text-for :choose-walk) [:br]
                   (if (contains? #{:mri :eeg :seeg}
                                  (get-in state [:record :settings :where]))
                     (text-for :button-box)
                     "Use the arrow keys on the keyboard: left, up, and right"
                     )

                   ]))}
   {:text (fn[state]
            (html [:div (text-for :single-tap)
                   (when (not(contains? #{:mri :eeg :seeg} ;(contains? #{:online :practice})
                                        (get-in state [:record :settings :where])))
                     "Held keys misrepresent choice timing, leading to study disqualification."
                     )]
                  ))}
   {:text (fn[state]
            (html (text-for :only-bucket)))
    :pos (fn[state] (-> state find-close-well val position-next-to-well))
    :start (fn[state]
             (let [well-side (find-close-well state)]
               (-> state
                   (assoc-in [:avatar :destination] (-> well-side val :pos))
                   wells/wells-close
                   (assoc-in [:wells (key well-side) :open] true))))
    :stop (fn[state]
            (-> state
                (wells/wells-set-open-or-close [:left :up :right] true)
                (assoc-in [:avatar :destination] (:avatar-home @current-settings))))
    }

   {:text (fn[state] (html (text-for :empty-well)))
    :pos (fn[state] (-> state find-close-well val position-next-to-well))
    :start (fn[{:keys [time-cur wells] :as  state}]
             (let [side (-> state find-close-well key)]
               (-> state
                   (assoc-in [:wells side :active-at] time-cur)
                   (assoc-in [:wells side :score] false))))

    :stop (fn[state]
            (assoc-in state [:wells (-> state find-close-well key) :active-at] 0))
    }
   {:text (fn[state] (html (text-for :full-well)))
    :pos (fn[state] (-> state find-close-well val position-next-to-well))
    :start (fn[{:keys [time-cur wells] :as  state}]
             (let [side (-> state find-close-well key)]
               (-> state
                   (assoc-in [:wells side :active-at] time-cur)
                   (assoc-in [:wells side :score] true))))

    :stop (fn[state]
            (assoc-in state [:wells (-> state find-close-well key) :active-at] 0))
    }
   {:text (fn[state] (html (text-for :must-respond)))
    :pos (fn[state] {:x 0 :y 100})
    :start wells/all-empty
    :stop wells/wells-turn-off
    }
   ;; {:text (fn[state]
   ;;          (html [:div "Sometimes you will be too tired to walk." [:br]
   ;;                 "Instead, you will fade out until you are rested." [:br]
   ;;                 "This happens randomly and is "
   ;;                 [:b "not related"] " to your choice" [:br]]))
   ;;  :pos (fn[state] {:x 0 :y 100})
   ;;  :start (fn[state]
   ;;           (-> state
   ;;               (assoc :zzz (floater/zzz-new (avatar/top-center-pos state) 2))
   ;;               (assoc-in [:phase :fade] true)))
   ;;  :stop (fn[state] (-> state
   ;;                       (assoc-in [:phase :fade] false)
   ;;                       (assoc :zzz [])))
   ;;  }
   {:text (fn[state] (html [:div "This " (item-name :well) " is far away." [:br] " It'll take longer to get than the other two."]))
    :pos (fn[state] (-> state find-far-well val position-next-to-well))

    ;; skip instruction if far is same as close.
    ;; causes recursion if done in start:(update-to-from state i-cur (inc i-cur))
    :skip (fn[state]
            (-> @settings/current-settings (get-in [:step-sizes 1]) (= 0)))
    :start (fn[{:keys [time-cur wells] :as  state}]
             (let [side (-> state find-far-well key)]
               ;; otheriwse show this note about far taking longer
               (-> state
                   (assoc-in [:wells side :active-at] time-cur)
                   (assoc-in [:wells side :score] 1))))
    :stop (fn[state]
            (-> state
                ;; wells/wells-turn-off
                (assoc-in [:wells (-> state find-far-well key) :active-at] 0)
                ))}
   {
    ;; :pos (fn[state] (-> state (get-in [:avatar :pos])
    ;;                    (update :y #(- % 150))
    ;;                    (update :x #(- % 100))))
    :start (fn[state] (-> state
                          wells/wells-close
                          (assoc-in [:phase :show-cross] true)))
    :stop (fn[state] (-> state
                         (assoc-in [:phase :show-cross] nil)
                         (wells/wells-set-open-or-close [:left :up :right] true)))
    :text (fn[state]
            (html (text-for :white-cross)))}
   {
    :pos (fn[state] (-> @current-settings :bar-pos
                        (update :y #(- % 200))))
    :start (fn[state] (-> state
                          (assoc-in [:water :score] 10)
                          (assoc-in [:trial] (* 0.25 (count (get state :well-list ))))))
    :stop (fn[state] (-> state
                         (assoc-in [:water :score] 0)
                         (assoc-in [:trial] 0)))
    :text (fn[state] (html (text-for :progress-bar)))}
   {:text (fn[state] (html (text-for :different-wells )))}
   {
    ;; in case we're skipping instructions:
    ;;  send avatar to actual home (might be off if we're in landscape=ocean
    ;;also close all wells. might not have happened if we rushed through instructions
    :start (fn[state]
             (-> state
                 wells/wells-close
                 (assoc-in [:avatar :destination]
                           (:avatar-home @current-settings))))
    :text (fn[state] (text-for :ready))

    ;; trigger test/blocking in read-keys
    ;; :key ...
    }])


(defn read-keys [{:keys [key phase time-cur] :as state}]
  (let [dir (key/side-from-keynum-instructions (:have key))
        last-instruction (dec(count INSTRUCTION))
        i-cur (:idx phase)
        i-keyfn (get-in INSTRUCTION [i-cur :key dir])
        i-next (if dir (instruction-goto i-cur dir) i-cur)
        ]
    (cond
      ;; if instruction has special plans for keypushes
      (and i-keyfn 1)
      (-> state (i-keyfn) (assoc-in [:key :have] nil))

      ;; ready but need trigger
      ;; check before i-cur vs i-next so we cannot go backwards if mri
      (and (contains? #{:mri} (get-in state [:record :settings :where]))
           (= i-cur last-instruction))
      (if (= dir :trigger)
        (instruction-finished state time-cur)
        state)

      ;; changing which what we should see
      (not= i-cur i-next)
      (-> state
          (assoc-in [:key :have] nil)
          (update-to-from i-cur i-next))

      ;; we want to go past the end (are "ready")
      (and dir (= i-cur i-next) (>= i-cur last-instruction))
      (instruction-finished state time-cur)

      ;; otherwise no change
      :else
      state)))

(defn step
  "run by model/next-step from loop/time-update"
  [state time]
  (-> state
      water/water-pulse-forever
      avatar/move-avatar
      floater/update-state
      read-keys))
