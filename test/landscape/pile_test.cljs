(ns landscape.pile-test
(:require
   [landscape.model.pile :as pile]
   [clojure.test :refer [is deftest]]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]))

(deftest make-test "create grid"
  (let [g (pile/grid-make 2 3 0)]
    (is (= 0 (get-in g [0 0])))
    (is (= 2 (alength g)))
    (is (= 3 (alength (get g 0))))))
(deftest swap-test "set and swap"
  (let [g (pile/grid-make 2 3 0)]
    (aset g 0 0 -2)
    (is (= -2 (get-in g [0 0])))
    (pile/grid-swap g 0 0 1 1)
    (is (=  0 (get-in g [0 0])))
    (is (= -2 (get-in g [1 1])))))

