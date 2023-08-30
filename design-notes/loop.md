- This is what needs to work:
- ```clojure
  (defn loop1 [coll]
    (loop [s coll res []]
      (if (empty? s) res
        (recur (rest s) (conj res (first s))))))
  
  (defn loop2 [colls]
    (loop [s colls res []]
      (if (empty? s) res
        (recur (rest s) (conj res (loop1 (first s)))))))
  
  (loop2 [[1 2] [3 4]])
  => [[1 2] [3 4]]
  ```
- We will track the execution path to illuminate what is going on.
- I inserted a break point at recur so I can see the execution path in the debug console, otherwise it runs off the tracks(infinite loop).
- I got pretty far yesterday, then realized (because it didn't work) that we need to add a check to see if the loop is already in progress.
- ## Steps (calling `(loop2 [[1 2] [3 4]])`):
	- ### `loop2` (outer)
		- 1. Set `'s__0'` to `[[1 2] [3 4]]`
		  2. Set `'res__1'` to `[]`
	- ### `recur` - `(recur (rest s) (conj res (loop1 (first s))`
		- 4. re-bind `s__0` to `([3 4])` (`(rest s)`)
	- ### `loop1` (inner)
		- 5. set `s__2` to `[1 2]`
		  6. set res__3 to []
	- ### `recur` - `(recur (rest s) (conj res (first s)))`
		- 7. re-bind `s__2` to `(2)`
		  8. re-bind `res__3` to `[1]`
- Wait a minute... I think there's a fundamental problem with how we're storing the metadata... when we add a property to the `recur` symbol... it adds it to the symbol itself, not the specific call to it!!!!!
- # Back to drawing board...
	- So how do I do this?
	- When a loop is initialized, we can store a `thread` designator or something.
	- Maybe this is a good use for a stack?
	- Is it possible to store metadata on the AST itself? I mean, it is an array... but will it persist when it comes around again? It works with vectors and functions...
	- ## What if...
		- We actually replace the variables in the code itself? We're already walking it and performing a search/replace.
		- I started a new branch for this, so I can begin with a clean slate.
		- We will turn the bindings vector into a map, and look up each item in the ast as we walk.
		- The first pass does the right thing:
		- ```clojure
		  (defn loop1 [coll]
		    (loop [s coll res []]
		      (if (empty? s) res
		        (recur (rest s) (conj res (first s))))))
		  
		  (defn loop2 [colls]
		    (loop [s colls res []]
		      (if (empty? s) res
		        (recur (rest s) (conj res (loop1 (first s)))))))
		  
		  (loop2 [[1 2] [3 4]])
		  
		  (loop [[[1 2] [3 4]] colls [] []] 
		    (if (empty? [[1 2] [3 4]]) 
		      [] 
		      (recur (rest [[1 2] [3 4]]) 
		             (conj [] (loop1 (first [[1 2] [3 4]]))))))
		  ```
		- Then we attach the bindings map to the loop AST. This is the complete interpreter step for loop:
		- ```js
		  let bindingsMap = new Map()
		  for (let i = 0; i < a1.length; i += 2) {
		      bindingsMap.set(a1[i].value, EVAL(a1[i + 1], env))
		  }
		  // walk code AST and replace locals with binding values
		  const walked = postwalk(x => {
		      if (bindingsMap.get(x.value)) {
		          return bindingsMap.get(x.value)
		      } else {
		          return x
		      }
		  }, ast)
		  // wrap loop body in implicit `do`
		  var loopAST = [types._symbol('do')].concat(walked.slice(2))
		  // attach bindings map as metadata
		  loopAST.bindings = bindingsMap
		  ast = loopAST
		  ```
- # `loop`?
	- It occurs to me that other lisps don't have `loop`. AFAIK it's just due to a limitation of Clojure. Just a thought, really, because my objective is still to write a *Clojure* interpreter.