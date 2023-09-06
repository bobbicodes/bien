- # Squint
- The squint compiler is like a mirror image of my project. The core.js module looks like it could almost drop right into my project because indeed, it's much of Clojure implemented in JavaScript.
- For example, this is LazySeq:
- ```js
  export class LazySeq {
    constructor(f) {
      this.f = f;
    }
    *[Symbol.iterator]() {
      yield* this.f();
    }
  }
  ```
- This is what I should be using as a guide. It makes heavy use of iterables, which have made their way into my project quite a bit.
- I didn't even realize how similar it is. You could think of my editor window as being input to squint, turning the Clojure into js code. It's basically the same thing.
- Here is `re-matches`, which is a big help:
- ```js
  export function re_matches(re, s) {
    let matches = re.exec(s);
    if (matches && s === matches[0]) {
      if (matches.length === 1) {
        return matches[0];
      } else {
        return matches;
      }
    }
  }
  ```
- I'll just drop that right in!
- It's used in the cw and wordy exercises
- # `filter`
- This seems to almost work already. I can do this:
- ```clojure
  (filter #(> % 5) '(3 4 5 6 7))
  => (6 7)
  ```
- Amazing! However, it's only realized by the *printer*, which uses the [...obj] syntax to do it. But under the hood it's still an iterable, so nothing else knows how to use it:
- ```clolure
  (first (filter #(> % 5) '(3 4 5 6 7)))
  => 
  Error: seq: called on non-sequence 
  ```
- And we can't even check for equality:
- ```clojure
  (= '(6 7) (filter #(> % 5) '(3 4 5 6 7)))
  => false 
  ```
- Fixed the equality thing, and also `count`. The trick is to just cast them to arrays
- And now I can implement all the lazy stuff with this iterator!
- Now we have this
- ```clojure
  (apply + (filter #(= 5 %) [4 5 4 5]))
  => "0[object Object]"
  ```
- ```clojure
  (reduce + (filter #(= 5 %) [4 5 4 5]))
  => 
  Error: seq: called on non-sequence 
  ```
- I've got it back up to 96 tests passing. I suppose it's good enough to merge!
- ```clojure
  (take 5 (filter odd? (range 13))) => 
  Error: coll.slice is not a function 
  ```
- # Destructuring
- ok so this thing is such a monster...
- As a result of the above, even the basic case is broken:
- ```clojure
  (destructure [[a b] [1 0]])
  => 
  Error: 'a' not found 
  ```
- wait... that's normal bc it has to be quoted. the reason it works in let is because it's inside a macro and the bindings are not evaluated
- I just got totally spun out trying to get lazy seqs to work. The truth is, I still don't understand iterators/generators at all.
- I tried adding squint's concat, apply, into, which work for the individual functions but now only 22 tests are passing... it's turned into a mess. I might make this a branch and revert main back to where it was before today.
- ok, done. a shame though. well, it's not like I lost any work, it's just all on a feature branch.
- It's weird... once I added enough squint functions... even functions don't work anymore...
- ```clojure
  (defn hi []
    (+ 1 1))
  
  (hi)
  => ((+ 1 1))
  ```
- Like... it somehow broke, well not evaluation, but function evaluation.
- In other words, this still works:
- ```clojure
  (+ 1 1)
  => 2 
  ```
- If I macroexpand the `defn`, things get weirder:
- ```clojure
  (def hi (with-meta (fn [] (do ((+ 1 1)))) {:name "hi"}))
  ```
