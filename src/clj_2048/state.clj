(ns clj-2048.state
  (:require [clojure.pprint :refer [pprint]]))

(def W 6)
(def H 4)

(defn vectorify-2d [seq]
  (->> seq
       (map vec)
       vec))

;; Can I apply automatic transformation on values trying to be set on this atom?
;; Or do I have to vectorify-2d this atom in the tail-position of every swap!?
(def world (atom (->>  false
                       (repeat W)
                       (repeat H)
                       vectorify-2d)))

(defn transpose [seq-of-seqs]
  {:pre [(not (empty? seq-of-seqs))]}
  (try
    (apply mapv vector seq-of-seqs)
    (catch clojure.lang.ArityException ex
      (println "ArityEx @ transpose")
      (println "seq-of-seqs: " seq-of-seqs)
      (throw ex))))

(defn getin [vec x y]
  (-> vec
      (nth y)
      (nth x)))

(defn pretty-print-world! []
  (pprint  @world))

(defn width [world]
  (count world))

(defn height [world]
  (count (first world)))

(defn spawn-randomly [world]
  "Returns a new world with a 2 or 4 - tile spawned randomly"
  (let [new-tile (if (< (rand-int 10) 5)
                   2
                   4)
        coord-pairs (for [x (range 0 (width world))
                          y (range 0 (height world))]
                      [x y])
        viable-coord-pairs (->> coord-pairs
                                (filter (complement (partial get-in world))))
        ;;Nth returns what it's supposed to and get returns garbage - wtf clj?
        final-coord-pair (nth viable-coord-pairs (rand-int (count viable-coord-pairs))
                              :you-lost)]
    (if (= final-coord-pair :you-lost)
      :you-lost
      (->> new-tile
           (assoc-in world final-coord-pair)
           vectorify-2d))))

(defn spawn-randomly! []
  "Changes the current state of the game. When this returns symbol :you-lost, game is over."
  (swap! world spawn-randomly))
