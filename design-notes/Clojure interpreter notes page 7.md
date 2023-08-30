- # `for`
	- Here is the real `for` macro from Clojure
	- ```clojure
	  (defmacro for [seq-exprs body-expr]
	    (assert-args
	       (vector? seq-exprs) "a vector for its binding"
	       (even? (count seq-exprs)) "an even number of forms in binding vector")
	    (let [to-groups (fn [seq-exprs]
	                      (reduce1 (fn [groups [k v]]
	                                (if (keyword? k)
	                                  (conj (pop groups) (conj (peek groups) [k v]))
	                                  (conj groups [k v])))
	                              [] (partition 2 seq-exprs)))
	          err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))
	          emit-bind (fn emit-bind [[[bind expr & mod-pairs]
	                                    & [[_ next-expr] :as next-groups]]]
	                      (let [giter (gensym "iter__")
	                            gxs (gensym "s__")
	                            do-mod (fn do-mod [[[k v :as pair] & etc]]
	                                     (cond
	                                       (= k :let) `(let ~v ~(do-mod etc))
	                                       (= k :while) `(when ~v ~(do-mod etc))
	                                       (= k :when) `(if ~v
	                                                      ~(do-mod etc)
	                                                      (recur (rest ~gxs)))
	                                       (keyword? k) (err "Invalid 'for' keyword " k)
	                                       next-groups
	                                        `(let [iterys# ~(emit-bind next-groups)
	                                               fs# (seq (iterys# ~next-expr))]
	                                           (if fs#
	                                             (concat fs# (~giter (rest ~gxs)))
	                                             (recur (rest ~gxs))))
	                                       :else `(cons ~body-expr
	                                                    (~giter (rest ~gxs)))))]
	                        (if next-groups
	                          #_"not the inner-most loop"
	                          `(fn ~giter [~gxs]
	                             (lazy-seq
	                               (loop [~gxs ~gxs]
	                                 (when-first [~bind ~gxs]
	                                   ~(do-mod mod-pairs)))))
	                          #_"inner-most loop"
	                          (let [gi (gensym "i__")
	                                gb (gensym "b__")
	                                do-cmod (fn do-cmod [[[k v :as pair] & etc]]
	                                          (cond
	                                            (= k :let) `(let ~v ~(do-cmod etc))
	                                            (= k :while) `(when ~v ~(do-cmod etc))
	                                            (= k :when) `(if ~v
	                                                           ~(do-cmod etc)
	                                                           (recur
	                                                             (unchecked-inc ~gi)))
	                                            (keyword? k)
	                                              (err "Invalid 'for' keyword " k)
	                                            :else
	                                              `(do (chunk-append ~gb ~body-expr)
	                                                   (recur (unchecked-inc ~gi)))))]
	                            `(fn ~giter [~gxs]
	                               (lazy-seq
	                                 (loop [~gxs ~gxs]
	                                   (when-let [~gxs (seq ~gxs)]
	                                     (if (chunked-seq? ~gxs)
	                                       (let [c# (chunk-first ~gxs)
	                                             size# (int (count c#))
	                                             ~gb (chunk-buffer size#)]
	                                         (if (loop [~gi (int 0)]
	                                               (if (< ~gi size#)
	                                                 (let [~bind (.nth c# ~gi)]
	                                                   ~(do-cmod mod-pairs))
	                                                 true))
	                                           (chunk-cons
	                                             (chunk ~gb)
	                                             (~giter (chunk-rest ~gxs)))
	                                           (chunk-cons (chunk ~gb) nil)))
	                                       (let [~bind (first ~gxs)]
	                                         ~(do-mod mod-pairs)))))))))))]
	      `(let [iter# ~(emit-bind (to-groups seq-exprs))]
	          (iter# ~(second seq-exprs)))))
	  ```
	- I wonder if it would still work without lazy seqs?
	- Since it's rather chunky, let's break it down.
	- The macro takes, as we know, `seq-exprs` and `body-expr`. Apparently the body can only be one form? Yes, TIL.
	- Then we have the `let`, which has 2 bindings, each of which is a function, called `to-groups` and `emit-bind`.
	- `to-groups`:
	- ```clojure
	  (defn to-groups [seq-exprs]
	    (reduce (fn [groups [k v]]
	              (if (keyword? k)
	                (conj (pop groups) (conj (peek groups) [k v]))
	                (conj groups [k v])))
	            [] (partition 2 seq-exprs)))
	  ```
	- Let's run an example this far and see how it works! I'll start with just the simplest one, i.e.
	- ```clojure
	  (to-groups '[x [0 1 2]
	               y [0 1 2]])
	  => [[x [0 1 2]] [y [0 1 2]]]
	  ```
	- If I just remove the destructuring it should work in mine.
	- The bindings are called keys/values, so I will too from now on.
	- Here's the working result:
	- ```clojure
	  (defn to-groups [seq-exprs]
	    (reduce (fn [groups binding]
	              (if (keyword? (first binding))
	                (conj (pop groups) (conj (peek groups) [(first binding) (last binding)]))
	                (conj groups [(first binding) (last binding)])))
	            [] (partition 2 seq-exprs)))
	  ```
	- Now for `emit-bind`, which is much bigger. It takes these destructuring bindings:
	- ```clojure
	  [[[bind expr & mod-pairs]
	     & [[_ next-expr] :as next-groups]]]
	  ```
	- wtf even is this? The function takes the output of `to-groups`, which in this case is
	- ```clojure
	   [[x [0 1 2]] 
	    [y [0 1 2]]]
	  ```
	- In simple cases like this, the groups are unchanged. Here's a bigger example, which works in my app
	- ```clojure
	  (to-groups '[x [0 1 2 3 4 5]
	        :let [y (* x 3)]
	        :when (even? y)])
	  => [[x [0 1 2 3 4 5] [:let [y (* x 3)]] [:when (even? y)]]]
	  ```
	- You know what... let me put this on a fresh page, [[for macro]]
	-
	- If I remove `assert-args` and change `reduce1` to `reduce`, it works in Clojure, but not babashka.
	- Let's do a little dive into macros, starting with Clojure brave.
- # Macros
	- ## Clojure brave
		- Not much I didn't know in here. Except I learned that quasiquote is what Clojure calls syntax quote. Also it explains those `iter->` functions in Mal that I was confused about because they look like macros. They're macro helpers. I kind of figured that, but good to hear that it's a common thing.
		- Oh... I learned about auto-gensym, which is that `#` thingy! I thought that was just a convention or something.
		- It's still really really muddy, though. It's explained matter-of-factly, but my head spins when I think about how I'm ever going to apply this stuff myself. I guess that's what this project is for - to give me a practical angle to start learning. Eventually it might not be so intimidating.
	- ## Joy of Clojure
		- I like the sound of this already:
		- > Where macros get complicated is when you try to
		  bring theoretical knowledge of them into the real
		  worldd, so to help you combat that we’ll lead you on a
		  tour of practical applications of macros.
		- This book is immediately denser. Brave was really just a primer, though I really admire Daniel's knowledge and writing. It's just geared towards being light and beginner-friendly.
		- For example, there's this very detailed treatment of `syntax-quote`:
		- ### Handling nested syntax-quotes
		- > Dealing with nested syntax-quotes can at times be complicated. But you can visualize
		  the way in which unquoting affects the nested structures as a result of repeated evaluations (Steele 1990) relative to its nesting level:
		- ```clojure
		  (let [x 9, y '(- x)]
		  (println `y)
		  (println ``y)
		  (println ``~y)
		  (println ``~~y)
		  (contextual-eval {'x 36} ``~~y))
		  ; user/y
		  ; (quote user/y)
		  ; user/y
		  ; (- x)
		  ;=> -36
		  
		  ```
		- I first saw this and got really scared because I don't know what to make of it. But then I looked at the next page:
		- > The nesting of the syntax-quotes in the first two println calls takes the value of y
		  further up the abstraction ladder. But by including a single unquote in the third
		  println, we again bring it back down. Finally, by unquoting a second time, we’ve
		  created a structure that can then be evaluated—and doing so yields the result -36.
		  We had to use contextual-eval in the tail because core eval doesn’t have access
		  to local bindings—only var bindings. One final note is that had we attempted to
		  unquote one extra time, we’d have seen the exception java.lang.IllegalState Exception: Var clojure.core/unquote is unbound. The reason for this error is
		  that unquote is the way to “jump” out of a syntax-quote, and to do so more than
		  nesting allows will cause an error. You won’t use this technique in this chapter, and
		  in most cases you won’t need to utilize it unless you’re planning to create macro-defining macros—something you won’t do until section 14.4.3.
		- It mentions `macroexpand-all` from clojure.walk, and gives this warning in the notes:
		- > The macroexpand-all function is a useful debugging aid, as we’ll demonstrate in this chapter. But it’s worth knowing that unlike the other macroexpand functions, it doesn’t use exactly the same logic as the Clojure compiler and thus may in some unusual circumstances produce misleading results.
		- This is probably the reason for the macroexpand in tools.analyzer. I should learn more about this because we are using it in the Exercism representer.
	- > Most control structures in Clojure are implemented via macros, so they provide a nice
	  starting point for learning how macros can be useful.
		- My thoughts exactly! I love this book! It has all the explanations I need to accompany my quest.
	- > One way to design macros is to start by writing out example code that you wish
	  worked
- Now I know why some macros from Clojure don't work... because I don't have auto-gensym. It's a good thing I figured that out before I got much more confused!
- It still doesn't explain why `and` wasn't working, the one I got from the Mal project...
- # `lazy-seq`
	- I want to take a stab at this. It might get weird.
	- I'm taking a radically logical approach. If the interpreter sees `lazy-seq`, it stops execution, stores the ast, and then... does absolutely nothing. So there. How much lazier can you get?
	- So... how *is* `lazy-seq` used?
	-