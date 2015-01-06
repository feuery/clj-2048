(ns merpg.2D.make-game
  (:require [seesaw.core :as seesaw]
            [merpg.2D.core :refer [image
                                   *buffer*
                                   merpg-object?
                                   in?
                                   get-class
                                   object
                                   move
                                   with-color
                                   key-down?
                                   Rect
                                   Draw
                                   width
                                   height
                                   key-up?]])
  (:import  [java.awt Color]
            [java.awt.event KeyEvent]
            [java.awt.image BufferedImage]
            [javax.imageio ImageIO]))

(defn map-entry? [obj]
  (instance? clojure.lang.MapEntry obj))

(defn Listen [widget event fn]
  (seesaw/listen widget event fn)
  widget)

(defn make-game [objects & {:keys [window-width window-height title
                                   pre-drawqueue
                                   post-drawqueue
                                   update
                                   mouse-clicked] :or {window-width 800 window-height 600 title "Testi"
                                                pre-drawqueue (fn [_])
                                                mouse-clicked (fn [_])
                                                post-drawqueue (fn [_])
                                                update (fn [state] state)}}]
  (let [view (image window-width window-height)
        state (atom objects)

        valid-keys [KeyEvent/VK_UP KeyEvent/VK_DOWN KeyEvent/VK_LEFT KeyEvent/VK_RIGHT KeyEvent/VK_CONTROL KeyEvent/VK_SHIFT KeyEvent/VK_ESCAPE]
        valid-codes [ :up :down :left :right :ctrl :shift :esc]
        keyboard-state (atom (zipmap valid-codes (repeat (count valid-codes) false)))

        keycode-to-keyword (fn [keycode]
                             (cond
                              (= keycode KeyEvent/VK_UP) :up
                              (= keycode KeyEvent/VK_DOWN) :down
                              (= keycode KeyEvent/VK_LEFT) :left
                              (= keycode KeyEvent/VK_RIGHT) :right
                              (= keycode KeyEvent/VK_CONTROL) :ctrl
                              (= keycode KeyEvent/VK_SHIFT) :shift
                              (= keycode KeyEvent/VK_ESCAPE) :esc
                              :t (throw (Exception. (str "Keylistener failed with KeyCode " keycode)))))

        create-keylistener (fn [retval e]
                             (fn [state]
                               (if (in? valid-keys (.getKeyCode e))
                                 (assoc state (keycode-to-keyword (.getKeyCode e)) retval)
                                 state)))
        
        f (-> (seesaw/frame
               :width window-width
               :height window-height
               :on-close :exit
               :title title)
              (Listen :key-pressed (fn [e]
                                     (swap! keyboard-state (create-keylistener true e))))
              (Listen :key-released (fn [e]
                                      (swap! keyboard-state (create-keylistener false e)))))
        viewport  (-> (seesaw/canvas
                   :paint (fn [jee g]
                            (.setColor g Color/BLACK)
                            (try
                              
                              (.drawImage g view 0 0 nil)
                              (catch IllegalArgumentException ex
                                (println "Viewport hajosi!")
                                (throw ex)))))
                      (Listen :mouse-released (fn [e]
                                                (mouse-clicked (vec (map int [(/ (.getX e) 50) (/ (.getY e) 50)]))))))
        render-loop (doto
                        (Thread.
                         (fn []
                           (println "Rendering began")
                           (binding [merpg.2D.core/*buffer* view
                                     key-down? (fn [key]
                                                (key @keyboard-state))
                                     key-up? (complement key-down?)]
                             (println "Rendering started")
                             (Thread/sleep 600)
                             (try
                               (println "Rendering really began")
                               (loop []
                                 (Rect 0 0 (width) (height) :fill? true)
                                 
                                 (pre-drawqueue @state)
                                 
                                 (let [*draw-queue* (->> @state
                                                         (filter map-entry?)
                                                         (map #(nth % 1))
                                                         (filter merpg-object?))]
                                   
                                   (doseq [to-draw *draw-queue*]
                                     ;; Now we can init state to the objects-map without damning the rendering process
                                     (try
                                       (Draw (if (and (not (merpg-object? to-draw))
                                                      (map-entry? to-draw))
                                               (nth to-draw 1)
                                               to-draw))
                                       (catch IllegalArgumentException ex
                                         (println "Nyt kusee dispatching...")
                                         (println "Piirrettävä olio tyyppiä: " (get-class to-draw))
                                         (throw ex))
                                       (catch UnsupportedOperationException ex
                                         (println "Nth epäonnistui?")
                                         (println "(not (Objekti?)) " (not (merpg-object? to-draw)))
                                         (println "map-entry?" (map-entry? to-draw))
                                         (throw ex))
                                       (catch Exception ex
                                         (println "Rendering failed")
                                         (throw ex)))))

                                 (post-drawqueue @state)
                                 
                                 (.repaint viewport)

                                 (swap! state update)

                                 (Thread/sleep 80)
                                 (when (.isVisible f)
                                   (recur)))
                               (catch UnsupportedOperationException ex
                                 (println "Joku tekee tyhmiä juttuja jollekkin tässä säikeessä")
                                 (println ex))))))
                      (.start))]
    (println "Everything set up")
    (seesaw/config! f :content viewport)
    (seesaw/show! f)))
