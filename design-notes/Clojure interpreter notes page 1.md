- ```clojure
  (def s "my string")
  
  (defn my-fn [s]
    (str "here is " s))
  
  (my-fn s)
  ```
	- `(map inc [1 2 3])` already works, because I added inc!
- # Demo
  collapsed:: true
	- I made a vite demo app, it works locally but fails in prod...
	- the preview works but errors, but the deployed version won't load at all. like... even the favicon 404s. It's like... reading the wrong index.html...
	- fartballs. It's the end of the day and it would have been really fucking amazing to get it launched. I'm confused and eepy so maybe I'll figure it out in the morning
	- omg, I actually figured out the bigger problem of it not deploying at all. I was using the stock static deployment workflow which was different from the one in the vite docs.
	- So now I just have the weirder issue of it not working in prod. The local vite preview has the same issue.
	- I found the difference! In dev mode, typeof module is undefined. But when built, it's an object!
	- when I print out the `env`, it is an empty map in prod.
	- I don't know how to debug the env because it uses constructors which make no sense. I'm going to try to do it with regular objects or something.
	- Seems to be coming along well. But right now, the env is coming up undefined.
	- Ah, the problem seems to be that the env needs to be passed to the eval-region functions. But does it? I don't think so. The job of that is just to send the correct form to be evaluated.
	- It actually works! wow, I might not be as dumb as I thought.
- # Core library
  collapsed:: true
	- What's broken now? I know that macros don't work yet, but I bet I can find something even before that. For example, hash maps are still broken. Let's go through the [demo app](http://kanaka.github.io/mal/) and see what works and what doesn't:
	- maps                      no
	- let                         no
	- list                          yes
	- vectors                   yes
	- scalars                   yes
	- anonymous fns     no?    `((fn [n] (+ n 2)) 1) => "12"`
		- which is weird, because addition works by itself
	- if                             yes
	- cond                       no, macro
	- comparisons/bools yes
	- predicates               yes
	- map, apply              yes
	- list, vector, hash-map  yes
	- conj, cons               yes
	- first, rest, nth         yes
	- last                           no
	- atoms                     yes
	- js interop                yes
	- Damn. this is pretty good! The first thing I should try to fix is hashmaps.
	- The `hash-map` function works:
		- ```clojure
		  (hash-map :a 1 :b 2)
		   => {:a 1 :b 2}
		  ```
	- You can even save it using def and call it. What does not work is evaluating a hashmap itself, i.e. `{:a 1 :b 2}`. We get `ReferenceError: k is not defined`.
	- The AST: `{ʞa: 1, ʞb: 2}` what in the world is that? that character is not in the code anywhere, only the bundled output.
	- Even `get` works on maps, as long as it was created with `hash-map`. But the literal syntax fails.
	- The eval-cell command doesn't seem to work right, it gives the value of the first form, not the second.
	- It recognizes the literal hash-map correctly.
	- Fixed it! Wow! it's because this was missing the word `const`:
	- ```js
	  for (const k in ast) {
	        console.log("k:", k)
	        new_hm[k] = EVAL(ast[k], env);
	      }
	  ```
	- well there we have it. I'm not an idiot.
	- the `let` thing seems to be the biggest issue now.
	- the let_env might not be right. It correctly creates a new scope with the let variables defined in it. But the outer env contains 3 keys instead of 2, one of them being current_env which shouldn't be there. The data is correct though.
	- Omg, it works!
- # Macros... or not
  collapsed:: true
	- I got macros working... I think. But cond doesn't work as expected.
	- This works in the mal demo:
	- ```clojure
	  (cond
	    (< 1 0) "negative"
	    (> 1 0) "positive"
	    :else "zero")
	  ;;=> "positive"
	  ```
	- but in mine, it returns `odd number of forms to cond`. So that would seem that the macros work in general, but there's a bug somewhere else...
	- this is the cond macro:
	- ```clojure
	  (defmacro cond 
	                (fn (& xs) 
	                  (if (> (count xs) 0) 
	                      (list 'if (first xs) 
	                                (if (> (count xs) 1) 
	                                    (nth xs 1) 
	                                    (throw \"odd number of forms to cond\")) 
	                                (cons 'cond (rest (rest xs)))))))
	  ```
	- could we do defn?
	- yeah, I added `or` which is a macro that requires gensym. so I added that too. but the output of `or` is fucky. So the macros are correct, just something else is messed up.
	- hmm... I still hadn't enabled macroexpansion so that may be why. but when I do it says that `mac.apply is not a function`.
	- I enabled the macroexpansion step in the `apply list` part of the interpreter, but commented out the function body of macroexpand. It actually doesn't make it past the *check* if it's a macro call, because of a problem in the `findKeyInEnv` function. It's recursive, and was stack overflowing because it wasn't finding the symbol. There's something inherently wrong with the env stuff, so I'd better start by confirming what I can.
- ## Mal workshop - environment
  collapsed:: true
	- this is part 2 of the workshop, and he gets into the environment and everything. https://www.youtube.com/watch?v=X5OQBMGpaTU
	- He talks about how the environment is a nested object with a pointer to its parent, or `outer`, and you look up the symbol at each level until you get to the top. timestamp: 30:20
- ## Debugging functions
  collapsed:: true
	- I fixed `evalCell` by wrapping the result in a `do`.
	- `do` evaluates `ast.slice(1, -1)`. So it skips the first one, the actual `do`.
	- `let` is working perfectly. I thought it was doing it wrong but I think not!
	- ok so this is weird:
	- ```clojure
	  ((fn [x] x) 5)
	   => (5)
	  ```
	- this might help explain this:
	- ```clojure
	  ((fn [n] (+ n 2)) 1)
	   => "12"
	  ```
	- The definition for `fn`:
	- ```js
	  case "fn":
	    return types._function(EVAL, a2, env, a1);
	  ```
	- Trying to figure out what `_clone` does. It's used in 4 places:
	- 1. `defmacro`
	  2. `assoc`
	  3. `dissoc`
	  4. `with_meta`
	- assoc/dissoc work.
	- As we can see, the anonymous function always puts the result in a list:
	- ```clojure
	  ((fn [x]	 x) '(1 2 3))
	   => ((1 2 3))
	  ```
		- what is `__gen_env__`?
		- uhm...
		- ```clojure
		  (+ 1 2 3) => 3 
		  ```
		- oh, that makes sense. `+` only takes 2 args, duh. I could fix it by like applying sum or something.
		- The eval function is recursive, it's in a while loop. If it makes it to the end without returning, it runs again. This happens in the case of lambdas defined with `fn`. It sets its scope
		- At the time that the symbol x is looked up in the final step of the evaluation, it's *already in an array*. Which means we have to go back to when it was defined.
		- How does this work, really?
		- ```clojure
		  export function _function(Eval, ast, env, params) {
		      const fn = function() {
		          return Eval(ast, bindExprs(env, params, arguments))
		      }
		      fn.__meta__ = null;
		      fn.__ast__ = ast;
		      fn.__gen_env__ = function(args) {
		          return bindExprs(env, params, arguments)
		      }
		      fn._ismacro_ = false;
		      return fn;
		  }
		  ```
		- It's passed
		- 1. the eval function itself
		  2. `a2`, the 3rd element of the ast being evaluated. I'm guessing that is the function body? `fn`, vector, arg, body? Seems plausible... `a2` is the ast,
		  3. and `a1` is passed as `params`. Yes! that makes sense!
		- It binds the names in the environment with `bindExprs(env, params, arguments)`
		- omg, I figured it out. I accidently was passing `arguments` instead of `args` above. You can see the mistake if you stare at it long enough which I just did, while comparing to the original. Somehow I messed it up while fucking with it.
		- Now this works:
		- ```clojure
		  ((fn [n] (+ n 2)) 1)
		   => 3
		  ```
- # Fuck macros
  collapsed:: true
	- So now it's just macros I'm missing!
	- Here is how `cond` should work:
	- ```clojure
	  (cond  
	    (< 1 0) "negative" 
	    (> 1 0) "positive" 
	    :else "zero")
	  ;;=> "positive"
	  ```
	- I'm having trouble with it. It's not making sense. I could skip it for now... and I can just implement more functions as special forms.
- # Defn
  collapsed:: true
	- That's fine with me actually. I don't need macros to finish the interpreter by making it more Clojure compatible. I'll try to make `defn`.
	- omg ![image.png](../assets/image_1689591065047_0.png)
	- This is the special form:
	- ```js
	  case "defn":
	     const fn = types._function(EVAL, a3, env, a2);
	     return _env.addToEnv(env, a1, fn)
	  ```
	- Cool, I guess that's enough for today, it's 4am, everything is checked in, demo is live, and macros are *off limits*...
	- ok it's noon the next day, just got up. I don't know *why* the function above works, because I just tried another which doesn't:
	- ```clojure
	  (defn mr [n]
	    (map inc (range n)))
	  
	  (mr 5) => 
	  Error: f.apply is not a function 
	  ```
	- And yes, this still works (I refreshed the page)
	- ```clojure
	  (defn yo [n]
	    (str "yo " n))
	  	
	  (yo "dawg")
	   => "yo dawg"
	  ```
	- So what's the difference between these 2?
	- OMG. It's because we don't have `range` yet!!!!!!!!!!!!!!!!!!!!
- # Range
  collapsed:: true
	- now we do! it's pretty dumb:
	- ```js
	  function range(start, end) {
	      if (!end) {
	          return range(0, start)
	      }
	      var ans = [];
	      for (let i = start; i <= end; i++) {
	          ans.push(i);
	      }
	      return ans;
	  }
	  ```
- # Testing with Exercism
  collapsed:: true
	- Made a dummy function so that using `ns` won't break it. Now we can actually use it for exercism exercises, like hello world, and the lasagna exercise:
	- ```clojure
	  (ns lucians-luscious-lasagna)
	  
	  (def expected-time 40)
	  
	  (defn remaining-time [actual-time]
	    (- expected-time actual-time))
	  
	  (defn prep-time [num-layers]
	    (* num-layers 2))
	  
	  (defn total-time [num-layers actual-time]
	    (+ (prep-time num-layers) actual-time))
	  
	  (total-time 4 8)
	   => 16
	  ```
