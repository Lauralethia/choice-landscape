(ns landscape.view-test
  (:require [landscape.view :as view :refer [photodiode-color]]
            ;[cljs.test :as t :include-macros true]
            [clojure.test :refer [is deftest]]
))


(deftest photodiode-color-test
  ;; default color
  (is (= "black" (photodiode-color {:name "dasfsafd"})))
  ;; neighboring phases do not have the same color
  (is (not (= (photodiode-color {:name :iti})
              (photodiode-color {:name :chose})))))

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
