- Alright, so the following exercise tests are passing:
	- 1. hello world
	  2. two-fer
	  3. reverse string
	  4. accumulate
	  5. series
	  6. robot_name
	  7. anagram
	  8. triangle
	  9. word_count
	  10. armstrong_numbers
	  11. difference_of_squares
	  12. roman_numerals
- Which one should I try next?
- Series requires `set`. I can implement that.
- Cool, the problem was that `arguments` is already an array, with the actual args as the first element.
- Now I'm dealing with an issue where equals is not working like it should.
- Wait... it's because when I changed `set` to use the first of the args, it broke the set literals! How annoying!
- I think I'll have to make another function called `toSet`.
- ok done. But there's also a bug in equals, which is... it's not implemented on sets yet!
- Alright, that was easy! It's handled the same as list and vector.
- Now up to `robot-name`. We need:
	- `repeatedly`
	- `format`
	- `rand-nth` ✅
	- `rand-int` ✅
- `format` is a huge can of worms. I could try using a library... actually, I found a stack overflow solution that seems to work well enough... or maybe not
- Paula Gearon wrote a Clojurescript implementation: https://github.com/quoll/clormat
- This is really good! And there's good tests to follow. What a great find. It was on clojuredocs. See you at [[format]]
- hahaha, so I realized that often times it's easier to modify the solutions than to add features to my language, so I skipped format.
- Now I ran into a keyword as a function. it would be cool to implement that!
- I'm getting some kind of bug with atoms.
- # Chars
	- I want to support character literals, i.e. `\A`. Obviously javascript doesn't have them, but I should be able to read them and treat them as single character strings.
	- I got it! First, in the reader:
	- Add this to `read_atom`:
	- ```js
	   } else if (token[0] === "\\") {
	          return _char(token[1]);
	  ```
	- In types, add this to `_obj_type`
	- ```js
	   else if (_char_Q(obj)) { return 'char'; }
	  ```
	- ```js
	  // Chars
	  export function _char(obj) {
	      if (typeof obj === 'string' && obj[0] === '\\') {
	          return obj;
	      } else {
	          return "\\" + obj
	      }
	  }
	  
	  export function _char_Q(obj) {
	      return typeof obj === 'string' && obj[0] === '\\';
	  }
	  ```
	- Pretty sure that's it. Great! Much easier than I thought it might be. I thought I might have to modify the tokenizer, but I saw that it indeed matched as long as it was escaped properly in the code. Now I just need to figure out what I need to do with them.
	- The place I remember seeing them was in expressions like `(int \A)`, to coerce a char to an int. I could implement that.
- # Regex bug (`re-seq`)
	- This returns nil:
	- `(re-seq #"[A-Z]{2}\\d{3}" (robot-name (robot)))`
	- Same if it's called directly on a string: `(re-seq #"[A-Z]{2}\\d{3}" "JN081")`
	- oh... it comes up as `nil` in clojurescript as well...
	- This one fails, but works in cljs: `(re-seq #"\d" "clojure 1.1.0")`. It returns `Error: Cannot read properties of null (reading '0')`
	- Also this `(re-seq #"\w+" "mary had a little lamb")`
	- Clearly there's something messed up.
	- I fixed the regex (sort of) by just using the output of `re.exec(s)`. But I'm getting very strange behavior, like it works sometimes but not others. It's confusing because it works if I evaluate everything in order, but not in the actual tests. It's very weird. It gives me `Error: f.apply is not a function` unless I actually evaluate the forms in order, and when I run the tests, it somehow loses the definitions. very weird!
	- I did have to modify the `is` function, because it wasn't doing the right thing.
	- It seems like when there's a `let`, it loses the definitions in the env.
	- Wait, no... it's the `deftest`. That's much better, I don't want there to be a flaw in the env
	- This is the repro:
	- ```clojure
	  (def letters (map char (range 65 91)))
	  
	  (defn generate-name [] 
	     (apply str (concat (repeatedly 2 (fn [] (rand-nth letters)))  
	        (repeatedly 3 (fn [] (rand-int 10))))))
	  
	  (defn robot []
	    (atom {:name (generate-name)}))
	  
	  (defn robot-name [robot]
	    (get (deref robot) :name))
	  
	  (deftest robot-name
	    (let [a-robot (robot-name/robot)
	          its-name (robot-name/robot-name a-robot)]
	        (is (re-seq #"[A-Z]{2}\\d{3}" its-name))))
	  ```
	- If I take away the deftest and leave the inner let, it all works.
	- I confirmed that this fails as well:
	- ```clojure
	  (deftest robot-name
	    (is (re-seq #"[A-Z]{2}\d{3}" (robot-name (robot)))))
	  ```
	- Somehow, the `deftest` makes it forget the definitions. Once you evaluate it, everything else is wiped out, at which point even this won't work: `(robot-name (robot))`
	- It works with this deftest commented out, but fails if uncommented:
	- ```clojure
	  #_(deftest robot-name
	    (is true))
	  
	  (robot-name (robot))
	  ```
	- So.... that's the minimal repro.
	- Why this deftest? We've used it on dozens of forms already and the only thing I can think of that's new is the atoms, so, it would stand to reason that it's the problem.
	- I could "cheat" and figure out how to do it without atoms. But no, I need to figure out what the problem is.
	- Waaaat. I took away the atom and it still breaks.
	- omg. I sure feel dumb now... it's because the test is named the same thing as the var. Holy shit! I should come up with a way to save it as a variation
