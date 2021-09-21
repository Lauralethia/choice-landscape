(ns landscape.view
  (:require
   [cljsjs.react]
   [cljsjs.react.dom]
   [sablono.core :as sab :include-macros true]
   [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn dbg-last break]]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]]
   ))

(let [node (.getElementById js/document)]
  (defn change-body
  "replace body w/ sab/html element"
  [reactdom]
    (.render js/ReactDOM reactdom node)))

(defn display-state
  "html to render for display. updates for any change in display"
  [state] (sab/html
           [:div "hi"]))


