(defn camel [s]
  (let [words (re-seq #"[a-zA-Z]+" s)
        words (cons (first words)
                    (map clojure.string/capitalize
                         (rest words)))]
    (apply str words)))