- # Threading macros
  collapsed:: true
	- The next would be the list exercise, which as we know is best solved using a threading macro. Seems like that might be hard. Let's try!
	- First of all, the exercise works using normal calls:
	- ```clojure
	  (ns tracks-on-tracks-on-tracks)
	  
	  (defn new-list [] '())
	  
	  (defn add-language [lang-list lang] 
	    (conj lang-list lang))
	  
	  (defn first-language [lang-list] 
	    (first lang-list))
	  
	  (defn remove-language [lang-list] 
	    (rest lang-list))
	  
	  (defn count-languages [lang-list]
	    (count lang-list))
	  
	  (defn learning-list [] 
	    (count-languages 
	      (add-language 
	        (add-language 
	          (remove-language 
	            (add-language 
	              (add-language (new-list) "Clojure") 
	              "Lisp")) 
	          "Java") 
	        "Javascript")))
	  
	  (learning-list)
	   => 3
	  ```
	- Here is the definition of Clojure's thread-first macro:
	- ```clojure
	  (defmacro ->  [x & forms]
	    (loop [x x, forms forms]
	      (if forms
	        (let [form (first forms)
	              threaded (if (seq? form)
	                         (with-meta `(~(first form) ~x ~@(next form)) (meta form))
	                         (list form x))]
	          (recur threaded (next forms)))
	        x)))
	  ```
	- I wonder if I should try making loop/recur first? that actually seems harder. Wait, don't we supposedly have TCO already? If so, could we just replace `recur` with the regular function? I also don't understand why we need to use metadata.
	- I have a version that works with a single list:
	- ```clojure
	  (-> "hello"
	      (str " kitty"))
	   => "hello kitty"
	  ```
	- code:
	- ```js
	  case "->":
	     const form = a1
	     const last = ast.slice(2)[0].slice(1)[0]
	     return EVAL(ast.slice(2)[0].slice(0, 1).concat(form).concat(last), env)
	  ```
	- What we need to do is build an array out of the items in the ast, make them lists, and interleave them
	- This code outputs our lists by putting the forms that are not lists into lists
	- ```js
	  case "->":
	          const form = a1
	          const rest = ast.slice(2)
	          let lists = []
	          for (let i = 0; i < rest.length; i++) {
	            if (types._list_Q(rest[i])) {
	              lists.push(rest[i])
	            } else {
	              lists.push([rest[i]])
	            }
	          }
	          return lists
	  ```
	- output:
	- ```clojure
	  (learning-list)
	   => ((add-language "Clojure") (add-language "Lisp") (remove-language) (add-language "Java") (add-language "JavaScript") (count-languages))
	  ```
	- alright I think I got it! I put a shit load of comments in the code.
	- ```js
	  // Let's make a function that will just thread one form into the other 
	  // It threads the *form* *into* the *expr*.
	  // `expr` must be a list.
	  // Example:
	  // `expr` -> (add-language "Clojure")
	  // `form` -> (new-list)
	  // output: (add-language (new-list) "Clojure")
	  // But it needs to handle cases where the expr is a list of 1.
	  function threadFirst(form, expr) {
	    let l = expr.slice(0, 1)
	    let r = expr.slice(1)[0]
	    l.push(form)
	    if (r) {
	      l.push(r)
	    }
	    return l
	  }
	  
	  function _EVAL(ast, env) {
	    ...
	    case "->":
	          // First element in the AST, a0, is the actual thread-first operator (`->`)
	          // so a1 is the first form to be threaded into the following exprs
	          const first = a1
	          // Make a new list of just the forms to be *threaded*,
	          // i.e. the ones that have forms threaded *into* them.
	          // so we slice it at 2
	          const rest = ast.slice(2)
	          let lists = []
	          // make each form to be threaded into a list
	          // if it is not a list already
	          for (let i = 0; i < rest.length; i++) {
	            if (types._list_Q(rest[i])) {
	              lists.push(rest[i])
	            } else {
	              lists.push([rest[i]])
	            }
	          }
	          console.log("lists:", lists)
	          let threaded = first
	          console.log(first)
	          for (let i = 0; i < lists.length; i++) {
	            threaded = threadFirst(threaded, lists[i])
	            console.log(threaded)
	          }
	         return EVAL(threaded, env)
	  }
	  ```
	- And thread-last is done!
	- ```clojure
	  (->> "kitty"
	    (str "hello ")
	    (str "say hi to "))
	   => "say hi to hello kitty"
	  ```
- Fogus started a new creation-forum in the server, so I posted this in it
- Implemented `pop`. Works like `conj`, i.e. different behavior on lists/vectors.
- Going through the bird watcher exercise. At the point where we need the anonymous function syntax.
- Also, we need `assoc` to work with vectors... done. Also did `dissoc`.
- # anonymous shorthand syntax
	- I need to implement `#` in the reader as in `@`
	- omg I did it already! That was actually... fn lol
- So, I did that because I'm up to this part of bird watcher:
- ```clojure
  (defn day-without-birds? [birds]
    (boolean (some #(= 0 %) birds)))
  ```
- that was just for the function passed to `some`. So now I need to do that
- Here's the source:
- ```clojure
  (defn some
    "Returns the first logical true value of (pred x) for any x in coll,
    else nil.  One common idiom is to use a set as pred, for example
    this will return :fred if :fred is in the sequence, otherwise nil:
    (some #{:fred} coll)"
    {:added "1.0"
     :static true}
    [pred coll]
      (when-let [s (seq coll)]
        (or (pred (first s)) (recur pred (next s)))))
  ```
- should I try to do recur? Without loop, all I should have to do is replace it with the function being called. Assuming that TCO really works...
- Here is a basic example from clojuredocs
- ```clojure
  (defn compute-across [func elements value]
    (if (empty? elements)
      value
      (recur func (rest elements) (func value (first elements)))))
  ```
- I'm going to have to check if a function call has a recur in it beforehand, so that it can be swapped ahead of time. Which means I'm going to have to walk the ast to do a search/replace, I can't do it with strings because we need the data structure intact.
- # AST walking
  collapsed:: true
	- I haven't fully grasped how clojure.walk works, so I guess I'm going to need to now! I don't understand how it advances through the subforms.
	- Here are the functions with the docstrings removed for brevity:
	- ```clojure
	  (defn walk [inner outer form]
	    (cond
	      (list? form) (outer (apply list (map inner form)))
	      (instance? clojure.lang.IMapEntry form)
	      (outer (clojure.lang.MapEntry/create (inner (key form)) (inner (val form))))
	      (seq? form) (outer (doall (map inner form)))
	      (instance? clojure.lang.IRecord form)
	      (outer (reduce (fn [r x] (conj r (inner x))) form form))
	      (coll? form) (outer (into (empty form) (map inner form)))
	      :else (outer form)))
	  
	  (defn postwalk [f form]
	    (walk #(postwalk f %) f form))
	  
	  (defn prewalk [f form]
	    (walk (partial prewalk f) identity (f form)))
	  
	  (defn postwalk-demo [form]
	    (postwalk (fn [x] (print "Walked: ") (prn x) x) form))
	  
	  (defn prewalk-demo [form]
	    (prewalk (fn [x] (print "Walked: ") (prn x) x) form))
	  
	  (postwalk-demo [1 2 [3 4]])
	  ```
	- I'm beginning to understand... it uses map to call the "inner" function on each element.
	- Here's what I have so far:
	- ```js
	  function walk(inner, outer, form) {
	    if (types._list_Q(form)) {
	      return outer(form.map(inner))
	    }
	    if (types._vector_Q(form)) {
	      let v = outer(form.map(inner))
	      v.__isvector__ = true;
	      return v
	    }
	    if (types._hash_map_Q(form)) {
	      
	    }
	  }
	  ```
	- So what do we do for hashmaps? In Clojure it's handled by the `coll?` bbranch:
	- ```clojure
	  (into (empty form) (map inner form))
	  ```
	- This relies on the fact that we can map on a map, lol
	- We have `seq`, but it doesn't work on maps. I'll have to add that.
	- Here's my final implementation:
	- ```js
	  function walk(inner, outer, form) {
	    if (types._list_Q(form)) {
	      return outer(form.map(inner))
	    } else if (types._vector_Q(form)) {
	      let v = outer(form.map(inner))
	      v.__isvector__ = true;
	      return v
	    } else if (form.__mapEntry__) {
	      const k = inner(form[0])
	      const v = inner(form[1])
	      let mapEntry = [k, v]
	      mapEntry.__mapEntry__ = true
	      return outer(mapEntry)
	    } else if (types._hash_map_Q(form)) {
	      const entries = seq(form).map(inner)
	      let newMap = {}
	      entries.forEach(mapEntry => {
	        newMap[mapEntry[0]] = mapEntry[1]
	      });
	      return outer(newMap)
	    } else {
	      return outer(form)
	    }
	  }
	  
	  function postwalk(f, form) {
	    return walk(x => postwalk(f, x), f, form)
	  }
	  ```
	- Implemented `recur` in `defn` forms:
	- ```js
	  let swapRecur = types.postwalk(x => {
	        if (x.value == types._symbol("recur")) {
	           return types._symbol(ast[1].value)
	        } else {
	            return x
	        }
	        return x
	    }, ast)
	  
	  ...
	  
	  case "defn":
	          const fn = types._function(EVAL, swapRecur[3], env, swapRecur[2]);
	          _env.addToEnv(env, swapRecur[1], fn)
	          return "#'" + namespace + "/" + swapRecur[1]
	  ```
	- Now it won't be hard to implement `loop`. What other recur targets are there? According to the [Clojure reference](https://clojure.org/reference/special_forms#recur), it can be any `fn`.
	- But wait... we can't do that, because the function doesn't have a name... uh, could we just replace it with the function? Yes, actually that makes more sense!!!!
	- Actually, I thought of doing it that way, by modifying the `_function` function, but then I realized we couldn't get the name... but that doesn't matter! my first idea was correct. we will just swap recur for the function itself, since it is a first class object.
	- I fucking did it. After sleepies (it's 6:22am!) I can try to implement `loop`.
	- Ok. So I think I'm going to have to move the `recur` logic back into the interpreter, rather than the main function definition because otherwise there's no way to do `loop` that I can think of.
	- So... the main issue that I see is that if we have `recur` as its own special form is, we need a way to refer back to the last "target", whether it be a `fn` definition or a `loop` form. So we'll introduce a special variable that will be reset whenever those points are hit.
	- Having trouble getting it to work. Watching my edited video of the mal workshop, and recording it again through OBS to get a cleaned up version of the audio via the RNNoise plugin.
	- Audio is done. exporting as mp3
	- I edited the video too, to crop it to just the viewscreen :) Rendering it now, it says it will be done in 1:30:00 (90 min)
- # Environment bug?
  collapsed:: true
	- Something seems off. Remember when it was creating a seemingly infinite series of environments? Well, uh, I guess I never solved that problem.
	- I think `bindExprs` is incorrect. Indeed, it sets the outer env every time.
	- yeah, bindExprs is just supposed to return a new env with the data set. So that means it doesn't need the env passed at all.
	- Or is it supposed to have the outer set to the env passed in? I'm confused. Goodnews: my video is done rendering! It's great, now I can actually see what he's doing
- # Testing
	- This will be a big help. I can use vitest. I'll start with the reader tests.
	- Yes! There is a bug in the env and this is what causes it:
	- ```js
	  let env1 = init_env
	  setInEnv(env1, 'a','val_a')
	  setInEnv(env1, 'b','val_b')
	  console.log(getKeyInEnv(env1, 'a'))
	  ```
	- It obviously should output 'val_a', but it outputs 'val_b'!
	- Ok, tests are all passing.
	- ## Exercism
		- I've been implicitly building up features by going through exercism. So what if I hooked up something like the exercism CI script?
		- First of all, we're going to need `load-file` or something.
		- I should build out a test runner right in the demo app. I'm watching Jeremy's presentation on the test runner tooling, and it's frightening me and inspiring me to come up with a better way.
		- `ns` will have another purpose - and I won't have to change anything. The current namespace will decide which exercise is being run. I'll copy over the test suites.
		- You know what? I'm going to make this a new project.
		- It will be able to load stubs as well.
		- Wait... does `slurp` work? OMG IT DOES
		- I'll include the test files anyway, so it will work offline
		- omg... I just spent hours getting a drop-down menu that loads the exercises, but it's done. What I wanted to do was fetch them from the actual source files, but apparently you can't do that so I had to make a json file with all the exercise stubs in it. So I guess now I'm going to have to do something similar for the test suites.
		- Cool, I've got a decent looking app:
		- ![image.png](../assets/image_1689990234748_0.png)
