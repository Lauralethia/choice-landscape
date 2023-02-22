(ns landscape.view-test
  (:require [landscape.view :as view :refer [photodiode-color]]
            ;[cljs.test :as t :include-macros true]
            [clojure.test :refer [is deftest]]
))


(deftest photodiode-color-test
  (let [state {:phase{:name "notaknownstate" :start-at 0} :time-cur 0}
        state-sometime (assoc state :time-cur 1000 )
        state-phasecolor (assoc-in state [:record :settings :pd-type] :phasecolor)]
    ;; default color
    (is (= "white" (photodiode-color state)))
    ;; color changes after some time
    (is (not= (photodiode-color state)
              (photodiode-color state-sometime)))
    ;; even 1second in colors are fixed if pd-type is phasecolor
    (is (= "white" (photodiode-color (assoc-in state-phasecolor [:phase :name] :choice))))
    (is (= "white" (photodiode-color (assoc-in state-phasecolor [:phase :name] :waiting))))
    (is (= "black" (photodiode-color (assoc-in state-phasecolor [:phase :name] :iti))))))

(deftest encode-url-test
  "only confirms function runs. does not check output makes sense"
  (is (re-find #"blob:" (view/create-json-url {:a "b"}))))

(deftest done-message-by-where
  "have link at end of mri and no code; :online is opposite"
  (let [state {:record {:mturk {:code "abcd"}
                        :settings {:where :mri}}}
        state-online (assoc-in state [:record :settings :where] :online)]
    ;; mri has download link not code
    (is (re-find #"blob:" (-> state view/done-view js->clj str)))
    (is (not (re-find #"completion code" (-> state view/done-view js->clj str))))
  
    ;; online is opposite
    (is (not (re-find #"blob:" (-> state-online view/done-view js->clj str))))
    (is (re-find #"completion code" (-> state-online view/done-view js->clj str)))))
