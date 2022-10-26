(ns landscape.timeline-test
  (:require  [cljs.test :as t :include-macros true]
             [landscape.mr-times]
             [landscape.model.timeline :refer
              [side-probs gen-wells reduce-time-fill
               fill-mr-times default-probabilities shuffle-blocks]]
             [clojure.test :refer [is deftest]]))

(deftest side-probs-test
  (is (= (first (side-probs 20 50 :left))
         {:up 20 ,:right 50,:left 100 :side-best :left}))
  (is (= (first (side-probs 80 20 50 :left))
         {:up 20 ,:right 50,:left 80 :side-best :left})))

(deftest have-iti
  (let [wells (gen-wells { :prob-low 20 :prob-high 50 :reps-each-side 1 :side-best :left})]
    (is (= 1000  (get-in wells [0 :iti-dur])))))

(deftest mrfixed
  (let [trial {:itidur 500 :good {:open true}, :nogood {:open false}, :up {:open true}}
        probs {:good 100 :nogood 20 :up 50}
        gono {:good :left :nogood :right}
        red (reduce-time-fill trial probs gono)]
    ;; didn't mess with itidur
    (is (= (:itidur red) 500))
    ;; no change
    (is (get-in red [:up :open]))
    ;; update prob
    (is (= (get-in red [:up :prob]) 50))
    (is (= (get-in red [:left :prob]) 100))
    ;; nogood is right is closed
    (is (not (get-in red [:right :open])))))

;; {:itidur 500,
;; ;;  :up {:open true, :prob 50, :step 1},
;; ;;  :left {:open true, :prob 100, :step 1},
;; ;;  :right {:open false, :prob 20, :step 1}}
(defn abs [x] (max x (- x)))
(defn sideprob-cnts [mr side]
  (map #(count(second %)) (group-by (fn[t] (get-in t [side :prob])) mr)))
(deftest mrfixed-all-left
  (let [mr (fill-mr-times landscape.mr-times/mr-seeds :goodnogood {:good :left :nogood :right})
        maxdiff (apply max (map #(abs (- 100 (count (filter(fn[t](get-in t [% :open])) mr)))) [:left :up :right]))
        leftcnt (sideprob-cnts mr :left)
        upcnt (sideprob-cnts mr :up)
        rightcnt (sideprob-cnts mr :right)]
    (is (= (count mr) 150))
    (is (<= maxdiff 1))
    ;; 3 blocks (50 20 100) w/same count (50 trials) for each
    (is (= 3 (count upcnt)))
    (is (every? #(= 50 %) upcnt))

    (is (= 1 (count leftcnt)))
    (is (every? #(= 150 %) leftcnt))
    (is (= 3 (count rightcnt)))
    (is (every? #(= 50 %) rightcnt))
    ))
(deftest mrfixed-all-right
  (let [mr (fill-mr-times landscape.mr-times/mr-seeds :goodnogood {:good :right :nogood :left})
        leftcnt (sideprob-cnts mr :left)
        rightcnt (sideprob-cnts mr :right)]
    (is (= 1 (count rightcnt)))
    (is (every? #(= 150 %) rightcnt))
    (is (= 3 (count leftcnt)))
    (is (every? #(= 50 %) leftcnt))
    ))

(deftest mr-prob-test
  "good is only ever 100.
  nogood and up should trade off starting low
  and should have a different value for each of the 3 blocks"
  (let [d (default-probabilities)]
    (is (= 3 (-> (map  #(get-in d [0 %]) [:good :up :nogood]) distinct count)))
    (is (= 3 (-> (map #(get-in d [% :up]) [0 1 2]) distinct count)))
    (is (= 3 (-> (map  #(get-in d [% :nogood]) [0 1 2]) distinct count)))
    (is (= 1 (-> (map  #(get-in d [% :good]) [0 1 2]) distinct count))))
  ;; is randomly assign
  (is (= 2 (-> (map #(get-in (default-probabilities) [0 :nogood])  (range 100))
               distinct
               count))))

(deftest use-all-mr-seeds
  (is (= 9 (->
            (map #(let [[seeds blocks] (shuffle-blocks landscape.mr-times/mr-seeds 3)]
                    seeds)
                 (range 50))
            ;; 48 if we dont flatten 9C3 == 84
            ;; about half of the combos dont match right/left evenly
               flatten
               distinct
               count))))