- I bet it's something to do with the macroexpansion machinery...
- I don't understand what would be putting the function body inside another list like that.
- I'm reading some of the issues in the squint repo. I'd love to be able to contribute, but it's also pretty scary, i.e. my immediate reaction is that the whole iterable/lazy stuff is a problematic can of worms.
- Yeah, I don't know about this stuff. It's just weird.
- Current fails:
- ['lev', 'word_chain', 'poker', 'my_group_by', 'powerset', 'rn', 'my_trampoline', 'lazy', 'allergies', 'dominoes', 'all_your_base', 'spiral_matrix', 'gtw', 'ttt2', 'happy', 'reversi', 'word_sort', 'eulerian', 'phone_number', 'meetup', 'wordy', 'go_counting', 'pig_latin', 'change', 'minesweeper', 'yacht', 'uce', 'my_merge_with', 'queen_attack', 'lt', 'graph', 'cw', 'robot_simulator', 'cards', 'run_length_encoding', 'atbash_cipher', 'binary_search_tree', 'ttt', 'luhn', 'scrabble_score', 'crypto_square', 'rotational_cipher', 'pov', 'trans_closure', 'binary', 'intervals', 'find_path', 'matching_brackets', 'binary_search', 'ps', 'f', 'seq_prons', 'octal', 'sh', 'veitch', 'clock', 'diamond', 'bal_num']
- Supposedly we don't have `Character/isLetter`... but we do. this is a mystery
- No matter what I do it doesn't work. And I can see it right in the env! wtf?!?! I've tried naming it different things, and defining it in clojure or javascript.
- I figured it out! I don't want to talk about it
- ok, there was a typo. In the core source it was Character/isletter
- Got it up to 130 tests passing! Implemented sets as functions.
- Uhhm... when we evaluate this string
- `"(((185 + 223.85) * 15) - 543)/2"`
- it tries to like... do something dumb
- we can't cast it to a string either
- ```clojure
  (str "(((185 + 223.85) * 15) - 543)/2")
  => "NaN/NaN" 
  ```
- This must be an issue with the reader, since we modified it for ratios and chars. It works in MAL
- It's being parsed as a ratio. Fixed it!
- Implemented partition with pad. Now 134 tests passing
- run-length encoding
- ```clojure
  (defn encoder-groups [string]
    (re-seq #"(.)\\1*" string))
  
  (defn encoder-values [group]
    (str (if (> (count group) 1)
           (count group)
           "")
         (first group)))
  
  (defn run-length-encode [s]
    (let [groups (re-seq #"(.)\\1*" s)]
      (apply str
             (map encoder-values groups))))
  
  (defn decoder-groups [string]
    (re-seq #"(\\d+)?(.)" string))
  
  (defn decoder-values [group]
    (apply str (repeat (Integer/parseInt (first (re-seq #"\\d+" group))) 
                 (last group))))
  
  (defn run-length-decode [s]
    (let [groups (re-seq #"(\\d+)?(.)" s)]
      (apply str
             (map decoder-values groups))))
  ```
- ```clojure
  (defn lt [cols]
    (let [row-num (dec (count cols))
          col-num 
          (loop [acc -1 cur (apply max cols)]
             (if (= 0 cur) acc
             (recur (inc acc) (quot cur 2))))
          is-mine 
          (fn [r c]
            (if (or (nil? r) (nil? c) (> c col-num) (> r row-num)) false
              (let [validate-v (bit-shift-left 1 (- col-num c))
                    candidate-v (nth cols r)]
            (> (bit-and validate-v candidate-v) 0))))
          line-from (fn [r c fr fc] (if (is-mine r c)
                                      (loop [acc [] next-col (fc c) next-row (fr r) l 2]
                                        (if (or (> next-col col-num) (> next-row row-num) (not (is-mine next-row next-col)))
                                          acc
                                          (recur (concat acc [{:r1 r :c1 c :r2 next-row :c2 next-col :l l :fr fr :fc fc}])
                                            (fc next-col)
                                            (fr next-row)
                                            (inc l))))
                                      []))
          mix-cols (fn [col1 col2] (mapcat (fn [e] (map (fn [e1] [e e1]) col1)) col2))
          all-line-from (fn [r c] (concat (line-from r c inc inc)
                                    (line-from r c inc identity)
                                    (line-from r c identity inc)
                                    (line-from r c inc dec)))
          size1 (fn [a b c] (let [mxl (max a b c) mnl (min a b c)] 
                              (/ (* (inc mxl) mnl) 2)))
          validate-tr (fn [l1 l2] (and (= (l1 :r2 ) (l2 :r2 )) (= (l1 :c2 ) (l2 :c2 ))
                                    (not (and (= (l1 :fr ) (l2 :fr )) (= (l1 :fc ) (l2 :fc ))))))
          triangle-from-line (fn [line] (let [lines1 (all-line-from (line :r1 ) (line :c1 ))
                                              lines2 (all-line-from (line :r2 ) (line :c2 ))
                                              line-combines (mix-cols lines1 lines2)
                                              validate-combines (filter #(validate-tr (first %) (second %)) line-combines)]
                                          (map #(size1 (line :l ) ((first %) :l ) ((second %) :l )) validate-combines)))
        all-lines (flatten (let [points (mix-cols (range (inc row-num)) (range (inc col-num)))]
                             (mapcat #(apply all-line-from %) points)))
  
        all-size (flatten (map triangle-from-line all-lines))]
         (if (empty? all-size) nil (apply max all-size))))
  
  (lt [15 15 15 15 15])
  ```
