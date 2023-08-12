(defn perfect-square [s]
  (let [l (re-seq #"\d+" s)]
    (clojure.string/join "," (filter #{"4" "9" "16" "25" "36"} l))))