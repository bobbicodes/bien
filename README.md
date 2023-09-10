# bobbi-lisp

A browser-based interactive Lisp environment for learning Clojure

## Note:
Development is moving to Codeberg: https://codeberg.org/bobbicodes/bobbi-lisp. I'll probably stop updating this once I'm sure I'm happy with it.

Bobbi-lisp is based on [MAL (Make-a-Lisp)](https://github.com/kanaka/mal), which is a pedagogical Lisp interpreter inspired by Clojure. In a nutshell, this means that it has a set of built-in data structures besides lists, and is designed for functional programming using immutability by default with an `atom` type for controlled mutation. A goal is to support as much of Clojure as possible so it can be used for teaching Clojure.

I'd estimate that the goal is around 50% complete *by quantity*, though the unimplemented half is likely much more involved than the first.

### Differences from Clojure

1. [Lazy sequences](https://github.com/bobbicodes/bien/issues/13) are probably the biggest one.

2. No multimethods/protocols. Chris Houser (Chouser) has created a [sketch](https://gist.github.com/Chouser/6081ea66d144d13e56fc) of protocols which is included but nothing uses them yet.

3. Namespaces. At this point they are not a high priority because this tool is meant for doing simple exercises and not writing production software. However, there are many functions which have slashes in their name to support patterns such as `str/join`, `Character/digit`, etc. 

[Zippers](https://www.st.cs.uni-saarland.de/edu/seminare/2005/advanced-fp/docs/huet-zipper.pdf) have been implemented, and are available as a library that can be loaded:

```clojure
(require "zip")
```

I plan to update this section regularly with detailed descriptions of various features as I make progress.

## Running

In the project root, run:

```
npm install
npm run dev
```

## Testing

The application has a built-in testing framework based on 185 coding exercises taken from 4clojure and Exercism. To run it, uncomment the last line of `main.js`:

```javascript
testExercises()
```
