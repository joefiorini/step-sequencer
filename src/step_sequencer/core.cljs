(ns step-sequencer.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"}))

(defn play-state-controls [playing owner]
  (reify
    om/IRender
    (render [this]
            (dom/ul #js {:className "list-plain list-horizontal"}
                    (dom/li nil
                            (cond
                             playing (dom/button #js {:onClick #(om/set-state! owner :playing false)} "Stop")
                             :else (dom/button #js {:onClick #(om/set-state! owner :playing true)} "Play")))
                    (dom/li nil (dom/button nil "Reset"))
                    ))))

(defn sequencer-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
                {:playing false})
    om/IRenderState
    (render-state [_ {:keys [playing]}]
            (dom/div #js {:className "sequencer"}
                     (om/build play-state-controls playing)))))

(om/root sequencer-view app-state
  {:target (. js/document (getElementById "app"))})
