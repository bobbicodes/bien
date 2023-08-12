(defn cw [word board]
  (let [across (map #(clojure.string/escape % {\space "", \_ \.}) board)
        down (apply map str across)]   ;; transpose the board so down becomes across
    (string? (->> (concat across down)
                  (mapcat #(clojure.string/split % #"#"))
                  (some #(re-matches (re-pattern %) word))))))