- # Threading *actual* macros
  collapsed:: true
	- Guess what I just found...
	- ```clojure
	  ;; Rewrite x (a a1 a2) .. (b b1 b2) as
	  ;;   (b (.. (a x a1 a2) ..) b1 b2)
	  ;; If anything else than a list is found were `(a a1 a2)` is expected,
	  ;; replace it with a list with one element, so that `-> x a` is
	  ;; equivalent to `-> x (list a)`.
	  (defmacro! ->
	    (fn* (x & xs)
	      (reduce _iter-> x xs)))
	  
	  (def! _iter->
	    (fn* [acc form]
	      (if (list? form)
	        `(~(first form) ~acc ~@(rest form))
	        (list form acc))))
	  
	  ;; Like `->`, but the arguments describe functions that are partially
	  ;; applied with *left* arguments.  The previous result is inserted at
	  ;; the *end* of the new argument list.
	  ;; Rewrite x ((a a1 a2) .. (b b1 b2)) as
	  ;;   (b b1 b2 (.. (a a1 a2 x) ..)).
	  (defmacro! ->>
	    (fn* (x & xs)
	       (reduce _iter->> x xs)))
	  
	  (def! _iter->>
	    (fn* [acc form]
	      (if (list? form)
	        `(~(first form) ~@(rest form) ~acc)
	        (list form acc))))
	  ```
	- Oh, wait, there's reduce? Where?
	- Holy shit, there's a whole standard library I hadn't seen yet.
	- https://github.com/kanaka/mal/blob/master/impls/lib/reducers.mal
	- ```clojure
	  (def! reduce
	    (fn* (f init xs)
	      ;; f      : Accumulator Element -> Accumulator
	      ;; init   : Accumulator
	      ;; xs     : sequence of Elements x1 x2 .. xn
	      ;; return : Accumulator
	      (if (empty? xs)
	        init
	        (reduce f (f init (first xs)) (rest xs)))))
	  ```
	- nice, it's just a regular function. But I've got to get macros working
	- JavaScript is so annoying, I hate it. I can't even figure out how to get keys in an object
- Mal workshop transcript
  collapsed:: true
	- https://www.youtube.com/watch?v=9Jn1VlVZRww
	- after we the the new thing that happens after we read in that line of input uh what we're going to do is do a process called reading or parsing parsing the input so we take that string and we uh basically do some transformation to that and what we get out of that isn't is a data structure an abstract syntax tree um and so that that gets passed to our eval function which just passes it right along to the print function which takes the that data structure the abstract syntax tree and turns it back into a string um and so it doesn't sound so interesting yet but this is a critical functionality that needs to work so here's here's just a quick example of this so we start with a string and the string contains a bunch of characters we tokenize that string and what that means is we split it into the meaningful chunks uh chunks that are going to be meaningful in the list of context so uh that that string is tokenized into five pieces and then we do a process it's called parsing so we look at we go through the tokens and build up the data structure from those tokens and so uh what will happen is this this the tokens the beginning and ending token here are parentheses which indicates a lisp list and inside that parenthetical block are three additional tokens the first token uh we we look at it and it's not a number or a true or false or nil value or any any of the other literals and so we say okay this is a symbol we'll treat it that way uh then we the next two tokens are numbers and we we change them into real numbers in the language we're implementing and we end up with this this data structure an abstract syntax tree which represents a list containing three things all right so I will actually go and we're going to look at the the code for this in Ruby so the the after the string is red we call the reader's read string or read stir function we pass in a string into this function and the first thing that happens is we tokenize it into a list of tokens an array of tokens then we check to see if the um the that token list is empty and what that would mean is that somebody entered a comment on a line um and there was no actual tokens in that that string and then we take the tokens we initialize this uh object reader object with those tokens and then call read form and so the first thing I'm going to look at is tokenize and by the way I forgot to mention the step one most of Step One is already provided for you there's a few few stubs that we're going to implement and and the reason I didn't have not having you implement all of Step One is because while that's interesting and sort of a language parsing context it's not really specific to to lisp this same process uh happens when like you use Json you have a Json string and you use Json parse or you know you you basically this it's basically the same process and in fact if you have a language that has a Json parser and printer you could probably take that code and modify it pretty easily to do this there's some additional things in lisp like symbols that Json doesn't have and nil the nil value but but for the most part uh a Json parser is very close to this so I'm not going to go into detail on that there's a few few sub areas that I left for you to implement when we get to the Hands-On part but so tokenize um there's a little bit of magic here a regex that that tokenizes Mal's syntax it looks long and complicated but mostly it's because it's just using regeck symbols and um it's it's not really as complicated as it looks but I'm not going to go into it because it's not that important for learning lisp but but needless to say when you run this regex against the string what you get out is tokens string tokens so after we have a token list then we initialize an object reader object and all this reader object does is it it you initialize it with illicit tokens it keeps track of that the token list and it has a position a position into that token list that starts off at zero at the first element and it has two functions a peak function which looks at the first at the current token in the list and a next function and all that does is look at the current it Returns the current token and updates the position to the next level so that's that's pretty easy like I said this this part you're not going to have to implement um and then here's the restore again so after we after we create this reader object we pass it to read form which is where the real work happens um so read form takes to the reader it looks at the token at the current position and if it's a parenthesis of the token is a parenthesis then we call the read list function if it's an N parenthesis we shouldn't have encountered that because the read list function should already have swallowed it so we have an error we'll throw an error and stop the the reading process at that point and otherwise if it's not a lisp then a list then we read an atomic value so let me let me look at read Adam next so read Adam again this is Ruby but the the python and JavaScript side of things are going to be pretty similar um so we when we read an atom we we pull a token off off the out of the reader updating the position and then we basically match on that token basically just string match I'm using some more regex here this is just matching all numbers numbers and minus signs this matches something that has a double quote at the beginning and end um and for this for the for the number one we convert it to an integer and for the the where it begins with a double quote we strip off those quotes and uh unescape that it's already a string so we don't have to convert it to a string again but we're just unescaping it so that like if somebody did uh double quote backslash double quote double quote you'd end up with a string that actually has a double code in it um and then if the token matches exactly nil or true or false we return the Ruby equivalent of those values which happened to be named the same in Ruby but in in Python and JavaScript they're a bit different but the equivalents are all there and otherwise we return a symbol if it didn't match any of those then we just return a symbol Ruby has a simple type already built into the language in the python and JavaScript side there's a types file in your implementation directory that has a very simple object definition for symbols that you'll use and that that's already included here for well actually we didn't need to in the Ruby but um so the only other piece I didn't cover is read list and this is this is basically empty right now um so uh read list what it what it will do when you when you have your hands on and implement this is it will uh uh when readlist is called the current token is a beginning parenthesis so you're going to strip that off you're going to go until you find the ending parentheses and for each of the tokens or yeah each of the tokens that you find in that list you're going to call read form on that and accumulate it in this in this list right here and then return it and then it's the equivalent for JavaScript and python also which all which have lists or array Concepts Dynamic and so what we switch to Hands-On that's that's the you'll be filling this in so let me let me just show you here so again this is how I run tasks I'm changing it to um uh to to run the test for step one so if I run those there were 21 tests that pass and eight tests that fail and this should be the same for you right now before you have implemented anything and the eight tests that failed by the way so here's so go all the way to the top of this um it's reading and then printing oh I forgot I'll I'll show the printer in a minute but um it's reading and printing these values and so uh by the way this may seem almost uh trivial and like we're not doing any work because it feels like we've not achieved anything from the read a little string and print a little literal string but what we have accomplished what the result of this step is is basically a lisp syntax Checker because if you have it invalid if you have something invalid it will throw an error but otherwise you know it is it is constructing the data structure in memory and then printing it out um and so the the tests that fail are all the ones related to list because What's Happening Here is we expected to send a plus one two list and get back plus one two list but instead what we got back is an empty list because we haven't actually read read those values um and so those are the eight tests they get printed again at the at the end here the actual the the list of failures um and so when you get when you've implemented the read list uh you should be able to get all the tests to pass and by the way if you don't have the test working you can still look at the test file oops and find some of the you know interesting ones and and copy and paste those into your into your implementation to test to see if it if list reading is working by the way uh one of the things I forgot to mention very important is that you need to implement um oops window here you need to implement read list in such a way that um it's recursive capable so even though I said read list basically goes through all the tokens in the list if it finds another parentheses in that list it's going to um well you're calling read form you're going to call read form in the read list you won't call read atom is is the key because read form has the ability to go back into and read a list again okay so um let me just show the printer real quick so here's the opposite of reading it's it's a much smaller what what printster per stir does is it takes it takes a data structure an object a ruby JavaScript or python object which could be a compound data structure and an optional print readably flag and then it switches on the type it switches on what that object is so if that object is an array um then what it does is it will it will basically map across all the elements of that array calling calling back into per store for that element um and once it has once it's built up that that printed all the elements of that array then it will join that with the space and Surround it with parentheses so what you'll get back is the lisp representation of a list if it's a string uh in the in the case where we don't have print readably set we just return the current string object that we have if print readability is set then we need to reverse what we did before where we escape things so that they could be re-read again but anyways um that's that's not super important but this the person is already implemented for you so I'm just going over so you understand how it works in the Ruby case nil the default weight nil gets printed to Ruby is just empty and so we actually want something to represent that so if if the object we're printing is nil then we'll print out a string for nil I'll skip over Adam for now because that's a different type um and in the final case uh we just we just return Ruby's string representation for that um and that's that's enough for Ruby in in JavaScript and python the true and false and none or nil null value you'll need to have cases for you you will have cases for those in your in your um that specifically cover that but we didn't need to don't need to in Ruby Okay so um to not let's move on to starting to make this a little more real because all we've got so far is just something that that checks to see if our syntax is valid about the syntax uh it doesn't do anything with it so the next step what we're going to do is expression evaluation the first steps of expression evaluation so what we're going to be adding here uh is inside basically inside of the eval block we're gonna have two functions we're going to have a function called eval AST and then um will have it's not actually a separate function it could be but it's not going to be the way we do it is the apply block so um so when eval eval AST is called it's going to be called with that that abstract syntax tree that data structure that was read in by the the read stir function um and what it will do is it'll look at the type of that that uh um that abstraction text tree uh and again uh if if any of you are functional programmers and I I'm sorry that the dynamically typed nature of this but uh we'll try and get through it anyways um so uh yeah we get we get an object that could be this could be several different types um and so email St what it the behavior would be about EST eval EST AST differs depending on what what that type of that abstract syntax tree object is so if it's a symbol uh what eval EST will do is look up in the environment and we'll I'll show you how you create a really basic environment in a second it will look up in the environment and return the value that's in the environment if there is a binding a mapping for it uh if if the AST is a list uh what it will do is it will iterate through each element of the list calling back into eval uh the top level eval function um for each of those elements and then whatever the result of that is is a new list that eval AST returns um we're not going to actually talk about vector and hashmap for this Workshop so after symbol and list then the default case of eval St is just to return return the AST unchanged so for this for step two I'm gonna I'm gonna give a demo the the examples I'm going to do are in JavaScript this time uh basically we have a loop down here that just reads lines of input and prints out the result of calling this read email print function rep function using that string the rep function calls read eval print in order and those those functions read calls the readers read stir function print calls the printer's personal function and eval does nothing at the moment but but this is where for step two this is really the critical point we're going to be starting to modify all right but the first thing um the first thing we're going to need is an environment here so what is an environment an environment is just a hash map or a dictionary um in for step two well it'll be a little bit more complicated later but I shouldn't say complicated it'll be have some more functionality later but for now for step two we're just going to create this this dictionary um and what we're going to put in here is basically just and again uh just just to point out I've switched from the root this is example is no longer Ruby this is Javascript and I'm going to create our basic arithmetic operations here so again this is just this is just a regular hashback or dictionary the keys are for now the keys are strings and uh one for each of the basic arithmetic operations and the value that we're assigning to each of those keys is basically Anonymous function that does takes two numbers and applies that operation okay so when we when we call the um the eval function this is the eval function parameter I'm modifying here uh the email function is going to be called with a result of reading that string into the an AST and the second argument we're going to pass to the eval function is the the Ripple environment that we just defined so let me just show you the Evolve function it takes these two arguments an AST and an environment and so the environment that we pass in when we call it is the rebel envirate we just created Okay so in I'm I'll leave the the finishing of eval for your Hands-On but I'll give a quick so this is not finished eval AST function is going to take the same arguments as eval and the reason I split it out here from the eval function itself is because we're going to use that several times we're going to use that over and over again in the eval function so it's it's it's utility uh that we that I pull out okay so basically you're going to have you're going to have a switch statement here that that tests the type of AST and if um so you're basically going to have what you do if it's a symbol basically as a symbol with the list and then else you just return AST let's do that so if if our St instance is an instance of type symbol oh and one thing I forgot critical for JavaScript we need we need a definition of the symbol type so we're pulling in the types the types module let me just show you that real quick so here's the here's the JavaScript definition of symbol it's pretty trivial it's just a it's just an object that stores a value but we can identify it distinctly from other things so if it's a symbol we're going to do something if uh and JavaScript is a little bit weird when you're trying to determine if something is array all right so I'm going to give um I'm going to give an implementation here so if if the AST is a symbol what we'll do is we'll just basically uh okay so in JavaScript it's an object with a value so basically what we'll do is we'll look up in the environment and return the whatever whatever's in the environment for that symbol if it's a list then we're gonna go over all this in that list and what we'll do is call eval against that and that's it so that's our that's our eval AST utility function and then we're going to use okay so I think I have enough here hooked together to actually show you something running okay so if I just put a symbol that should be found in the environment um what happened here was walk through this so we read a line of input that line happened to have just a plus in a string then we called using that line we called the Redevelopment function the first thing that happens in readable print function is read we read that line that line happens to have just a plus so after the read function we now have an object symbol object that contains the Value Plus uh we take that and call Ebel and we use our our environment what we call eval the environment has these certain things defined so eval in eval we're not doing anything such as passing on to passing on rent to eval AST we'll do a little bit more there in a second and India Valley's AST are our plus symbol here match this first case and we looked it up in in the environment and it happens to have a mapping down here you can see plus has a mapping to the function and so that's what was returned here then print it out this is JavaScript representation of that function obviously not a lisp representation but but that's that's what happened there so if I um if I give it a list you can see that same function there at the beginning of the list again this is this is weird because it's mixing two different types of printing but I think you can see what's happening um you can see that there's a function at the beginning list and then two and three two and three as I mentioned before numbers evaluate to themselves so nothing happened to the two and three they just pass it through directly so so we gave it a list um it evaluated it and returned us a new list um but this this isn't the final functionality step two because in a lisp if you get a list uh it should it should actually invoke that if the first position is a function all we've done is just return it directly and that's that's the change we need to make to the eval function so uh what I'm gonna what I'm gonna do here is so this will look a little weird at first but I'll explain why um if if the thing we got is not a list in the eval function then what we'll do is return eval AST if it is so if we fall through this this check then that means we are we are handling a list now um and uh what I'll do is actually this is the weird part I'm actually going to call eval AST and capture the results um and the reason the reason you know I could have I could have called the valley Yesterday Once capture the result and then check the um the type of the original uh but this this gets split into more cases later so that's why I'm doing it this way so we've we've followed through to this case and we're capturing the result of running eval AST and we know that's a list at this point because we checked to see um if it's not unless we did the first case so we're it's definitely a list now so uh What uh the only thing we're going to support though for this step is we're going to assume that the um that the first the first element of that list is a function and then what we'll do so we'll pull that function out of that list we'll pull the function out we will slice off that function so we get a new list that's just the arguments and we will call we'll actually call that function Now using those arguments and this by the way this is this is what I mean uh Java Java javascript's way of doing applies a little weird because you need a you need you need this value in the first position but don't that's just incidental complexity um okay so let's see if this even does what I think it should do okay so let's leave that up oops whatever okay so let's just walk through that real quick again when we get to the eval function we've already we've already called read so we have we have a data structure um if it's not a list we just call it eval ASD and return the result of that so the only really the only cases there would be if it's a symbol or if it's something some number or something or a true false nil um because because in this case the the eval AST won't won't call the list mode but then down here it is a list so we call evalist this time and capture the result again this is a little weird but it's the structure that we will have later so then then we pull out the first element we assume that's a function we take that function and we call it using the rest of the the elements that we evaluated so but let's let's go on to step three okay so in step three uh in step two we had just this this single dictionary um that we use to look up our values in uh but we're gonna we're gonna extend that a little bit to have basically a hierarchical dictionary and what that means is you'll have this dictionary hash map and it'll also have a pointer to a parent and when we do an environment lookup uh if we don't find it in the current one and there's a parent we'll go to the parent and look up uh try and find the value in the parent and so on until until we get to the top of the tree and so that's um that's basically a lisp lexical environment it's just a it's just a hash map of symbols to values with pointers to a hierarchy of of the environments so again this python uh there's actually a new file I'm going to Define and I'm going to import a class from that EMV environment file this would be the equivalent in whatever language you're working in but I haven't created this yet so I'm going to create this class here and I'm going to put the put down the stub of this for you guys first so I have the python class environment Constructor uh the environment class will also have a set function a find function a get function all right so um in the in the Constructor we're going to take what we're going to do is basically just create uh we're encapsulating a hash map here or a dictionary so inside this class so I'm going to start with an empty dictionary here that are in internally and the pointer the pointer to our part so that's all we have to do to initialize this to basically be an empty environment that's what that's what it looks like and so what what the SAT function will do is basically create in the data uh create a mapping between key and value find what that will do is uh look in the look in my current data see if there's a mapping if there isn't and we have a parent it will go to the parent and try and find it in the parent and keep going until it doesn't find it or it does find it and and it'll return the environment the self or the environment that it actually found it in and get get what you find to find an environment that has it and then actually return the mapping itself so the difference between find and get is that fine returns an environment that does have a match and get Returns the value of that match in the environment but get can use find okay so I won't I won't Implement that right away but I am going to go over here to step three and uh so my my definition of my Rebel environment here is going to change because we're no longer going to just use a regular python dictionary we're going to use our new hash map or a new our new environment type so um our our our root environment doesn't have any um doesn't have any parent environment so then we're going to there's not much difference between this and the previous um what we previously find we just have this this encapsulating class called environments at this point so one other thing that that set does is return that return that value just makes it more useful to have it as actual function to return something useful so find if key is in uh in our self data return itself else if um if we have a parent then we'll return the result of calling find in the parent and if not if there was if we don't have a value for that okay if we don't have a value for that key and none of our parents do then what we return is um just return none or nil and then in get we'll call fine to try to find that key so what we'll get is an environment so it might be ourself but it might also be one of our parents that that has this value find Returns the first match up the tree if we didn't find an environment that had the match then um technically what we need to do is raise an exception but if we did find it if we did find an environment with that then we'll do we'll return the value that it has in that environment and again just note here that this environment here might not be ourselves it might not be this class it might be one of our parents that matched so it's at this point that I really need to switch to using um symbols here rather than strings but one one thing to note here is in JavaScript you can't use objects in the key and so you have to do it you you'll you'll store strings in your in your dictionary but then when you look it up you'll look it up by the string rather than the actual symbol but for python we can have objects in the in the the key position uh our environment is now class an object um and so we'll call its get method using that symbol what we can do now with this um is is Implement our first special forms so I'll give you the stubs for this and then let you uh you implement as much of this as you can and we'll have our our half hour break before the second half of the of the workshop but let me just let me just stub this out here for you all right so um in our apply section uh we're we're no longer going to automatically eval call eval AST so I wanna I wanna actually capture um the first uh the first element of the list that we got without about without without evaluating anything I just I want to capture the first the the starting the sorry the first position of the list so I'm just pulling it out and now I'm gonna now I'm going to change Behavior depending on what's in that first position if we happen to get here and the list is an empty list um then we just return an empty list actually closure yeah that's you you might think it does and in some in some lisps an empty list does give you an error um it'll in most lisps though it just it evaluates to itself it's a weird it's sort of a weird case um so even in closure an empty list will evaluate to itself if the first position of my list is a death bang the other case we're going to handle in this step if it's a let Star these are the two special forms that we're going to add in this step um so this is what changes this step you know the the uh to to actually have lexical environments the deaf bang case what that'll do is that'll take it takes two arguments uh it's a list so it's a death bang argument one argument two it's going to evaluate argument two and then it's going to take argument one assume it's a symbol and bind the result that it just evaluated in the in the current environment that is an interesting one let me just type out a example let here so there's three let has three so apart from the lead itself there are two arguments um there's a the first argument is a binding list and the second argument is the body you want to evaluate and so what let does let will take odd even pairs from The Binding list and and basically create um create those bindings in an environment and and the difference between let and death by the way is that let creates a new environment and that new environment has a pointer to the previous environment that the light occurs in so uh let created a new environment it it bound a to seven and B to 8 in that new environment and then it evaluated that that last list so it had the value of b as eight I added one to it now the key the key with with the let is that once the lat finishes that environment that new environment it created went away and so we're back we're back into the global the rebel environment um that's that's different distinct from Death where I've now modified my current environment so we mentioned for the for the deaf um one of the things I forgot to mention is that it Returns the value that you define also in our inner current environment I'm going to update one of the or or possibly create a new the new setting I'm going to use the first argument so let me let me just put so in this case the the second element of AST is is that ABC symbol so I'm going to set in the environment ABC to the evaluated value of the second element and then return the result the the return result is just going to be the way we defined set Returns the value that we set uh set it to so this value is going to be set in the environment and returned now in our current environment we have a new binding for ABC that is one two three now if I did one of the key things is that if I do this um the the the updated value of ABC is is the evaluated value it's not that literal list we evaluate that that second argument first before we set it in the environment and that's what that's what this does here that that part right there is evaluating evaluating the the body of the deaf and then we use the first argument unevaluated so here's an example where um we only evaluate part of the list not the whole thing because the def is a special form and that's that's one of the sort of fundamental Natures of special forms is that the special form gets to decide how its arguments are evaluated in what order so the first thing that let does is create a new environment it's parent it's the current environment so then uh what um let does okay so I'm gonna I'm gonna iterate over all the all the values in um all the odd values in the uh in The Binding list so those are those are going to be the symbols that A and B in the in the binding list then I'm going to take our new environment and I'm going to get that get that element that symbol from The Binding list and I'm going to evaluate it in the context of that new environment this is a key a key uh functionality that we need to do here um the uh we're we're building up an environment and we can refer to earlier bindings in that environment as we build up the environment and the final thing we do here is we evaluate the body of the let so the the star a b we evaluate it using that new environment and you'll notice here that that we're not once the let is gone once the let finishes this new environment that we created we're not saving it anywhere so we discard it so it goes away um it's garbage collected in this case so there's a new valve here that is missing very simplest example Works make sure that we're properly evaluating the body of the let here and then also make sure that we're evaluating the uh the basically the right hand side of the bindings the odd that even okay so a was bound to seven e was bound to a plus one and then the the final value of the let is B plus one so that's why we have nine there alrighty let's talk about step four so this is where I think it's really starting to get interesting um not as not harder to implement so to speak but but we we after step four we're beginning to have a real lisp language as a result so what we're going to add to step four is conditional support um and uh function closures and also some some core functions that do side effects all right so as you can see from the red here in the upper left what we add is uh a this new namespace called core we're going to move basically we're going to move our functions we have plus minus star and divide we're explicitly putting those in we're going to move them into a namespace along with some other functions that we Define and I provide those for you already um the in the evaluator we have three new special forms do if and FN star do is a way of combining several different steps together and the the the reason you'd want to do that is because you have side side effects in your steps uh then if if it's kind of self-explanatory we already talked about that in the crash course that's basically our conditional and then FN Stars how we Define new functions new Anonymous functions closures for lambdas since this is Lambda conf the first function we Define is equals for comparing two comparing two things and again these are normal functions not special forms we have four printing functions the first two just return a string the second two actually print out to the console so there they are actually our first two side effect functions that have a side effect so they have a return value of nil but they also have a side effect that they print out something then we have some comparison operators for comparing editors we have some integer operations that we saw before let's just we've copied those in here and we have a couple of new sequence functions so we have a function called list and this what this does is basically just return a list of its arguments and this function list question so this will return true if if if the thing that you you run it invoke it against is actually a list and not something else we have a function called count that will count the number of entries in a list and a function called empty that will return true if it's a list but it's empty what we're going to do now is add we're going to add do if and fun star so those those are three new special forms that we're going to do and then down here we'll create the rebel environment but not set anything in it yet a little Loop here over that dictionary in the doofah this is a ruby style iteration over the keys and values of that that dictionary um and then up to this point we don't actually we haven't actually had a way to do this um to to actually have a list returned because when we put a list you know we do this it's going to try and evaluate that as a function invocation and we'll say two you can't really use two as a function basically is what this is saying but we we created a function that takes arguments and what it does is return a list I can then use one of the other functions that we just defined in the core test that really is a list so just by importing that core environment and then basically inserting those those values into your into your Rebel environment you've got you've got access to all those core functions now I'm going to go ahead and just do the the implementation of the do the do special form um just right out the bat so um so in do what I'm going to do is I'm going to I'm going to use our eval AST utility function now um but I'm not gonna I'm not actually going to run it on whole thing because if I did that what it would see is our evaluous function would see a do at the beginning so I want to skip the do to do special form and I want to evaluate the rest of the the elements in that that do special form and then the what I'm going to return from that do special form is the evaluated value of the last thing now why is this interesting to do that that doesn't do anything interesting but one thing we can do is have have something in here that has a side effect yeah so that's that's the do special form for a couple minutes I'm gonna I'm gonna pause so I can answer any questions that may have come up and uh let you guys you can Implement do in your own language or copy the Ruby one if you're doing Ruby um and and also the next um so for the first hand on implement the if the if special form um and so just just real quick recap the way if it's going to work is you're going to evaluate the first argument and then depending on the results of that you're going to evaluate either the second argument or the third argument and return the value of that okay since this is a big step let me uh let me just quickly Implement Implement how I do if in Ruby foreign so what we're going to do is capture the result of evaluating by the way up here I pulled out the the some some short some shorthands for the uh the the the first three arguments of the ASD that we're evaluating if they don't exist they'll just be Nell and Ruby in Python you'll actually have to um this works a little differently but I don't have to do this I could have done ST1 here but just just for shorthand I'm doing doing that so I'm going to evaluate the first argument in our environment and capture the results then if if it evaluates to something to either nil or false and and conveniently in Ruby uh only nil and false are false in Ruby but in other languages you'll have to be more explicit here and test for specifically for nil and false because like in JavaScript there's some other things that are falsy you need to and you don't want those to be false in lisp so if it is false um then I'm going to return the evaluation of oh and I need to add another convenience here I'm going to return the evaluation of the third argument if it's true and of course I'm going to return the valuation of the second argument and there's one other one other bit of functionality that's that's really nice to have here which is that um if if you don't actually have something um in the in the third for a third argument then we just return nil in the false case and what this effectively gives you is sort of a uh you know if if this is true then do this otherwise return nil but it's automatically you don't have to explicitly specify the nil but this is just sort of a convenience convenience thing that I use okay um so that's if let me just uh okay 53. when just an end all right so if true so it was true so we evaluate and return these the second argument if it's false or nil we evaluate and return the 30 argument and what I was giving the example of before is that if you happen that I added that functionality so you happen to admit the false case it'll still return nil and not give you an error an empty string in Mao is a true is a True Value only only false and nil are falsy so anything else um yeah even zero an empty list is true this is and that's you bring up a good question actually in some in some lists the lisps they treat empty list and nil as basically equivalent in Mal enclosure because this model after closure these are not treated the same next thing we'll talk about is functions so FN star is another it's yet another special form and as I mentioned earlier it returns it returns a Lambda an anonymous function um and so uh and the way it works is uh it doesn't the the FN star special form doesn't even evaluate his arguments at all um all it does is basically capture them and store them for later so the the the the first argument of FN star is the parameter list so it captures that list of that a B list without evaluating it it also captures the body of the function the star a b as a lisp as a list and doesn't evaluate it um and so that's that that's all that that special function does at that point there's there's some changes that we actually have to make to the apply section when we run into a a mal-defined function that's defined in Mal this way because so far so far all the functions that we defined have been have been implemented in Ruby or JavaScript or python now we're now we're allowing you to create your own functions um and so when we when we call that we need to do something special and what that is is when we call a function that we've defined a new environment will be created Created just for that function call um and in that environment the parameter list each of the each of the parameters in that list will be bound to the arguments that that function is called with so in this case we have a function that multiplies A and B together and it takes a and b as parameters and what will happen when this function is called as a new environment we created then a will be bound to 7 in that environment B will be bound to eight in that environment and then in that new environment we've created just for the function we'll evaluate the body of the function that we saved from before um now I should point out that when I say saving here saving the values in the special form you can do that you could do that explicitly with an object and save it you can also use a facility in the language that you're you're targeting if it has it you can use a closure functionality not not closure with a J but closure with an S which means you're creating an anonymous function and it's referring to some things in its environment you can create you can use a Anonymous a Lambda like in Ruby you can use a Lambda you can use a closure in Java crafter closure capability in in Python I also do provide in in Python and JavaScript that the types file has a function class that you can use to save save that data off which we will need to use later but for now in all three of them you could just use a closure if you wanted um and then the difference is down here we're actually going to test down here if you use a closure so I do recommend for this step using the closure from from your target environment then you don't actually have to change anything here I don't believe what this does is uh We've added two optional arguments basically to um to our environment Constructor uh a binds list an Expressions list and what we do is we basically iterate iterate over the binds and expressions together and just uh just assign them into this this current environment just add them to our environment so that's just just some functionality some utility functionality that we're going to use for the um for the the definition of our um of our FN star I'm going to use Ruby's Lambda capability to create to create a closure Anonymous function here it's going to be a Lambda function that takes a variable number of arguments what this Ruby Lambda function is going to do is it's going to return the result of evaluating A2 which so we're capturing we're capturing the second argument of the of the function special form that's what we're going to evaluate the body of the function basically and then we're going to create a new environment for this evaluation and its parent is going to be the the current environment when the function was defined not when it's run but when it's defined it's a key distinction so it's going to be its parent environment is going to be where it's defined and that's that's called lexical scope um there are other options but they're more confusing in my opinion and most modern lisps don't do Dynamic binding like that so okay so we're creating a new environment uh the parent environment is the one we Define it in and then we're going to use our new the new functioning functionality we added to our environment class so uh the uh the list of the list of things we're going to bind is the um is the parameter list and what we're going to bind them to are the args in the function special form I'm using Ruby Ruby's closure okay laminate capability and defining a Lambda it's not running to see vowel by the way when I do this we're we're creating a new function a new Ruby function and what that Ruby function does is it will when it's called it will call our eval function using the body of our the original body of our function create a new environment with the parameters bound to the arguments that it's called with and that that happens when it's called you have to keep that in mind that this this eval is not happening when we Define the function but later when we call it and so we're returning a first class function here um and uh it captures the body and the parameters and the environment that it was defined in so we have a new function now that we've defined so when I call that function this code right here is being run and notice that it's it's CL this is called closing over the value A2 and A1 here because when later when we actually run this um those those have gone away in the context that we we that that eval that created this function um has long since exited but closures allow you to keep references to values and only when only when you get rid of all the closures that reference those values would those be cleaned up and garbage funded but this is this is something that's uh fairly common in modern functional uh or programming languages that have functional capabilities so all three languages Ruby Python and JavaScript have the ability to create closures a minor extension that I'm going to do to this is as we're processing our parameter list for our function if we come to an ampersand if we if one of our parameters in our function list happens to be an ampersand then we're going to do something different and what we're going to do is we're going to use the next symbol after the Ampersand and we're going to bind it to the list of arguments that we haven't processed yet now why would we do this so I'm just going to return this function all it's going to do is return the value of B it's parameter B what happened here was um a since it's before the Ampersand just got bound normally to seven then we then the uh we see that we ran into the the Ampersand there um and so what we do is we we take B and we bind B in our environment to all the rest of the arguments that we haven't processed yet and so what this basically with that small change what we've basically accomplished is we now have variable length uh functions that support variable number of arguments by doing this so I could have done this in which case what we've basically just created is an equivalent of our internal list function which just returns all the arguments we could also do what this function is doing is is telling us how many arguments we gave um but yeah so basically this this this minor change is giving us variable length arguments so let me just show you that again so as we're processing through the bindings if we run into an ampersand we take the next symbol and we bind it to the rest of the arguments as a list it is complete but there's some things that are particularly inefficient about it um so one of the things you might have been wondering you know this is a complete language but uh where are our Loops for example um and uh there are there is actually the ability to do uh iteration or recursion already Define what you've finished that for um I didn't show it to you um but uh it's extremely inefficient because every time um you you recursively call the same function again um you're creating a new environment right um and so uh it you know you wouldn't have to process a list that's very long um before you'd run out of memory or you'd run out of Stack your stock would you'd overflow your stack and so um basically all lists have some concept of of tail recursion um tail call optimization and that's it's an optimization um it's not it's not like a core thing that's visible in the language but there's optimization that allows you to do recursion um indefinitely so you can uh if you structure your code right you can you can rehearse indefinitely and that that actually allows you to do loops um uh so let me show you um I'm going to create a function here called sum2 what sum2 does is sums it counts down and sums up every every value as it counts down and so if you look in this function you'll see that I've added added a conditional in the function so the function takes one number uh if if the number that it got was Zero then it just returns zero otherwise what it does is adds that number to another call of sum two with a small value so basically what you'll what will happen here is if you called sum2 with seven some two of seven is called then sum26 is called sum two five four three two one and all those functions are on the stack with each one with their own environment using upstack and so that worked but that didn't so what happened is we we ran out of Ruby stack we we just have too many function calls stacked on top of each other each one with their own environment each one using up stack space um and so we crash and so that that's really limits what we can do and in fact technically step four is not turning Complete because you can't do things like this so up in the environment you can see there's a knot so one of the cool things about um step four is that like I said we have we now have the ability to create our own functions and we can do that even in our implementation itself so and the way we do that is we can just call the rep function directly with a string this is going to define a function called not uh that will reverse the truthiness of its argument not will return true or false depending on the truthiness of its argument and it's just a function that we defined so using the capabilities that we have now in step four so we have conditionals uh so we can test we can test the truthiness of our argument and we have the ability to find new functions and then what we did with that function is we defined it in our base environment this read email print call is happening in the context of our base environment here so it created this it added the symbol knot to our Rebel environment now let's move on to TCO Hotel call optimization so in this you can see the red here is basically uh it's just some arrows there's no new pieces here it's just there's this sort of control flow change um and so there's there's uh four basically four cases in the eval uh in the eval function where we we have to do some modification to change the control flow okay so here's our here's our eval and just to refresh if it's not a list we just do eval EST on it if it is a list we have these special forms that we check for and if there weren't any special forms in the first position then we do basically apply the invocation of the function case all right so TCO basically looks like this I'm not going to change the indenting I'm going to wrap the entirety of our eval code in a infinite Loop right there now because because every every control flow path through here calls a return this actually didn't change the behavior doing that but that's the that's the foundation of our our tail call optimization so um as I mentioned here let star do if and and the apply block the else of our apply are things that are going to change for for Telecom optimization so we're going to make a modification here here in this block and also in this area I'm going to do I'm going to just show the implementation of how we do TCO for the let block so we look at if we look at the definition of eval we have these local variables AST and EMV the the body of our let there's no other there's there's no other code in here that can touch though that EST and the EMV local variables and so what that means so that that's the the body of the is called the tail position of the let because there's nothing else that could happen after that and so what I'm doing here is instead of calling eval so what I had here was this instead of calling eval again so instead of recursing into eval our eval function all I'm doing is I'm setting our I'm updating our AST value for our UL function to be the body of the let I'm updating our environment to be our let environment and then I'm looping so I'm not what I'm doing here is I'm skipping the call to eval so we're not using up any more stack when this happens so a let a let now will not when we when we do a lot if we had lets inside of let's in the title let's the body of that let we wouldn't be using up any more stack we'd have one eval call that's looping around to handle all the leads yeah and I could have done for example I could have done this and then skip this up here but it's a little confusing to have EMV and EnV on the same line I mean this works but I I like the clarity of defining a new environment and then updating updating our our current environment to be this and the reason this works again just just to be clear is that because the body of the let is in the tail position we can't there's no other there's no other thing that could use like use AST and EnV again and so it's safe to update it because nothing else will use it and so we're reusing we're reusing this stack context basically so I did it for let in the Hands-On for you guys is to implement this for do and if and I'll come back and I'll I'll do the uh the function the FN star the function special form so I mean you can you can go on to do that but I'll I'll uh jump in in a couple minutes and finish that off so the reason is because this return happens in front of the eval so there's nothing else that could happen by definition we're we're going to recurse into eval and the return from our current eval and there's no other no other use could happen to the to the ASC and emva values right so that's um that's the that's the the answer that didn't answer the list part but I think it's almost easier it's almost easier to conceive it here what tail position means um now in in uh in the lisp context what it means is that um the uh the the sum two example that I gave is not tail recursive which means that the uh the the recursive call to sum to is not actually in the tail position of the if um so we'll so to get a to get a recursive a tail call recursive proper uh implementation of some too we'll have to modify it slightly to actually have have the recursive call in the teleposition otherwise you know we can't we can't do the TCO we have to keep those AST and EMB values around um and actually do a recursive call so I will do a do is a little bit a little bit odd because the tail position of a do is the very last argument so the modification I'm going to do here is so I'm going to I'm going to evaluate every every argument of the Dew except for the last one that's what this slice is doing here and I don't care about the results and then what I'm going to do here is I'm going to update our AST there's no environment changes that do does so we don't have to worry about the environment you can just stay it'll just stay the same but our our AST will be updated to so so the reason I know I can do this is because we have a return here um after the evaluation and nothing else so I'm going to get rid of the non-tco code so so the difference here is that before I had eval AST evaluating every argument of the do that's the do special form and then returning the evaluated final position in the in the tail call optimization version I'm going to skip the last position and then I'm going to update AST to be the last position and so this accomplishes basically the same thing functionally I'm going to I'm going to loop back into eval and and evaluate the last position of the do but it's a tail tail recursive version of it so that's due so if so the the problem here the problem here is this recursive call to eval that we return so what I'm going to do is uh well basically basically I'm just going to update this and get rid of that so this this is handling both conditions where we we don't have a third argument and in which case we'll return null but if we do have a third argument then we're updating our AST to be the third argument and looping so here's the here's the non-tco version the non-cl tail optimized version where we're recursively calling into eval and then returning the result the tailcol optimized version here instead of evaluating it we're just setting AST to be that to be the um the true position and then looping again to the beginning of eval it's effectively so again just to be clear it's effectively the same functionally as calling eval again but uh what we're instead of actually recursing into eval we know we know we can reuse this this eval context on the stack and so we're updating AST to be the the true position and then looping back so there were no there when we when we hit an F there won't be any additional stack that's consumed when we evaluate the true and false position and the most complicated one here um for tail call optimization is that we have to change how we do our functions and so I'm actually going to capture the value of this the amount of function instead of returning it and and uh JavaScript is kind of convenient here um I can take a function and I can just add arbitrary attributes to it and if I want to um and and I can still use that function and call it but it happens to have some some attributes sort of like metadata almost stored in that function that I can access later and so I'm explicitly I want to be able to use the the captured values the the body and the parameters and the environment I want to be able to use them explicitly later so I'm capturing them when I Define the function here um for the for the python and Java Python and Ruby side um actually Ruby Ruby the python and Ruby implementations in the types file there's a class that's actually defined that you can use for storing this in JavaScript it's a bit easier kind of like I said I can just store along the function okay and then here's here's the problematic um here's the problematic Place uh when I when I actually do a function call um I am I'm if I have if I have a function I've defined in Mal um and then I call it uh that that results in um in in something on the stack and I don't have to do that so so in fact what I can do here is after I've evaluated my arguments to my function to my function call again this is the function call case not the function definition case um I can check to see does this yeah so I'm capturing the function over here does this function have that attribute and if it does if it doesn't I'm going to keep doing the other case I did so there's nothing there's really nothing we can do with core functions um to to save them from stack but for our own functions that we defined which is this case uh we there is something we can do so what I'm going to do here so this is going to be the the case where we're breaking Loop so I'm going to redefine our evals local parameter AST to be the AST value that we captured um from the body I mean it could have probably a better name for this would have been body and then most critically I'm going to basically replicate what I do up here for this function call but instead of doing that function call when we call a mal-defined function rather than just executing it directly calling it directly the closure that it represents um what we're going to do is um we're going to set our AST updated to be the body that we captured before and we're going to create a new environment um but we're going to set our our evals environment to it and so we're basically just collaborating the old environment that we don't need anymore except for this here the reference oops the reference that we captured to the environment is going to be the parent so I will point out here that recursive function calls are creating new environments um but they're not creating stack they're not creating an entry on the stack so they're not going to stack Overflow you are going to use up memory if you call recursively call malfunctions but it's pretty minimal and if you're not actually using those environments anymore they get garbage collected by these but by these three languages anyways um so I'm creating a new a new environment new environment here with the parameters bound to the arguments that we're called with and then we Loop okay I know that was a little bit confusing hopefully hopefully that's hopefully you're getting that a little bit um when I think what it'll really sink in is when um when when you actually are able to implement this from scratching your own all the steps and up to this point um you'll be seeing you'll run into a lot of errors that you'll fix and that'll help you to understand what what's really going on but I'm going to Define an alternate sum that sum to function so um this this alternate function actually takes two arguments an accumulator and the number that we want to sum down this allows us to restructure this code so that the the recursive call the sum2 happens happens exactly in the the if spot so previously the previous version We defined probably would have in JavaScript probably would have stack overflowed at about two thousand but we're not this is not creating any new new calls on the stack every function call every recursive function call is just updating that AST and EMV in your eval function um and and looping around um so it's not it's there's no there's no new function calls on the stack it's one single function call in the stack that's just looping uh doing this doing this function and so you can see that it works here all right so like I said in step four we had a real lisp um but it was a bit inefficient in step five we implemented technical optimization so we have a much more efficient list that can do you know basically arbitrary recursion which allows us to do you know most anything we need to do even with this the small kernel of the lisp [Music] in in Step six there's sort of a grab bag of things that we add to sort of flesh out our lisp um so the first uh if you look up at most of them they're not actually changes to the eval function itself um they're they're around that so there's some new core functions that we added um that that are added they're actually already in there I skipped over them before there's a new function called read string which what that does is that just exposes the reader to our map programs itself so you can actually do a read of a string and get back in a mile type slurp is it's just buying from closure um that basically reads a file into a string um and then there's some there's a new data type with some functions for that the atom data type and this so I one of the things I borrow from closure is immutable by default so the closure types you don't you can't change them um but what you can do is create an atom type and that atom type you can update what type it's referring to and that allows you to do it's it's contained a mutation but it does it does give you the ability to do mutation in the context of an atom at least the reference to the state not the values themselves and it turns out you can do what you need to do that way it limits you a bit but it it reduces a lot of the errors that can happen in your code that was kind of a sales pitch for closure um okay and then the more interesting things are things we're going to Define directly in our our top level environment not as core functions but as as actual uh Mal features um so the first one is the most interesting one is eval so we're going to create a new basically a new top level eval function and what that does is that exposes the basically that exposes the eval function our own eval function to be available to malprograms to call [Music] um and then we have this called earmuff variable with stars on either side called arcv This is environment parameter so if we call our if we call our male interpreter with arguments it'll run it'll it basically allows us to use Mouse as a scripting language and then uh there's a finally a load file a load file is another is another function defined in terms of other Mal components and what that does is basically read in a file read it evaluate it and return the result and so this this is um part of what allows us to have a mouse of scripting language there's a there's an atom related function the atom type related function called um called Swap and it needs to be able to it it doesn't have access it's at a different level than the TCO machinery and so we still have to have a closure the ability to call this this function from that so what we're going to do is uh we'll redefine our repo environment here we're going to add some things to our ruffle environment so the first one we're going to add is eval next one we're going to add is RV which is the arguments that the script was called with like like we Define not I'm going to define a load file function and this this function what we'll do is it takes a single argument which is a file name f and then we'll we'll go from the inside out which is often something you end up doing with with lisps to figure out how they work so from the the yeah from the inside out so we're going to call the slurp function on that file name to read that into a string um we're going to we want to be able to write Mal code that doesn't have to always be wrapped in a do um and so what we're doing here is basically just just wrapping this this file that we just read in an implicit do um and then we're going to call read string this is the this is where the actually yeah read string is the core function that exposes the reader so we're going to take the string we're going to read string on it we perform reach string on it which will return to abstract syntax tree a closure code basically in memory and then we call our eval that we're going to find up here normally normally we fall through here and just do a while loop for our Rebel our readable print Loop but in the case where where we have arguments or calling this on the command line and we have additional arguments what we're going to do is um if we have arguments we're going to treat that first argument as another program to run um and so we're gonna we're gonna call a load file that we just created on that argument and and evaluated it basically um and then we'll exit if we don't have any of our arguments then we'll we'll just run our normal read eval print interactive Loop so like I mentioned eval this this uh symbol in our environment that's going to map to a a review or a I'm sorry a python function python Anonymous function that takes a some other abstract syntax tree some other object and what it's going to do any guesses here so we have an implicit environment that we're using because in the there's not there's not this this type of lisp that that Mal is an implementation of doesn't doesn't directly expose environments to your program itself you can't refer to environments in your mail code so we're just we're implicitly referring to the top level environment for our eval and then for RV all we're going to do here is uh the list of the rest of the Python arguments that are called so this again just just expose the reader Machinery so that we can actually invoke it directly from our mail program and that with that return is not a string but are a list and we have a vowel function which takes any sort of any sort of male type and evaluates it and so you can see here it knows that we had a list we evaluated it now we could have done this also like this where we use our list function to create a list that contains plus two and three and then evaluate that same same behavior there you slurp to read that file into a string okay so this file contains it defines a function Inc for which just adds four to a number and then we call ink4 and print out the result instead of slurping it in right so we call that file actually calls ink four with five and so you can see here that we printed out the result nine the the that file the final thing that it returns is now so you can see the return value nil there of that that file that we executed um so at step six what we've basically accomplished is we've taken we've taken our step four lisp we've we've added tail Co optimization to make an inefficient uh programming language and now we've added just some miscellaneous top level functions to basically expose some of the machinery and just by doing that we now have we have a scripting language Mouse scripting language so this this uh again that's just that's a Mal program um that we're running with our model implementation I'm just going to push on so that you can see self-hosting because I think that's a just an interesting thing to see in itself so unfortunately ran out of time a bit so we're not going to have much more Hands-On I mean feel free to keep iterating on what you're iterating and listening in the background if you want but I'm going to press on to some some minor additions to step six now normally in the full process these editions would happen in Step a um but I've I've back ported it and I've well I'll get to that so um six six um no I want six right nope actually okay let's go back and switch back to Ruby here um let me just look at this here so oops um so uh yeah so step step six plus plus basically is uh I want I want to be able to show you self-hosting so I'm I'm skipping ahead to some of the the things that would normally be in Step a um but uh let me just and since the tests know these things as step ahead I'm going to copy it to step a okay so here's our here's our Ruby step six so here's the Ruby coolant so what I showed you before so here's here's the eval symbol in our Rebel environment with a Lambda around it that just calls the eval here's how we do the list uh the the arguments uh getting the arguments load files basically the same um just slight differences because it's Ruby so now um the uh um so stepping back a bit the the malcode that you've seen seen so far has basically just been one-liners for the most part that I've typed in on the repo or that the very short program that um that I showed you I had a couple of things in it so now what we're going to add to our step six or really step a um is a few more things to allow us to to really run bigger programs bigger Mount programs so um so let me let me actually show you a larger mile program first so I'll just let you look at this for a second so if you're realizing what's what this is you might be feeling a little bit like this is from the movie Inception where you're really not sure what level of reality you're on anymore but so this this is a Mal program and it happens to be a male program that is an implementation of Mao step one um and so any any of the any of the implementation of mouth can run this program um and so uh let me I think there's just uh to run the step one implementation amount in Mal there's just a couple of additions that I need to do here um first one actually I think there's just one for this so read line so this is this is the one place where you need a synchronous version of readline that we were working on before um just the way it's designed it assumes synchronous read line so this is actually uh I'm going to leverage Ruby's built-in read line functionality here the the argument to read line is a prompt I honestly don't remember what the true value to the read line Ruby Redline is but I think it's needed um you know what I think that's all I need so I'm going to run oops the step a make sure that still works and uh yeah cool nice so right so just again because this is a bit confusing I'm running Ruby Ruby is running our our current implementation of Mal uh we're using the scripting functionality we just implemented to load up the step one implementation of Mal in Mal um and so this prompt that you're seeing here is actually being pro it's different than the other prompt it's Mal user because that prompt is coming from the male implementation amount not from the Ruby implementation of Mal um now this is step one so you know all it does is syntax check and and print that I can verify that it's doing it's not just printing the string by adding some Extra Spaces because those are those are stripped out by step one um so um let me just let me just add there's only a few more things so that's all we can do right now is step one but there's a few more things that I can do just add to our Ruby implementation there's not much that's needed um so actually most of it's in core new functions so one one thing that we need to expose is just a uh a way of throwing exceptions um okay another thing that lighter steps depend on the later steps implemented and now that is that they depend on is um they uh need to be able to test whether something is a symbol um because the the in step two if you'll recall the eval AST implementation needs to check to see if that thing it got disassembled and so we need to add that functionality to our Ruby implementation to be able to use that from step two of Mali Mel so it's that and final function this is the most interesting one we need a way of exposing some other machinery to be able to actually so in step two we have the supply The BLT invoke also known as apply in the lisp setting and so I actually need that functionality it's not something I can sort of simulate so I'm going to implement apply as a function but it's going to it's going to expose some um of the the functionality we've already implemented basically but all right this is a weird it's weird but basically what apply does apply takes a function and some more arguments and what it does is it runs um you know the best way to show this is It's just uh wrong number of arguments two for three in in of course I did it wrong oh must be in that fly here in just a second here I probably just got yep yep yep okay apply Plus so what the apply function does is the first argument is a function and then it takes arguments to to run that function with um it's sort of a you know stepping back level and allowing you to to use to to Leverage The apply Machinery that's down in there there's another there's another form of of apply which can do um apply always ends in a list the last argument always has to be a list but um you can you can put arguments to your function uh before that list so what it does is basically it takes so if I did um that I could do and you'll see that the list function was basically called with uh the middle arguments concatenated with the list of the arguments in the end but the most common way that applies used really is just you know apply function and a list of things okay um so uh let's see there's a couple more minor things um and this is more this is this is almost ascetic rather than necessary but it does help us keep track of where we are Ruby oops that's right I got that right yeah it should be fine okay and then when we start this up um we're gonna um we're gonna give a nice message here for the user gotta keep track here okay so let's just run that so now when we run arms rotation it gives us just a a quick header that tells us that this is Mal and what underlying implementation we used for it okay and I think I think we should have everything yep so there's step two and let me just show you step two so um because we're skipping over steps seven eight nine and a and most of a um there's a bunch of uh I'm simulating a bunch of things that we would have added but I'm defining them in terms of Mal itself these are super inefficient but they do work they are correct those are just some core functions that I don't have yet so here's as you can see here's the reader the read function in Mount implemented amount um here's the uh eval AST so we check to see is that AST thing that we got a symbol if it is we call environment get and by the way uh let's see environment get is yet another utility routine that I had to Define um it's pretty ugly but you don't need to really care about that and if it's a list then we map across calling the eval for each of those elements here's eval as you can see here um if it's an empty list we just return it it's not an empty list we're using let here to pull out to capture the result of running eval AST [Music] into El I mean hopefully hopefully you're you're seeing in your mind the code that we're writing in one of the other languages here and it is it is fairly similar um uh we'll pull out the first element of that evaluated list we'll get the arguments which are the rest of them and then we'll we'll call that apply that's why I needed to apply here the applied that I implemented Ruby is going to be involved here and we'll call apply the function and the arguments that we just that we just evaluated um so oh by the way I hear just just so you can see this um the the rebel environment that we Define um here is actually a list because we haven't obviously we haven't implemented hash Maps yet that that data type so um we actually have a list that just alternates the symbols and the functions and when this when this is actually evaluated uh we're looking up the plus minus star and slash functions from the Ruby you know at the bottom layer you know these are being basically lifted up into this layer so that was step two let's just go straight to step six all right okay so just like stew on that for a second um so what this is doing is using load file this is the load file implemented here right here um as you can see we Define load file again it's important to keep in mind when I did this oops we're running Ruby we're telling Ruby to run the Ruby implementation amount and then we gave it the the program the mail program that we want to run is itself a implementation of Mal and then we're using that to run yet another Mal program so you want me to do this right yeah yeah let me try one first yep okay let me oops uh yeah I can't remember if this works completely I think it does it's just one of the things because this is interpreted program and there's no like just in time compilation that's optimizing all this you have an exponential slowdown every time you uh you add a layer here and so I don't know if this will this will actually come back in time um but but let me uh let me go over to the full all right so this is the C implementation of Mal um it's compiled running the malnutation amount which is running again the male implementation amount a different you know all the all the memories separate the objects are separate um and I'm using that to call to call this simple now program so there's a bit of a bit of inception going on here but um I think this this is um when I did actually uh for the first time write a self-hosting interpreter um that was pretty cool and I I do I do encourage you to continue this process try try and do this from scratch because the point when you get to this point it's just it's um mind-blowing even you've seen it you know when you have your own code that does this it's uh it's a worthwhile experience and I think you'll I think you'll gain a lot of knowledge about lisp just by doing this um and and doing the whole process and I I mean I apologize that there's a lot of information here and I'm sure most people are gonna have to catch up uh later but uh anyways the um let's see here okay so let me see how much time we got um so I'm just gonna go quick through through some of the other uh steps of Mal we're not going to do any Hands-On for this I'm not really going to show that much code but I just wanna I just want to give you a taste for what these different steps do and again um the guide that you should have on your cheat sheet and um I'll also be posting this uh all this Workshop material online um so you can get access to it for sure um so step seven step seven EDS adds sort of this uh uh we it adds a concept of meta programming um and uh it's it's it's sort of the first step of being able to treat uh Mal code as data and manipulate it as data um so actually I will show some code here um so uh let me go back here to Workshop um no actually I do want to be over here uh let's see let's use the make implementation of of Mile for this this is a full invitation underneath it saw gonna make just it's rather verbose but um it's uh it's real um so quote the quote what quote does is basically it's a way of getting something it's a way of saying don't evaluate this um you're you're short-cirking the evaluator so one of the things I can do here is oops not that oops so if I want to have a literal list I can use the quote special form to tell the evaluator don't evaluate this just give it to me straight okay um now let now the the quote that's pretty trivial it's not only interesting um so what I what I uh quasi quote is really where the power lies um so um I'm going to define a couple things here yeah and uh quasi quote by default does the same behaviors quote except that quasi quote has some special forms that that can be contained in there that have special behavior when you use quasi quote so um I can have unquote here not seven X and uh if if any of you have used powerful string templating before a string templating basically allows you to have a string literal but inside the string literal there's some special symbols that say I want to step out of the string templating access some you know program data and then insert that into the string and that's that's kind of what's Happening Here um so quasi-quote when it encounters the unquote x what it does is it says it steps back and says Okay I want to reactivate the evaluator just for this thing um and then insert it right here and there's so there's another special form a special form that only can happen inside quasi quote unquote and splice unquote this won't be nine um so splice unquote if I just done unquote here I would get I would get y directly um in in that position if I do splice on quote it basically puts it into the into a list that's already there so um so anyways this is this is a the beginning of sort of uh being able to do meta programming where your your program is manipulating programs um so what's that yeah yeah this is this would be one one of the tool set that you'd use for doing self-modifying code but not just self-modifying code um the more common use of meta programming is that uh what we're going to get to next which is um macros and so macros are a a uh sort of what is probably what lisp is often known for um you don't you don't end up creating your own macros all that often um in most in most circumstances and if you do you probably need to step back and ask yourself whether that's really the right approach but um but uh macros are extremely powerful and what what macros allow you to do is basically uh you can write code with code so rather than self-modified code is one way of looking at it but um macros allow you to basically generate uh new code um and another way of looking at a macro is a macro is a way that as a malprogrammer or a lisp programmer I can create new special forms um so and the reason that's important so um let me let me give you a concrete example here um do we want to do any other implementation remember if I have all the dependencies nope I'll just stick with uh make yeah you have awk I didn't do that one actually somebody else um okay uh here we go all right so um I copied this but um this deaf macro is a special form that creates it creates a function but it's a special function it's a special it's a function that's marked as a macro and um you can see the function it has some some it uses classic quote and let me let me show you what happens macro expand is another special what that does is it it looks to see if what you're about to do is a macro and it short circuits the final step of of calling the macro so so when so I defined a macro called unless and basically what it is is it's a it's a it's flipping the if um it's a it's a reverse if um is basically all it is and so um if you look up here into the Quasi quote what you'll see is that um we're doing an if unquote of the predicate um but then we reverse the order of of b and a so we we've again this is this is using both quoting and macros and so if I do I'll get seven but importantly here um unless is a macro that transforms that transforms the code that you give it or the arguments transforms the arguments that you give it somehow and then calls eval on that um and so if you look up to the macro expand what what basically happened is that unless ran switched the two arguments and gave you an if and then when you actually run OS it does the macro expansion and does it an evaluation step um and so it's it's a way of defining it so the reason this is important I was about to get to is that if if I tried to Define unless as a function a normal function what would happen here is that uh it would it would evaluate true evaluate Pern a and evaluate Pern seven so you'd get eight printed out then seven printed out and then finally you'd get nil printed out because it's doing all the maybe a better way to show this would be um so this is this is one of the examples of where I do is useful where we want to have a side effect and return something and so if unless was a normal function we would have had eight printed out even though seven was returned finally by the function by the and last function um we would have gotten because we can't in normal functions we can't control the the when we get the arguments they're already evaluated as a normal function s yeah I mean lazy is one way of looking at it uh it's I I tend to think of it as macros decide how to evaluate arguments um so you could Define macros so that they evaluate all their arguments first and then you know so you can you can make a macro that operates basically like a function but you can choose to do whatever about you could evaluate these things in a different order um you know like you could make a macro that that that changes do so that it evaluates them in the reverse order yeah right facility for something that would allow you to do yeah exactly um so um and this uh I'm not going to get a chance to show you some really interesting examples here but basically um yeah uh well I can or something um or as a macro that that starts evaluating each of its arguments and as soon as it comes to something that's truthy it returns that and it doesn't evaluate any more arguments this is again something you couldn't do with a function with an or function it would try and evaluate the eight um and then you know it would evaluate all these arguments including the eight it would still return seven but if eight if the eight position had a side effect you would see that so um also efficient efficiency here um you know we if if eight was rather a function call that ran for 15 minutes most of the time you probably don't want to do that and that's why you had the org or there so um okay um let's see no it doesn't no it doesn't it doesn't at all um that's just it's convenient um it makes it makes defining macros a lot more uh a lot prettier because otherwise you have a lot of like list like you're constructing this this big list of stuff and um yeah to build the ability to have the quoting and unquoting uh makes building macros a lot easier um and it if you're interested in macros there's a you know this is just touching on the air surface but there's you know uh many many research papers and uh books and and things that you can get into um so exception handling um uh this this just adds a new special form called try catch um and try catch looks like that's good let's do this one oh I haven't built it yet the downside of when you have a compiled language all right um so here's the try catch special form so what it does is it tries to tries to evaluate its first argument um if that if that are if that throws an exception um then it will catch that exception the the implementation amount will catch that exception it will bind that exception to the um the first argument of the catch block and then and then it will evaluate the second argument of the catch block uh in that environment so it creates a new environment for the catch block with the exception Bound in it and so you can um yeah so here I tried to call ABC which doesn't exist that threw an exception um but this this whole this form as a whole didn't actually throw out an exception it caught it it evaluated the catch block printed out the exception and moved on um so and then also in Step nine is a sort of a grab bag of other things other useful core functions and then the final step a is we've already touched on a lot of the things that we added here because I had a short circuited and added added it to step six but um there's there's a few other things here including um oh by the way back back at step nine I should say um the uh um this is a good place to add vectors and hash map support if you haven't done it already in your mouth implementation that's one thing we haven't touched on at all but uh it it makes your code a lot prettier and it's a Well hash maps are a data structure that you definitely want um Step a in addition to the things that we showed one of the things that's added here in Step a is metadata um the the amount imputation of Mal in mouth that I showed you um is is much more verbose than it needs to be um because it doesn't have some of these data types it doesn't have a metadata um so the actual in in the full mail repository if you look at the malnutation it's it's much shorter because it uses things like metadata and hash maps for the environment and that kind of thing so it doesn't have to go into it doesn't have to go through hoops to to do that so um okay um I think that's basically yes the Mona Transformer approach um I'm just curious if there's like things that aren't possible Runway versus the other um I don't know actually I um I'm a monad beginner so I'm hoping to learn uh more about that in some of the the sessions here so unfortunately I can't I I don't have anything useful to to give me a foot on that um um I threw up some of the links again most of these are on the cheat sheet um I don't let's see yeah I don't have a actually I do have a link there's nothing there right yeah but the uh um the Lambda conf uh the GitHub kanaka Lambda comp that'll be where this material lands um yeah that's what I mean this slides these slides that I've been presenting um it's the third the third light there that's where it's going to be they're not there yet I haven't pushed them I was modifying this all the way up till uh to our an hour before this um so I haven't pushed there but I will I will that is where I'll push it um one thing I don't remember when I mentioned this or not I know I did mention it um there's the slack Channel um so if if you want to be working on this uh during the conference or in the week or two afterwards I'm going to be hanging out on that channel I'll answer any questions that you have um also feel free to catch me during the conference and ask questions um show me your broken code and I'll look at it together um in the longer term um the uh the hash Mal channel on IRC for General General discussion of Mal um some of the some of the people who've created my own Temptations hang out there and you're welcome to to join that and discuss there's my Twitter handle and uh I work for biosot.com um and I think we have two more minutes so I'm just going to show some inspirational material these are our languages that don't have implementations in Malia of maling them so if if anything's up here were languages that you're interested in learning and you really wanted to learn deeply I have found that implementing Mal in a new language is a way to learn that language really deeply um and and to be honest it's the most efficient way that I've personally found for learning a new language is to implement something real like this and so some of these would be more challenging than others for sure a lot of the low-hanging fruit has already been implemented but feel free to re-implement any language that's already been implemented I won't guarantee that I'll pull it into the repository but I would be happy to throw a link to alternate implementations of you if you have implementation that's that's complete you know existing language um but yeah so let's see do I have any anything else I think that's I think that's it so I think that's the end of the time for me any other questions that people have here's a it's kind of interesting this is this was made a while ago so I think about 10 implementations are not represented here but this is a if there's no other questions I'll just talk about this um this is a graph that shows uh popularity one measure language popularity not definitive um on the on the x-axis over going over to the right um and then on the uh the y-axis I have a micro Benchmark a few micro benchmarks that I run um and so it's probably pretty hard to read but I can just tell you um like uh down in the very lower left-hand Corners Mount itself oh and the size of the bubble is how many lines of code um the implementation the colors represent the typing discipline the salt the The Fill color um the uh yeah there's lots of information here but um some of the most interesting language in my opinion are the ones that are in the upper left hand quadrant which have good performance but are not popular um so like our python Nim Crystal kotlin um o camel Factor this little bubble right right here is factor which is an interesting language it happens to be one of the smallest implementations it also happens to be the fastest dynamically typed implementation um and uh but it is a stack based concatenative language so um if you're you're up for learning a new a new programming Paradigm if you've never touched that it's uh it's an interesting language there also an implementation I didn't do so um other questions before we break like I said I'll be available and around and on the slack Channel try and answer is as quickly as I can but uh I do have kids and they're here so we'll see we'll see how quickly I'm able to answer but anybody anybody who wants to weigh in and help out and catch me while I'm here at the conference and I'll be happy to answer any questions or um discuss them out so thank you foreign
- Macros are working! The bug is this... the `Symbol` types were changed to `Symbol$1`, which is why it was tripping the checks. So I removed them! Problem solved!
- # Loop
  collapsed:: true
	- `loop` is a macro:
	- ```clojure
	  (defmacro loop
	    "Evaluates the exprs in a lexical context in which the symbols in
	    the binding-forms are bound to their respective init-exprs or parts
	    therein. Acts as a recur target."
	    {:added "1.0", :special-form true, :forms '[(loop [bindings*] exprs*)]}
	    [bindings & body]
	      (assert-args
	        (vector? bindings) "a vector for its binding"
	        (even? (count bindings)) "an even number of forms in binding vector")
	      (let [db (destructure bindings)]
	        (if (= db bindings)
	          `(loop* ~bindings ~@body)
	          (let [vs (take-nth 2 (drop 1 bindings))
	                bs (take-nth 2 bindings)
	                gs (map (fn [b] (if (symbol? b) b (gensym))) bs)
	                bfs (reduce1 (fn [ret [b v g]]
	                              (if (symbol? b)
	                                (conj ret g v)
	                                (conj ret g v b g)))
	                            [] (map vector bs vs gs))]
	            `(let ~bfs
	               (loop* ~(vec (interleave gs gs))
	                 (let ~(vec (interleave bs gs))
	                   ~@body)))))))
	  ```
	- That's the one with destructuring, which is uh, kinda hairy:
		- ```clojure
		  (defn destructure [bindings]
		    (let [bents (partition 2 bindings)
		          pb (fn pb [bvec b v]
		               (let [pvec
		                     (fn [bvec b val]
		                       (let [gvec (gensym "vec__")
		                             gseq (gensym "seq__")
		                             gfirst (gensym "first__")
		                             has-rest (some #{'&} b)]
		                         (loop [ret (let [ret (conj bvec gvec val)]
		                                      (if has-rest
		                                        (conj ret gseq (list `seq gvec))
		                                        ret))
		                                n 0
		                                bs b
		                                seen-rest? false]
		                           (if (seq bs)
		                             (let [firstb (first bs)]
		                               (cond
		                                (= firstb '&) (recur (pb ret (second bs) gseq)
		                                                     n
		                                                     (nnext bs)
		                                                     true)
		                                (= firstb :as) (pb ret (second bs) gvec)
		                                :else (if seen-rest?
		                                        (throw (new Exception "Unsupported binding form, only :as can follow & parameter"))
		                                        (recur (pb (if has-rest
		                                                     (conj ret
		                                                           gfirst `(first ~gseq)
		                                                           gseq `(next ~gseq))
		                                                     ret)
		                                                   firstb
		                                                   (if has-rest
		                                                     gfirst
		                                                     (list `nth gvec n nil)))
		                                               (inc n)
		                                               (next bs)
		                                               seen-rest?))))
		                             ret))))
		                     pmap
		                     (fn [bvec b v]
		                       (let [gmap (gensym "map__")
		                             gmapseq (with-meta gmap {:tag 'clojure.lang.ISeq})
		                             defaults (:or b)]
		                         (loop [ret (-> bvec (conj gmap) (conj v)
		                                        (conj gmap) (conj `(if (seq? ~gmap) (clojure.lang.PersistentHashMap/create (seq ~gmapseq)) ~gmap))
		                                        ((fn [ret]
		                                           (if (:as b)
		                                             (conj ret (:as b) gmap)
		                                             ret))))
		                                bes (let [transforms
		                                            (reduce1
		                                              (fn [transforms mk]
		                                                (if (keyword? mk)
		                                                  (let [mkns (namespace mk)
		                                                        mkn (name mk)]
		                                                    (cond (= mkn "keys") (assoc transforms mk #(keyword (or mkns (namespace %)) (name %)))
		                                                          (= mkn "syms") (assoc transforms mk #(list `quote (symbol (or mkns (namespace %)) (name %))))
		                                                          (= mkn "strs") (assoc transforms mk str)
		                                                          :else transforms))
		                                                  transforms))
		                                              {}
		                                              (keys b))]
		                                      (reduce1
		                                          (fn [bes entry]
		                                            (reduce1 #(assoc %1 %2 ((val entry) %2))
		                                                     (dissoc bes (key entry))
		                                                     ((key entry) bes)))
		                                          (dissoc b :as :or)
		                                          transforms))]
		                           (if (seq bes)
		                             (let [bb (key (first bes))
		                                   bk (val (first bes))
		                                   local (if (instance? clojure.lang.Named bb) (with-meta (symbol nil (name bb)) (meta bb)) bb)
		                                   bv (if (contains? defaults local)
		                                        (list `get gmap bk (defaults local))
		                                        (list `get gmap bk))]
		                               (recur (if (ident? bb)
		                                        (-> ret (conj local bv))
		                                        (pb ret bb bv))
		                                      (next bes)))
		                             ret))))]
		                 (cond
		                  (symbol? b) (-> bvec (conj b) (conj v))
		                  (vector? b) (pvec bvec b v)
		                  (map? b) (pmap bvec b v)
		                  :else (throw (new Exception (str "Unsupported binding form: " b))))))
		          process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
		      (if (every? symbol? (map first bents))
		        bindings
		        (reduce1 process-entry [] bents))))
		  ```
	- We won't be doing that any time soon. But here is the non-destructuring loop:
	- ```clojure
	  (def
	   ^{:macro true
	     :added "1.0"}
	   loop (fn* loop [&form &env & decl] (cons 'loop* decl)))
	  ```
	- Could it be that simple?
- It's almost like a step-debugger: 
  collapsed:: true
	- ![image.png](../assets/image_1689922075876_0.png)
- I found those delicious threading macros in the mal repo, in the root of the `impls` directory. What else is in there?
- So holy shit. Seems like it's finally come together.
- I'm back at that hashmap problem again. what did I end up doing for that? That's right... there was a missing const or something
- A couple of tests are failing in types.test.js again, the env functions... is this the one that magically fixed itself? Well it magically broke again
- See you at [[Exercism express]]
-