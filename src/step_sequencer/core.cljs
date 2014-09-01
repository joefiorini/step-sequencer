(ns step-sequencer.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:playing false :steps
                      [{:freq 440 :amp 0.5 :silence false}
                       {:freq 880 :amp 0.7 :silence false}]
                      :context nil
                      :current-source nil}))

(defn play-sound [step source]
  (. js/console (log "play" step))
  (let [
        frequency (.-frequency source)]
    (set! (. frequency -value) (:freq step))))


(defn play-loop [steps source state]
  (let [last-step (last steps)
        playing (get @state :playing)]
      (. js/console (log (get @state :playing)))
    (when playing
      (doseq [step steps]
        (js/setTimeout
         (fn [] (play-sound (:step step) source)) (:delay step)))
      (js/setTimeout #(play-loop steps source state) (* (:delay last-step) (count steps))))))

(defn start-play-loop [state]
  (. js/console (log "2"))
  (let [context (js/AudioContext.)
        source (.createOscillator context)
        steps (get @state :steps)
    play-steps (map-indexed (fn [index step]
                   {:delay (* 500 index) :step step}) steps)]
    (om/update! state :current-source source)
    (.connect source (.-destination context))
    (.start source 0 0 0.25)
  (play-loop play-steps source state)))

(defn stop-play-loop [state]
  (let [source (get @state :current-source)]
    (.stop source 0)))

(defn start-playing [state ]
  (om/update! state :playing true)
  (. js/console (log "1"))
  (start-play-loop state))

(defn stop-playing [state]
  (om/update! state :playing false)
  (stop-play-loop state))

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

(defn step-view [{:keys [freq amp silence] :as step} owner]
  (reify
    om/IRender
    (render [this]
            (dom/div #js {:className "step"}
                     (dom/div nil freq)
                     (dom/div nil amp)
                     (dom/div nil silence)))))

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
