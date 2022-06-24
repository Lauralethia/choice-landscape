(ns landscape.view
  (:require
   [landscape.sprite :as sprite]
   [landscape.utils :as utils]
   [landscape.model :as model]
   [landscape.url-tweak]
   [landscape.settings :refer [current-settings]]
   [landscape.instruction :as instruction]
   [landscape.model.survey :as survey]
   [landscape.key :as key]
   [cljsjs.react]
   [cljsjs.react.dom]
   [sablono.core :as sab :include-macros true :refer-macros [html]]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]])
  (:require-macros [devcards.core :refer [defcard]]))

;;
(def ^:export DEBUG
  "show phase edn? display will no longer be pixel perfect
  use in javscript console like: landscape.view.DEBUG = true"
  false)

(def CHANGE-DOM-ID "id of element where updates will go"
  "main-container")
(defn change-dom
  "replace body w/ sab/html element"
  [reactdom]
    (let [node (.getElementById js/document CHANGE-DOM-ID)]
      (.render js/ReactDOM reactdom node)))

(defn position-at
  [{:keys [x y] :as pos} inner]
  (html [:div.abs {:style {:position "absolute"
                           :transform (str "translate(" x "px, " y "px")}}
         inner]))


;; scene components
(defn water-fill
  "show progress with growing image. scale by FILL"
  [fill]
  (let [img (case (:vis-type @current-settings)
              :mountain "imgs/money_pool.png"
              :wellcoin "imgs/money_pool.png"
              :ocean "imgs/money_pool.png" ;; NB no fill for :ocean but instructions die
              "imgs/water.png"
              )]
    (html [:img#water {:src img :style {:transform (str "scale(" (/ fill 100) ")")}}])))

(defn water [state]
  (let [fill (get-in state [:water :scale])]
    (water-fill fill)))

;; TODO: should this be in phase.cljs?
(defn photodiode-instructions
  "Paging between instructions can be used to test hardware (color based on instruction idx)"
  [phase]
  (if (even? (or 0 (:idx phase))) "white" "black"))
(defn photodiode-white-on
  "white at onset of new phase. cleared after 20ms (is it ms?)"
  [{:keys [start-at] :as phase} time-cur]
  (if (< (- time-cur start-at) 20) "white" "black"))
(defn photodiode-color [{:keys [phase time-cur] :as state}]
  (if (= (:name phase) :instruction)
    (photodiode-instructions phase)
    (photodiode-white-on phase time-cur)))

(defn photodiode-color-steps
  "define colors for each phase. DEPRICATED 20220624. but keeping incase RTBox can use"
  [{:keys [name] :as phase}]
  (cond
     ; RTBox maybe on any step (untested 20220624)
     (= name :waiting)     "white"
     (= name :chose)       "#333"
     (= name :iti)         "#666"
     (= name :feedback)    "black"

     ;; switch every other instruction
     (= name :instruction) (photodiode-instructions phase)
     (= name :survey)      "#999"
     :else                 "black"))

(defn photodiode
  "display block to position photodiode over. for percision timing.
  multidisbatch: default position is top left. uses photodiode-color to set.
  likely white for a brief period"
  ([state] (photodiode state [0 0]))
  ([{:keys [phase] :as state} pos]
     (when (-> state :record :settings :use-photodiode?)
       (html [:div#photodiode {:style {:background-color (photodiode-color state)}}]))))

(defn progress-bar
  "show how far along we are in the task."
  [{:keys [trial well-list water] :as  state}]
  (let [ntrials (count well-list)
        score (/ (:score water) ntrials)
        progress (/ trial ntrials)
        vis-class (-> @current-settings :vis-type name)]
    (position-at (:bar-pos @current-settings)
                 (html [:div#fullbar {:class vis-class}
                        [:div#progressbar_trials {:class vis-class
                                                  :style {:height "100%" :width (str (* progress 100) "%")}}]
                        ;[:div#progressbar_score  {:style {:height "49%" :width (str (* score 100) "%")}}]
]))))


;; points
(defn show-point-floating
  "show scored points (eg '+1') at position
  should be floated up by state update
  list of points in state:points-floating "
  [{:keys [pos points progress] :as p}]
  (position-at
   pos
   (html [:div.pointfloating
          {:style {:opacity (- 1 progress)}}
          "+" (str points)])))

(defn show-points-floating [{:keys[points-floating] :as state}]
  "show all points in points-floating{:pos :points}"
  (html [:div (mapv #'show-point-floating points-floating)]))


;; catch event sleepy ZZZs
(defn show-zzz-floating
  "show a Z floating around"
  [{:keys [alpha size pos body] :as zzz}]
  (position-at
   pos
   (html [:div.zzz {:style {:opacity alpha :scale (str size "%")}}
          body])))

(defn show-all-zzz [{:keys[zzz] :as state}]
  "show all zzzs that could be floating"
  (html [:div (mapv #'show-zzz-floating zzz)]))

;;
(defn bucket []
  (let [imgsrc (case (get @current-settings :vis-type)
                           :mountain "imgs/axe.png"
                           :wellcoin "imgs/chest.png"
                           :ocean "imgs/key.png"
                           "imgs/bucket.png")]
              (html [:img {:src imgsrc :style {:transform "translate(20px, 30px)"}}])))

(defn well-or-mine
  "what sprite to use based on the board setting. default to well"
  [] (case (get @current-settings :vis-type)
       :mountain sprite/mine
       :wellcoin sprite/wellcoin
       :ocean sprite/chest
       sprite/well))

(defn well-show "draw single well. maybe animate sprite"
        [{:keys [time-cur] :as state}
         {:keys [active-at score open] :as well}]
        (let [tstep (sprite/get-step time-cur active-at (:dur-ms sprite/well))
              css (sprite/css (well-or-mine) tstep)
              ;; if not score, move bg down to get the failed well offset
              v-offset (if score {}
                           {:background-position-y
                            (str "-" (:height sprite/well) "px")}
                           )]
          (html [:div.well {:style (merge css v-offset)}
                 (if open (bucket))])))


(defn well-side
  "side is :left :up :right"
  [{:keys [wells] :as state} side]
  (position-at (get-in wells [side :pos])
               (html [:div {:on-click #(key/sim-key side)}
                      (well-show state (side wells))
                      (if DEBUG [:div (-> wells side :prob)])])))

(defn well-show-all
  "3 wells not all equadistant. sprite for animate"
  [{:keys [wells] :as state}]
  (html [:div.wells
         (well-side state :left)
         (well-side state :up)
         (well-side state :right)]))

(defn button-keys [] (html [:div.bottom
                               [:button {:on-click #(key/sim-key :left)
                                         :class "arrow"}
                                [:img {:src "imgs/arrow_l.png"}]]
                               [:button {:on-click #(key/sim-key :up)
                                         :class "arrow"}
                                [:img {:src "imgs/arrow_l.png"
                                       :class "rot_up"}]]
                               [:button {:on-click #(key/sim-key :down)
                                         :class "arrow"}
                                [:img {:src "imgs/arrow_l.png"
                                       :class "rot_down"}]]
                               [:button {:on-click #(key/sim-key :right)
                                         :class "arrow"}
                                [:img {:src "imgs/arrow_l.png"
                                       :class "rot_lr"}]]]))
(defn instruction-view [{:keys [phase] :as state}]
        (let [idx (or (:idx phase) 0)
              instr (get instruction/INSTRUCTION idx)
              pos-fn (or (:pos instr) (fn[_] {:x 0 :y 0}))]
          (position-at (pos-fn state)
                       (html [:div#instruction
                              [:div.top (str (inc idx) "/" (count instruction/INSTRUCTION))]
                              [:br]
                              ((:text instr) state)
                              [:br]
                              (button-keys)]))))
(defn survey-view [{:keys [phase] :as state}]
  (let [qi (or  (:qi phase) 0)
        ci (or  (:ci phase) 0)
        quest (get-in survey/SURVEYS [qi :q])
        choices (get-in survey/SURVEYS [qi :answers])
        cur-choice (if (count choices) (nth choices ci) "ALL DONE")
        ]
    (position-at {:x 100 :y 10}
                 (html [:div#insturctions
                   [:div#instruction
                    [:div.top (str (inc qi) "/" (count survey/SURVEYS))]
                    [:h3 quest]
                    [:ul#pick (mapv
                               #(html [:li { ;; :id  TODO
                                            :class
                                            (if (= cur-choice %) "picked" "ignored")}  %])
                               choices)]
                    (button-keys)]]))))

(defn create-json-url [data]
  (let [jsonstr (.stringify js/JSON (clj->js data) )
        blob (js/Blob. [jsonstr] {"type" "application/json"})]
    (.createObjectURL js/URL blob)))

(defn done-view [state]
  (html [:div#instruction
         [:h1 "Great Job!"] ;[:h3 "You filled the pond!"]; TODO pond might be mine
         [:br] "Thank you for contributing to our research!!"
         ;; if online, show compeltion code
         (if (contains? #{:online} (get-in state [:record :settings :where]))
           [:div
             [:br] "Your responses have been recorded. " [:br]
             ;; code defaults to WXYZ1. hopefully it's been updated since finish
             [:div.confirmcode "Your completion code is " [:br]
                       [:h3 (get-in state [:record :mturk :code] )] [:br]
                       "Save it for your records." [:br]]
             [:span "You can close this page."]])
         ;; when mri, offer to download json
         (if (contains? #{:mri :eeg} (get-in state [:record :settings :where]))
           [:div [:a
                  ;; make a useful name for the json output that we could save
                  ;; includes url params and current timestep
                  ;; TODO: most useful when running as a static page
                  ;; but wont have a useful url path in that case!
                  ;; maybe set id= and pull from path anchor?
                  {:download (str (landscape.url-tweak/path-info-to-id
                                   (get-in state [:record :path-info]))
                                  "_"
                                  (:time-cur state)
                                  ".json")
                   :href (-> state :record create-json-url)
                   } "download task data"]])
         [:br]]))


(defn view-score [score]
  (html [:div#scorebox "Total: " score]))

(defn popup-state
  "show state. reacts to button push when DEBUG
  TODO: maybe do more than console.log"
  [state]
  (console.log (clj->js state)))

(defn maybe-disable [disable? html]
  (if disable?
    (sab/html [:div  {:style {:opacity "50%"}}  html])
    html))

(defn display-state
  "html to render for display. updates for any change in display"
  [{:keys [phase avatar] :as state}]
  (let [avatar-pos (get-in state [:avatar :pos])
        vis-class (-> @current-settings :vis-type name)]
    (sab/html
     [:div#background {:class vis-class}
      (progress-bar state)

      (if DEBUG [:div {:style {:color "white"}}
                 [:br] (str (:key state))
                 [:button {:on-click (fn [] (popup-state state))} "show"]])

      (if (-> state :phase :name (= :instruction) not)
        (view-score (get-in state [:water :score])))
      (if (contains? #{:mountain :desert :wellcoin} (get @current-settings :vis-type))
         (water state))
      (well-show-all state)
      ;; NB. this conditional is only for display
      ;; we're waiting regardless of whats shown
      ;; dont want to rework the logic to be agnostic to actual phase
      ;; :show-cross exists only in instruction phase
      (if (or (get phase :show-cross)
              (= :iti (:name phase)))
        ;; cross does not get centered well. off by a few pixels
        (position-at (update avatar-pos :x #(+ % 5))
                     (html [:div.iti "+"]))
        ;; catch event will show a disabled (opaque) avatar
        (maybe-disable
         (or (= (:name phase) :catch)
             (:fade phase))             ;; fade set in instructions
         (position-at avatar-pos (sprite/avatar-disp state avatar))))

      (show-points-floating state)

      ;; draw ZZZ over avatar during catch trial
      (show-all-zzz state)

      ;; instructions on top so covers anything else
      ;; -- maybe we want them under?
      (case (:name phase)
        :instruction  (instruction-view state)
        :survey (survey-view state)
        :forum (survey/view-questions)
        :done (done-view state)
        nil)

      ;; and very top add photodiode square (when anchor portion of url calls for it)
      (photodiode state)])))



;; debug/devcards
(defcard well-no-and-score
  "well animation by steps. should animate with js/animate"
  (fn [state owner]
    (utils/wrap-state state [:div
                             (well-show @state (assoc @state :score nil))
                             (well-show @state (assoc @state :score 1))]))
  {:time-cur 100 :active-at 100 :open true})

(defcard mine-sprite
  "mine animation by steps"
  (fn [state owner]
    (with-redefs [current-settings (atom (assoc @current-settings :vis-type :mountain))]
      (utils/wrap-state state [:div
                                 (well-show @state (assoc  @state :score nil))
                                (well-show @state (assoc  @state :score 1))])
        ))
  {:time-cur 100 :active-at 100 :open true})
(defcard wellcoin-sprite
  "well but with coins animation by steps"
  (fn [state owner]
    (with-redefs [current-settings (atom (assoc @current-settings :vis-type :wellcoin))]
      (utils/wrap-state state [:div
                                 (well-show @state (assoc  @state :score nil))
                                (well-show @state (assoc  @state :score 1))])
        ))
  {:time-cur 100 :active-at 100 :open true})
(defcard chest-sprite
  "well but with coins animation by steps"
  (fn [state owner]
    (with-redefs [current-settings (atom (assoc @current-settings :vis-type :ocean))]
      (utils/wrap-state state [:div
                                 (well-show @state (assoc  @state :score nil))
                                (well-show @state (assoc  @state :score 1))])
        ))
  {:time-cur 100 :active-at 100 :open true})

(defcard avatar
  "step through avatar"
  (fn [state owner]
    (html [:div
           (utils/wrap-state
            state
            (maybe-disable
             (:disabled? @state)
             (sprite/avatar-disp @state @state)))
           [:button {:on-click (fn [] (swap! state assoc :direction :left))} "left"]
           [:button {:on-click (fn [] (swap! state assoc :direction :right))} "right"]
           [:button {:on-click (fn [] (swap! state assoc :direction :up))} "up"]
           [:button {:on-click (fn [] (swap! state assoc :direction :down))} "down"]
           [:button {:on-click (fn [] (swap! state assoc :disabled?
                                             (not (:disabled? @state))))} "catch?"]
           [:br]
           [:select {:on-change #(swap! state assoc :sprite-picked (-> % .-target .-value keyword))}
            (map #(html [:option { :value (name %)} (name %)]) (keys sprite/avatars))]
           [:br]
           (str @state)
           ]))
  {:time-cur 100 :active-at 100 :direction :left :sprite-picked :astro :disabled? false})

(defcard photodiode-card
  "giant box to trigger event stim. style adjusted to fit here. will be bigger and fixed to top of screen. depending on photo sensor, might only be able to get on or off. currently have 4 levels"
  (fn [state owner] (html
           [:div
            [:button {:on-click (fn [] (swap! state assoc-in [:phase :name] :chose))} "chose"]
            [:button {:on-click (fn [] (swap! state assoc-in [:phase :name] :waiting))} "waiting"]
            [:button {:on-click (fn [] (swap! state assoc-in [:phase :name] :feedback))} "feedback"]
            [:button {:on-click (fn [] (swap! state assoc-in [:phase :name] :iti))} "iti"]

            ;; "relative !important" otherwise fixed to top of page
            ;; this way it's at least close to the controlling/testing buttons
            (html [:div {:style {:position "relative !important" :scale "10%"}}
                   (photodiode @state)])]))
  {:phase {:name :chose} :record {:settings {:use-photodiode? true}}})
