- We've got a problem with destructuring:
- ```clojure
  (destructure '[s [[1 2] [3 4]]]) => 
  Error: seq: called on non-sequence 
  ```
- Oh wait... it's because I was still on the branch with the non-working `loop`. Which is kind of a problem, because there *isn't* a branch with a working `loop`!
- # `loop` as function/macro?
	- It seems like it should be completely trivial, rearranged as nested `let` forms:
	- ```clojure
	  (let [s [[1 2] [3 4]] res []]
	    (if (empty? s) res
	      (let [s (rest s) res (conj res (first s))]
	        ...)))
	  ```
- # ~~function~~
	- It seems the reason it can't be a function is that the bindings can't be evaluated in the normal way.
- # Macro
	- I started (barely) with this
	- ```clojure
	  (defmacro loop [bindings & body]
	    (if )
	    `(let* ~bindings ~@body))
	  
	  (defn recur [& forms]
	    forms)
	  ```
	- And then my mind went blank. Then I thought how it would be easier to do something like before, implementing them as special forms.
- omg, it *almost* works... I've just got a small bug somewhere but I think it's very close
- ```js
  case "loop":
  // build map from bindings vector
  let bindings = new Map()
  for (let i = 0; i < a1.length; i += 2) {
      bindings.set(a1[i].value, EVAL(a1[i + 1], env))
  }
  // Walk loop and attach body and bindings map to recur form as metadata
  // If symbol is key in bindings, replace with its value
  const walked = postwalk(x => {
      if (types._symbol_Q(x[0]) && x[0].value === 'recur') {
          x.loopAST = [types._symbol('do')].concat(ast.slice(2))
          x.bindings = bindings
      }
      if (types._symbol_Q(x) && bindings.get(x.value)) {
          return bindings.get(x.value)
      } else {
          return x
      }
  }, ast)
  // wrap loop body in `do` and set as AST
  ast = [types._symbol('do')].concat(walked.slice(2))
  break
  case "recur":
  console.log(ast, PRINT(ast))
  console.log("loopAST:", PRINT(ast.loopAST))
  // update bindings map with evaluated recur forms
  let recurForms = ast.slice(1)
  for (const [key, value] of ast.bindings) {
      ast.bindings.set(key, EVAL(recurForms[0], env))
      recurForms = recurForms.slice(1)
  }
  // replace symbols in loopAST with new binding values
  const recurWalk = postwalk(x => {
      if (types._symbol_Q(x[0]) && x[0].value === 'recur') {
          x.loopAST = [types._symbol('do')].concat(ast.slice(2))
          x.bindings = ast.bindings
      }
      if (types._symbol_Q(x) && ast.bindings.get(x.value)) {
          return ast.bindings.get(x.value)
      } else {
          return x
      }
  }, ast.loopAST)
  console.log("recurWalk:", recurWalk, PRINT(recurWalk))
  ast = recurWalk
  break
  ```
- God, this is confusing. I'll try to talk through it.
- At `loop`, we attach the `loop` body to the `recur` form:
- ```js
  
  ```
- But then on the `recur`, what do we do? Do we have to update the loopAST attached to the `recur`?
- I think we might have to walk it twice... first to replace the locals, and again to attach it to the recur. Where for loop we only had to walk it once. Does that make sense? I can't think for some reason.
- I just can't seem to get it right. It's returning early or something, or maybe the binding values are getting lost... if I put a println in the conditional, it shows it reading `nil`:
- ```clojure
  (loop [s [[1 2] [3 4]] res []]
    (if (empty? s) 
      (do (println s)
        res)
      (recur (rest s) (conj res (first s)))))
   => [[1 2]] 
  ```
- I don't understand. But I got closer than I did the last 2 times...
- It even shows the correct code if I print the form at the end of the `recur` step:
- ```clojure
  (do 
    (if (empty? ([3 4])) 
      (do (println ([3 4])) 
        [[1 2]]) 
      (recur (rest ([3 4])) (conj [[1 2]] (first ([3 4]))))))
  ```
- I feel like I should have picked a different hobby.
- Maybe I'll try the macro idea again? That's 3 times now I've tried the metadata thing and failed, not even including my original naive implementation which only half-worked.
- # `loop` finally working!
- Finally stumbled upon something that works! Took me nearly a week of struggling every day. I made a total of... 5 or 6 different branches... Where is even my notes from yesterday? I made so many pages I lost track. Ah... it's in [[loop-stack]]. But it trails off at the end so I never recorded when I actually got it working.
- emccue suggested to just use a javascript `while` loop, which I never considered because it would involve parsing out the entire loop form and dealing with it at once. If I continue to have problems and find I need to go to the drawing board again, that might be something to consider. But I seem to have gotten it! 81 tests are currently passing. But I had to disable destructuring because it's obviously not ready yet. I think I need to have a better way to tell when the bindings do not require destructuring so that it will pass it through unchanged. IIRC Clojure checks whether each left-hand binding is a symbol. Seems simple enough... I wonder what the problem is.
- Yes, it is this:
- ```clojure
  (if (every? symbol? (map first bents))
        bindings
       ...)
  ```
- Oh yes, I was actually hoping that fixing `loop` would fix my issue with `for`. I wonder if it did. Actually I'm kind of terrified by this whole thing right now. I seem to have bitten off too much. But hey... 81 tests are passing! The language is Turing complete, if only you know how to avoid all the rough edges.
- [[Clojure interpreter notes page 11]]