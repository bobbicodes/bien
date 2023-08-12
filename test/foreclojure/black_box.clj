 (defn black-box [c]
  ((zipmap (map str [{} #{} () []]) [:map :set :list :vector]) (str (empty c))))