- This fails:
- ```clojure
  (re-find #"^1?([2-9]..)([2-9]..)(....)$" "2234567890")
  ```
- There's something really weird happening in the phone-number exercise, something I never remember seeing before. Mysteriously inconsistent results. First it works, then it doesn't. At least when it doesn't work there's something to investigate, but if it sometimes works... I guess this is what they call an heisenbug.
- I think I do vaguely remember something like this, actually. It seems to have something to do with the tests
- Sometimes it works:
- ```clojure
  (defn area-code [input]
    (first (parts input)))
  
  (area-code "2234567890")
  => "223" 
  ```
- It seems to work as long as we don't touch the tests. Then, mysteriously, it does this:
- ```clojure
  (deftest area-code
    (is (= "223" (area-code "2234567890"))))
  => 
  Error: f.apply is not a function 
  ```
- Ah! I remember now! It's because the test is named the same as the function! OK!
- I implemented division and equality on ratios, but much like things like symbols, other things like assoc don't respect it. It will happily put duplicate keys in a map:
- ```clojure
  {1/2 [[1 2]] 1/2 [[2 4]] 2/3 [[4 6]] 1/2 [[3 6]]}
  ```
- It also made a few tests fail, but there's still 132 passing so it might be a net win...
- I needed to check while dividing an integer by 1 before returning a ratio
- Say is failing, and that bothers me because I like that one. It's something to do with remainders, specifically the `int` function. Here's the issue:
- ```clojure
  (quot 1 1000)
  => 
  Error: x.charCodeAt is not a function 
  ```
- Did it! needed to implement `int` on ratios. Cool. Fraction-js has its own floor() function
- Nice! Back up to 138 tests passing
- complex numbers is failing because now we have to actually convert things to decimals because division now returns rationals by default.
- 141 tests passed 😍
- `quot` still sometimes returns rationals, which is wrong:
- ```clojure
  (quot 1640945 42)
  => 39070/1 
  ```
- `contains?` fails on vector keys:
- ```clojure
  (contains? {[:x :x :x] 1} [:x :x :x])
  => false 
  ```
- The implementation is simply `coll.has(key)`
- I fixed it, but now I need to fix `get`:
- ```clojure
  (get {[:x :x :x] :x} [:x :x :x])
  => nil 
  ```
- I did it! Wow, that was a big improvement. It's just using the iterator instead of the built-in `has()` method:
-
- ```js
  if (types._hash_map_Q(coll)) {
          for (const [k, value] of coll) {
              if (types._equal_Q(k, key)) {
                  return value
              }
          }
      }
  ```
- `group-by` is failing on equality check
- ```clojure
  (group-by frequencies ["meat" "mat" "team" "mate" "eat"])
  => 
  Error: Unknown type 'undefined'
  
  (defn group-by [f coll]
    (reduce
     (fn [ret x]
       (let* [k (f x)]
             (assoc ret k (conj (get ret k []) x))))
     {} coll))
  ```
- Curiously, the first step works:
- ```clojure
  (def coll ["meat" "mat" "team" "mate" "eat"])
  (def f frequencies)
  (def x "meat")
  (def k (f x))
  (def ret {})
  
  (assoc ret k (conj (get ret k []) x))
  => {{"m" 1 "e" 1 "a" 1 "t" 1} ["meat"]} 
  ```
- And so does the second (uh, oh...)
- ```clojure
  (def ret {{"m" 1 "e" 1 "a" 1 "t" 1} ["meat"]})
  (def x "mat")
  
  (assoc ret k (conj (get ret k []) x))
  => {{"m" 1 "e" 1 "a" 1 "t" 1} ["meat"] {"m" 1 "e" 1 "a" 1 "t" 1} ["meat" "mat"]} 
  ```
- Ah... it's because I forgot to implement not-found on get with maps
- oh, but it's failing the equality check
- omg I did it! I had to add a check in `types_equals_Q()`:
- ```js
  case 'hash-map':
              if (a.size !== b.size) { return false; }
              for (var [key, value] of a) {
                  if (!b.has(key)) { return false; }
                  if (!_equal_Q(a.get(key), b.get(key))) { return false; }
              }
              return true;
  ```
- Looking at [[associative destructuring]]
- [[Clojure interpreter notes page 14]]