- Robot name passes now!
- I had to shuffle the exercises array to get a new exercise. I got anagram. It needs `sort`, which should be really simple... wait... we do have sort! Aha. It's not implemented on strings!
- we need `and`... done! now anagram works!
- I just found `some`:
- ```clojure
  (def some
    (fn (pred xs)
      (if (empty? xs)
        nil
        (or (pred (first xs))
            (some pred (rest xs))))))
  ```
- That made 1 more exercises pass, `triangle`! Now there's 8.
- Looking at `word-count`. Giving the ol' `TypeError: Cannot read properties of null (reading '0')`.
- We need `split`. An alternate approach in the tests uses `frequencies` and `re-seq`. `frequencies` sounds fun, actually
- # `frequencies`
	- ```clojure
	  (frequencies ['a 'b 'a 'a])
	  {a 3, b 1}
	  ```
	- First try!
	- ```clojure
	  (frequencies ['a 'b 'a 'a]) => {"a" 3 "b" 1} 
	  ```
- There's a problem with the regular expression syntax, because the slashes require escaping due to the normal reading process I guess.  Is there a way we could fix this, specifically for regex?
- Got it!
- # `re-seq` continued
	- So I never figured this out.
	- I want to make it behave like Clojure, where the slashes don't need to be escaped. But any unescaped slashes are already swallowed by the time they are interpreted.
	- ok, I think I've got it figured out... but the slashes need to be escaped, there doesn't seem to be any other way. And if the regex comes from a file, there needs to be 4 slashes so that it will be 2 in the user code.
- Word count is solved!
- # `distinct?`
	- I implemented this using a javascript set, but there's a problem: it doesn't recognize symbols as distinct.
	- What I'll do is implement `distinct`, and have it use that. That way I can hopefully allow for the edge cases.
	- The `=` function does the right thing. So I'll just loop through it and check every element.
	- fuck. No matter what I try it... well, it recognizes duplicate symbols... and then puts them in the new coll anyway! wtf?!?
	- Here's the Clojure impl:
	- ```clojure
	  (defn distinct [coll]
	     (let [step (fn step [xs seen]
	                  (lazy-seq
	                    ((fn [[f :as xs] seen]
	                       (when-let [s (seq xs)]
	                         (if (contains? seen f)
	                           (recur (rest s) seen)
	                           (cons f (step (rest s) (conj seen f))))))
	                     xs seen)))]
	       (step coll #{})))
	  ```
	- I'm pissed. Think I'd better move on before I want to break something.
- # Triangle
	- This exercise is taking awhile. It's the only one that visibly hangs, all the others are evaluated extremely quickly. Let's see what's up.
	- Weird... it doesn't look like such a big deal. It's just a bunch of comparisons. How anticlimactic. But indeed... the test run takes a few seconds by itself.
	- There's a benchmark thingy, isn't there?
	- Added `time`. It prints the elapsed time to the console, because I don't know how to print 2 return values. And it only took 2265msecs.
	- Here's the weird part - the actual computations, without the testing part, took only 293msecs. I'm confused. Don't the other exercises have just as many test assertions? They're only checks of `true?` or `false?`.
	- There's 20 of them. Let's say I make all the functions just return true.
	- 2msecs! So wtf... it's not the testing stuff, and it's not the calculations. It's a paradox!
