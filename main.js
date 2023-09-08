import './style.css'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { clojure } from "./src/clojure"
import solutions from './test/exercises.json';
import testSuites from './test/tests.json';
import { evalString, deftests, clearTests } from "./src/interpreter"

let editorState = EditorState.create({
  doc: `(def mycolls
  [[1 2] [3 4] [5 6] [7 8] [9 0]])

(def f str)
(def c0 [1 2])
(def c1 [3 4])
(def c2 [5 6])
(def colls [[7 8] [9 0]])
    
(macroexpand
  (map+ str [[1 2] [3 4] [5 6] [7 8] [9 0]]))

(def colls mycolls)
(def f str)
(loop* [s0 (seq (nth colls 0)) s1 (seq (nth colls 1)) s2 (seq (nth colls 2)) s3 (seq (nth colls 3)) s4 (seq (nth colls 4)) res []] 
       (if (or (empty? s0) (empty? s1) (empty? s2) (empty? s3) (empty? s4)) 
         (apply list res) 
         (recur (rest s0) (rest s1) (rest s2) (rest s3) (rest s4) 
                (conj res (f (first s0) (first s1) (first s2) (first s3) (first s4))))))

(list* c0 c1 c2 colls)

(map+ f (list* c0 c1 c2 colls))

(macroexpand
  (map+ f (list* c0 c1 c2 colls)))

(def f str)
(def colls [[1 2] [3 4] [5 6] [7 8] [9 0]])

(loop* [s0 (seq (nth colls 0)) s1 (seq (nth colls 1)) s2 (seq (nth colls 2)) s3 (seq (nth colls 3)) s4 (seq (nth colls 4)) res []] 
  (if (or (empty? s0) (empty? s1) (empty? s2) (empty? s3) (empty? s4)) 
    (apply list res)
    (recur (rest s0) (rest s1) (rest s2) (rest s3) (rest s4)
      (conj res (f (first s0) (first s1) (first s2) (first s3) (first s4))))))

(map str [1 2] [3 4] [5 6] [7 8] [9 0])`,
  extensions: [basicSetup, clojure()]
})

let view = new EditorView({
  state: editorState,
  parent: document.querySelector('#app')
})

let testState = EditorState.create({
  readOnly: true,
  extensions: [
    //EditorView.editable.of(false),
    basicSetup, clojure()]
})

let testView = new EditorView({
  state: testState,
  parent: document.querySelector('#test')
})

let topLevelText = "Shift+Enter = Eval top-level form"
let keyBindings = "<strong>Key bindings:</strong>,Alt+Enter = Eval cell," +
  topLevelText + ",Ctrl/Cmd+Enter= Eval at cursor";
keyBindings = keyBindings.split(',');
for (let i = 0; i < keyBindings.length; i++)
  keyBindings[i] = "" + keyBindings[i] + "<br>";
keyBindings = keyBindings.join('');
document.getElementById("keymap").innerHTML = keyBindings;

let exercise
const results = document.getElementById("results")

function loadExercise(slug) {
  results.innerHTML = ""
  exercise = slug
  const k = slug.replaceAll("-", "_")
  const src = solutions[k].trim()
  //console.log("loading test suite", testSuites[k + "_test"])
  const testSuite = testSuites[k + "_test"].trim()
  const doc = view.state.doc.toString()
  const testDoc = testView.state.doc.toString()
  const end = doc.length
  view.dispatch({
    changes: { from: 0, to: end, insert: src },
    selection: { anchor: 0, head: 0 }
  })
  testView.dispatch({
    changes: { from: 0, to: testDoc.length, insert: testSuite },
    selection: { anchor: 0, head: 0 }
  })
}

function testSolution(slug) {
  loadExercise(slug)
  const k = slug.replaceAll("-", "_")
  const src = solutions[k].trim()
  let doc = view.state.doc.toString()
  const end = doc.length
  view.dispatch({
    changes: { from: 0, to: end, insert: src },
    selection: { anchor: 0, head: 0 }
  })
  doc = view.state.doc.toString()
  clearTests()
  const testSuite = testSuites[k + "_test"].trim()
  try {
    evalString("(do " + doc + ")")
  } catch (error) {
    results.innerHTML = error
    results.style.color = 'red';
    return null
  }
  try {
    evalString("(do " + testSuite + ")")
  } catch (error) {
    results.innerHTML = error
    results.style.color = 'red';
    return null
  }
  let fails = []
  for (const test of deftests) {
    if (test.result.includes(false)) {
      fails.push(test.test.value)
    }
  }
  //console.log("deftests:", deftests)
  //console.log("fails:", fails)
  const uniqueFails = [...new Set(fails)];
  if (uniqueFails.length == 1) {
    results.innerHTML = "1 fail: " + uniqueFails[0]
    results.style.color = 'red';
  } else if (uniqueFails.length > 1) {
    results.innerHTML = uniqueFails.length + " fails: " + uniqueFails.join(", ")
    results.style.color = 'red';
  }
  else {
    results.innerHTML = "Passed 😍"
    results.style.color = 'green';
  }
}

function shuffle(array) {
  let currentIndex = array.length, randomIndex;
  while (currentIndex != 0) {
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex--;
    [array[currentIndex], array[randomIndex]] = [
      array[randomIndex], array[currentIndex]];
  }

  return array;
}

const exercisesToTest = shuffle(Object.keys(solutions))
function randExercise() {
  return exercisesToTest[Math.floor(Math.random() * exercisesToTest.length)]
}

function testExercises() {
  let passes = []
  let fails = []
  for (let exercise = 0; exercise < exercisesToTest.length; exercise++) {
    console.log("Testing ", exercisesToTest[exercise])
    testSolution(exercisesToTest[exercise])
    if (results.innerHTML === "Passed 😍") {
      console.log("exercise passed")
      passes.push(exercisesToTest[exercise])
      results.innerHTML = passes.length + " tests passed 😍"
    } else {
      results.innerHTML = passes.length + " tests passed, " + exercisesToTest[exercise] + " failed"
      fails.push(exercisesToTest[exercise])
    }
  }
  console.log("Passes:", passes)
  console.log("Fails:", fails)
}

function testExercisesUntilFail() {
  let passes = []
  var fails = []
  for (let exercise = 0; exercise < exercisesToTest.length; exercise++) {
    console.log("Testing ", exercisesToTest[exercise])
    testSolution(exercisesToTest[exercise])
    if (results.innerHTML === "Passed 😍") {
      console.log("exercise passed")
      passes.push(exercisesToTest[exercise])
      results.innerHTML = passes.length + " tests passed 😍"
    } else {
      results.innerHTML = passes.length + " tests passed, " + exercisesToTest[exercise] + " failed"
      fails.push(exercisesToTest[exercise])
      break
    }
  }
  console.log("Passes:", passes)
  console.log("Fails:", fails)
}

//testSolution(randExercise())
//testSolution("go_counting")
//loadExercise("go_counting")
//testExercisesUntilFail()
//testExercises()