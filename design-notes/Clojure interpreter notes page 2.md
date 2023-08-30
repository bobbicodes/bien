- So now that multiarity functions work, we can try to work through the Clojure source. Slowly, obviously. Because there will doubtlessly be half a dozen other things that won't work at every step...
- # When-let
	- This is a macro. It needs to only evaluate the body if the test is true.
	- Haha this is the best way to get me to actually learn everything
	- ```clojure
	  (defmacro when-let
	    "bindings => binding-form test
	  
	    When test is true, evaluates body with binding-form bound to the value of test"
	    {:added "1.0"}
	    [bindings & body]
	    (assert-args
	       (vector? bindings) "a vector for its binding"
	       (= 2 (count bindings)) "exactly 2 forms in binding vector")
	     (let [form (bindings 0) tst (bindings 1)]
	      `(let [temp# ~tst]
	         (when temp#
	           (let [~form temp#]
	             ~@body)))))
	  ```
	- So first we need `assert-args`
	- ```clojure
	  (defmacro ^{:private true} assert-args
	    [& pairs]
	    `(do (when-not ~(first pairs)
	           (throw (IllegalArgumentException.
	                    (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
	       ~(let [more (nnext pairs)]
	          (when more
	            (list* `assert-args more)))))
	  ```
	- There seems to be a critical difference in how macros work.
	- In mine, the body needs to be inside a function:
	- ```clojure
	  (defmacro when (fn [x & xs] (list 'if x (cons 'do xs))))
	  ```
	- But in Clojure it's done implicitly:
	- ```clojure
	  (defmacro when [test & body] (list 'if test (cons 'do body)))
	  ```
	- ```js
	  case 'defmacro':
	          var func = types._clone(EVAL(a2, env));
	          func._ismacro_ = true;
	          return env.set(a1, func);
	  ```
	- So yes, it doesn't define a function, rather it clones one. Let's see if we can change that.
	- It keeps getting deeper. for assert-args, we need this:
	- ```clojure
	  (defn spread
	    {:private true
	     :static true}
	    [arglist]
	    (cond
	     (nil? arglist) nil
	     (nil? (next arglist)) (seq (first arglist))
	     :else (cons (first arglist) (spread (next arglist)))))
	  
	  (defn list*
	    "Creates a new seq containing the items prepended to the rest, the
	    last of which will be treated as a sequence."
	    {:added "1.0"
	     :static true}
	    ([args] (seq args))
	    ([a args] (cons a args))
	    ([a b args] (cons a (cons b args)))
	    ([a b c args] (cons a (cons b (cons c args))))
	    ([a b c d & more]
	       (cons a (cons b (cons c (cons d (spread more)))))))
	  ```
	- So there's a problem with our multiarity handling that prevents the variadic arity being called.
	- Besides that, it *almost* works:
	- ```clojure
	  (list* 1 2 [3 4])
	  => (1 2 [3 4] nil)
	  ```
	- The `nil` should not be there. Which doesn't make sense because the first arity just calls seq on the args, which works:
	- ```clojure
	  (seq [3 4])
	  => (3 4)
	  ```
	- We have the basic variadic functionality:
	- ```clojure
	  (defn yo [a & more]
	    [a more])
	  
	  (yo 1 2 3 4 5)
	  => [1 (2 3 4 5)]
	  ```
	- Which means that if we look for an `&` in the arglist, we could define it as `<function>-variadic`, and find it when it's called likewise.
	- Continue this in section below
- # `core.clj`
	- I kind of want to start a proper thing now. Do we have load-file?
	- no, actually `slurp` doesn't even work. But I thought it did...
	- God this is so annoying. I think I'm gonna give up because this is so stupid.
	- The only lead I have is to do something like what I did for the download feature in MECCA:
	- ```clojure
	  [:button
	         {:on-click #(let [file-blob (js/Blob. [@(subscribe [:notes])] #js {"type" "text/plain"})
	                           link (.createElement js/document "a")]
	                       (set! (.-href link) (.createObjectURL js/URL file-blob))
	                       (.setAttribute link "download" "mecca.txt")
	                       (.appendChild (.-body js/document) link)
	                       (.click link)
	                       (.removeChild (.-body js/document) link))}
	         "Download"]
	  ```
	- This makes me think that it could be possible to do the opposite. I just found the code somewhere and it seems sufficiently hacky, i.e. it accomplishes something that afaict shouldn't be possible. It makes a fake link and automatically clicks it.
	- I don't think I feel like doing this now because I'm ultra pissed and was sort of happy working on my actual project. I just thought it would be nice to not have to evaluate code using 100 calls to `evalString`, but alas.
- # Multi-arity with variadic
	- I took a peek at how SCI does it: https://github.com/babashka/sci/blob/master/src/sci/impl/fns.cljc
	- Here's the basic idea:
	- ```clojure
	  (defn fn-arity-map [ctx enclosed-array fn-name macro? fn-bodies]
	    (reduce
	     (fn [arity-map fn-body]
	       (let [f (fun ctx enclosed-array fn-body fn-name macro?)
	             var-arg? (:var-arg-name fn-body)
	             fixed-arity (:fixed-arity fn-body)]
	         (if var-arg?
	           (assoc arity-map :variadic f)
	           (assoc arity-map fixed-arity f))))
	     {}
	     fn-bodies))
	  ```
	- I just tried to break out the function stuff into a separate module to organize the project better, but I couldn't get it to work. Another annoying fail, that's 2 today. Like ok, I'll just have this giant pile of shit
	- Anyway. Is there allowed to be more than one arity that is variadic? I need to find out.
	- I didn't find the answer in the docs. I might have missed it. But it only took 10 seconds at the repl:
	- ```clojure
	  (defn a
	    ([] "no args")
	    ([x] x)
	    ([x y] (str x y))
	    ([x y & more] (str x y "and" (apply str more)))
	    ([x y z & more] (str x y z "and" (apply str more))))
	  ; clojure.lang.ExceptionInfo: Can't have more than 1 variadic overload user
	  ```
	- So there we go! That makes it easier, because all we have to do is look for the `&`.
	- Got it! The definition part, anyway. Now I need to handle calls.
	- ![image.png](../assets/image_1690452072921_0.png)
	- Alright! That went surprisingly well!
- # Testing (CI)
	- So I figured out why the tests have been broken... though I don't understand why.
	- I remember noticing when the env is printed to the console, certain symbols are undefined but I was unable to discern any pattern. Well, it turns out the ones that failed to load are the ones from types.js. But it doesn't make sense why those functions aren't available. It does work, however, if I simply copy the function into the core module! Whaaaat?
	- OK so now it's failing because it is looking for plain old simple `two-fer`, and not the appropriate arity... which are properly defined in the env - I can plainly see that. Why would it be doing that? The lookup logic is built into the interpreter. Let's see what we can debug.
	- It correctly outputs `fn has no docstring and is multi-arity`
	- Could there be a bug in my calling logic? If so, it should also fail in the editor...
	- Aha! It does fail!
	- I see what the problem is. The logic is incomplete.
		- It first checks if there is a variadic arity defined
		- if there is, then check if there's a fixed arity that matches
		- otherwise we call the variadic function
		- but... we then need to recheck if there's a fixed-multiarity! Derp
		- Got it!
	- Cool, so now I've completed the workflow that I was aiming for when I started this Exercism Express thing in the first place. I wanted a way to help guide the process of working towards the goal of being able to use it for solving exercises as if it is Clojure. And I can't think of any better way to test the thing, either. All the example solutions are idiomatic Clojure, using pretty much, the most commonly used functions. Match made in heaven.
- So now I've got a clear development trajectory, but I haven't chosen a logical order to work through the exercises. I did two-fer and hello world, and right now I'm working on diamond, for no particular reason except that it's at the top of the json file. Why is that? I thing it's just the order that java.io listed the directories!
- you know what I could do... process the config.json and sort them by difficulty. Sounds like a plan!
- I can just loop through each integer from 1 to 10 and list the exercises that match each difficulty. simple.
- Cool, so that's done. There's 4 exercises at level 1:
	- [ 'two_fer', 'armstrong_numbers', 'hello_world', 'reverse_string' ]
	- Ha. Armstrong numbers should not be in that. In my PR I changed it to 4, which seems right.
	- I'll take a little detour and come up with a way to compute them objectively.
	- If we make a sliding scale between the highest and lowest completion rate, and place them on the curve.
	- the lowest is go counting, at 35%. Highest is 93%.
	- so if we consider go counting a 10, we can subtract 35 from 93 and we get 58.
	- Anagram is 93, which is 0.
	- Here's my formula:
	- ```clojure
	  (defn difficulty [rate]
	    (int (/ (- 93 rate) 5.5)))
	  ```
	- Let's do it!
	- The formula puts Armstrong Numbers at 5. Excellent!
	- reverse-string is failing and look at this:
	- ```clojure
	  (defn reverse-string
	    ([word] (s/reverse word)))
	  ```
	- Who did that? I mean, there shouldn't be a problem with it. But it seems to have revealed a deeper bug: the function being defined is `reverse-string-arity-0`, but it's an arity 1... and the function being called is... plain, simple `reverse-string`. What's the deal?
	- And when I change it to the function as it would commonly be... it goes into an infinite loop!
	- An additional side problem has also emerged. It uses `s/reverse`, from the `clojure.string` namespace, which reverses a string. Eventually I can fix this by actually parsing the ns requires and doing real namespaces. That won't even be that hard. But one thing at a time. Why tf would it save a 1-arity as a 0-arity? Do we not go by the length of the arglist? That would be pretty dumb if it was by index, i.e. in order of fn bodies. I hope I didn't do that.
	- omg I did
	- I didn't mean to though... I had
	- ```js
	  fnName = types._symbol(a1 + "-arity-" + i)
	  ```
	- instead of
	- ```js
	  fnName = types._symbol(a1 + "-arity-" + args[i].length)
	  ```
	- I won't tell anyone if u don't
	- wait, that's not even right
	- it's `fnName = types._symbol(a1 + "-arity-" + args.length)`
	- ok so now the function is being assigned correctly.
	- But it's not looking for the correct function.
	- Wait... only the final test is messing up
	- ```clojure
	  (deftest long-string-test
	    (let [s (reduce str "" (repeat 1000 "overflow?"))
	          rs (reduce str "" (repeat 1000 "?wolfrevo"))]
	      (is (= rs (reverse-string/reverse-string s)))))
	  ```
	- I don't see what the problem is.
	- whoa... it works if I remove the let, like this:
	- ```clojure
	  (deftest long-string-test
	      (is (= (reduce str "" (repeat 1000 "?wolfrevo")) 
	            (reverse-string/reverse-string 
	              (reduce str "" (repeat 1000 "overflow?"))))))
	  ```
	- That's an awfully weird bug.
	- Now, when I fix the last test, the empty string test is failing, even though it evaluates fine in the editor. Kind of confused here on this one.
	- It gets weirder. When I comment out the one failing test, the next one fails. How fuuuuunnn
	- So, there's definitely something funny going on. Something about the test runner logic or something, because the code is all fine
	- Hmm, actually the deftests variable shows *every* test failing in reverse string
	- I might need to import the regular eval function so we can execute each one in its own env. I can see now that the tests are not being cleared like they should.
	- omg I did it! that was the whole problem. I didn't even change the env though, all I did was add my `cleartests()` function which I forgot I wrote
	- um... now the tests are passing, but you can clearly see *failing* tests in the deftests report... I think maybe I should rest from this. It's getting really fuzzy.
	- I've been working on this for like... over 24 hours or some shit. It's 1:30 PM
- # Testing bug, continued
	- So, for some reason the reverse-string test results are showing false, even though the fails vector is empty, so something is not working properly.
	- So I'm going to go through the system from the bottom up and see if I can make it better.
	- I'm going to move all the testing logic into the test file, and start a fresh env like it should.
	- I don't know what's up with the testing workflow. I had to copy over the entire types module into core because *it refuses to import them*!
	- Now I have all the functions available in the test env except for read string, for which I need to copy over the reader stuff too. What a confusing mess! Why would it not be able to import them?
	- It might be an issue with paths... that might explain it. But we're importing from `"../src/interpreter"` in the test module, which imports from everything else, including core. So why wouldn't the same functions from types be available to core as they're available to the interpreter, and why is it only broken in the tests? Plus that means I can trusts the tests less because they're using different code copied from another place! But when I tried to actually eliminate the types module, the entire app broke. This sucks.
	- It seems that EVAL is not working.
- # Webdriver tests
	- I'm kind of desperately squirming, nothing seems to work. What if I try doing the browser tests like in the lang-clojure-eval project?
	- I got the testing browser window opening up now... but I'm getting a strange error: `ReferenceError: Cannot access '__vi_esm_0__' before initialization`
	- I'm immediately stuck. Maybe I'll start with my working template and try to switch out the interpreter?
	- Or, just not worry about testing if it's causing a whole series of headaches?
	- Yep.... blah. I hate this shit.
	- Maybe I could build a test feature into the app itself? That might just be the best idea. Because... the application *is a test runner*
	- It works! What a great idea!!!!
	- Now I can focus on actually building the app
- Let's go on to a fresh page, [[Clojure interpreter notes page 3]]