- # Finally... `core.clj`
	- I finally got load-file working! But... will it work in prod? Let's find out...
	- ```clojure
	  (load-file "src/core.clj")
	  myvar => "this is my var" 
	  ```
	- In order for the static asset to be available in prod it needs to be imported, but it can't because it contains undefined vars or something
	- omg, I just found the answer... in the docs https://vitejs.dev/guide/assets.html#importing-asset-as-string
	- this is the solution:
	- ```js
	  import core from './src/core.clj?raw'
	  ```
	- OMG it works! Holy shit!!!
	- One small issue is quotes have to be double-escaped.
	- I've got all the code moved over and it works great! Feels great... I'd been stuck on this problem for weeks.
- # Destructuring
	- I'm like delusional or masochistic if I think I'm gonna do this right now, but it's worth examining how it even works.
	- Input: `(destructure* '[[a b] [1 2]])`
	- Output:
	- ```clojure
	  [vec__461
	   [1 2]
	   a
	   (#object[clojure.core$nth 0x75cd3577 "clojure.core$nth@75cd3577"] vec__461 0 nil)
	   b
	   (#object[clojure.core$nth 0x75cd3577 "clojure.core$nth@75cd3577"] vec__461 1 nil)]
	  ```
	- Those weird looking things... are nothing more than the `nth` function. The final vector is output by: `(list nth gvec n nil)`
	- So it's really just this:
	- ```clojure
	  [vec__461 [1 2] 
	   a (nth vec__461 0 nil)
	   b (nth vec__461 1 nil)]
	  ```
	- ok, I think I get it! It just expands it to a regular let binding!
	- So, don't tell anyone, but... I think I can make the code much cleaner. Sure, it works, put it's a scary mess. I've got a style, it might seem very amateur, because it is, but that isn't a bad thing. I want it to be understandable, like all the rest of my code.
	- I'm thinking I could get the basic case covered first.
	- ## Destructuring `destructure` (haha)
		- So I broke it out into 2 functions, so it's slightly tamed.
		- Broke out another function. If I keep doing this, it might make sense!
		- I wonder if these functions are inlined for performance? That would make sense. So what I'm doing is disassembling.
		- Great! It's all functions that I can see on one screen.
		- So what is this process business? Like, what are we processing?
		- `pb` might be process builder? It becomes `process-entry`, which is a reducing function.
		- Eh... I broke it somehow by tearing it apart. It worked with a simple vector, but failed when I tried destructuring a map.
		-
	- In the cljs source I found this:
		- ```clojure
		  (defn seq-to-map-for-destructuring
		    "Builds a map from a seq as described in
		    https://clojure.org/reference/special_forms#keyword-arguments"
		    [s]
		    (if (next s)
		      (.createAsIfByAssoc PersistentArrayMap (to-array s))
		      (if (seq s) (first s) (.-EMPTY PersistentArrayMap))))
		  ```
	- Here's the destructuring test suite: https://github.com/clojure/clojurescript/blob/6aefc7354c3f7033d389634595d912f618c2abfc/src/test/cljs/cljs/destructuring_test.cljs#L9
	- Somehow... I fixed map destructuring. But idk what I did... I was just noodling around, and now it works:
	- ```clojure
	  (def client {:name "Super Co."
	               :location "Philadelphia"
	               :description "The worldwide leader in plastic tableware."})
	  
	  (destructure '[{name :name
	                  location :location
	                  description :description} client])
	  ```
	- Output:
	- ```clojure
	  [map__13417 client
	   map__13417 (if (seq map__13417)
	                  (clojure.core/seq-to-map-for-destructuring map__13417)
	                   map__13417)
	   name (get map__13417 :name)
	   location (get map__13417 :location)
	   description (get map__13417 :description)]
	  ```
- Related to the editor, not the interpreter: It would be nice to implement [[slurp and barf]]
- Starting [[Clojure interpreter notes page 4]]
-