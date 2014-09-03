(ns step-sequencer.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:playing false :steps
                      [{:freq 130.81 :amp 0.5 :duration 0.5 :silence false}
                       {:freq 146.83 :amp 0.7 :duration 0.5 :silence false}
                       {:freq 164.81 :amp 0.7 :duration 0.5 :silence false}
                       {:freq 174.61 :amp 0.7 :duration 0.5 :silence false}
                       {:freq 196.00 :amp 0.7 :duration 0.5 :silence false}]
                      :context nil
                      :current-source nil}))
(defn indices-of [f coll]
  (keep-indexed #(if (f %2) %1 nil) coll))

(defn first-index-of [f coll]
  (first (indices-of f coll)))

(defn find-thing [value coll]
  (first-index-of #(= % value) coll))

(defn play-sound [step context]
  (. js/console (log "play" step))
  (let [source (.createOscillator context)
        frequency (.-frequency source)
        stop-time (+ 0.3 (.-currentTime context))]
    (.connect source (.-destination context))
    (set! (. frequency -value) (:freq step))
    (.start source 0)
    (.stop source stop-time)))

(defn calc-delay [step index]
  (* 500 index))

(defn play-loop [state]
  (let [playing (get @state :playing)
        context (get @state :context)]
      (. js/console (log (get @state :playing)))
    (when playing
      (doseq [step (get @state :steps)]
        (js/setTimeout
         (fn [] (play-sound step context)) (calc-delay step (find-thing step (get @state :steps)))))
      (js/setTimeout #(play-loop state)
                     (+ (calc-delay (last (get @state :steps))
                                    (find-thing (last (get @state :steps)) (get @state :steps))) 500)))))

(defn start-play-loop [state]
  (. js/console (log "2"))
  (let [context (js/AudioContext.)
        steps (get @state :steps)]
    (om/update! state :context context)
 (play-loop state)))

(defn start-playing [state ]
  (om/update! state :playing true)
  (. js/console (log "1"))
  (start-play-loop state))

(defn stop-playing [state]
  (om/update! state :playing false))

(defn play-state-controls [state owner]
  (reify
    om/IRender
    (render [this]
            (dom/ul #js {:className "list-plain list-horizontal"}
                    (dom/li nil
                            (cond
                             (get state :playing) (dom/button #js {:onClick
                               #(stop-playing state)} "Stop")
                             :else (dom/button #js {:onClick
                               #(start-playing state)} "Play")))
                    (dom/li nil (dom/button nil "Reset"))
                    ))))

(defn new-step []
  {:freq 440 :amp 0.5 :silence false})

(defn new-silence []
  {:freq 0 :amp 0 :silence true})

(defn add-step [state _]
  (om/transact! state :steps #(conj % (new-step))))

(defn add-silence [state _]
  (om/transact! state :steps #(conj % (new-silence))))

(defn add-step-view [state owner]
  (reify
    om/IRender
    (render [this]
            (dom/button #js {:onClick #(add-step state owner)} "Add Step"))))

(defn add-silence-view [state owner]
  (reify
    om/IRender
    (render [this]
            (dom/button #js {:onClick #(add-silence state owner)} "Add Silence"))))

(defn update-step-freq [e step owner]
  (om/transact! step :freq (fn [_] (.. e -target -value))))

(defn step-view [{:keys [freq amp silence] :as step} owner]
  (om/component
   (dom/div #js {:className "step"}
            (dom/input #js {:value freq :onChange #(update-step-freq % step owner)})
            (dom/div nil amp)
            (dom/div nil silence))))

(defn step-container-view [{:keys [steps] :as state} owner]
  (reify
    om/IRender
    (render [this]
            (. js/console (log steps))
            (apply dom/div #js {:className "steps-container"}
                     (om/build-all step-view steps)))))

(defn sequencer-view [state owner]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:className "sequencer"}
                     (om/build play-state-controls state)
                     (dom/ul #js {:className "list-plain list-horizontal"}
                             (dom/li nil (om/build add-step-view state))
                             (dom/li nil (om/build add-silence-view state)))
                     (om/build step-container-view state)))))

(om/root sequencer-view app-state
  {:target (. js/document (getElementById "app"))})
