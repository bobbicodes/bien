import './style.css'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { clojure } from "./src/clojure"
import solutions from './test/foreclojure-solutions.json';
import testSuites from './test/foreclojure-tests.json';

let editorState = EditorState.create({
  doc: `(or true true)`,
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

let topLevelText = "Alt+Enter = Eval top-level form"
let keyBindings = "<strong>Key bindings:</strong>,Shift+Enter = Eval cell," +
  topLevelText + ",Ctrl/Cmd+Enter = Eval at cursor";
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
  console.log(src)
  const testSuite = testSuites[k].trim()
  const doc = view.state.doc.toString()
  const testDoc = testView.state.doc.toString()
  const end = doc.length
  view.dispatch({
    changes: { from: 0, to: end, insert: src},
    selection: { anchor: 0, head: 0 }
  })
  testView.dispatch({
    changes: { from: 0, to: testDoc.length, insert: testSuite},
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
    changes: { from: 0, to: end, insert: src},
    selection: { anchor: 0, head: 0 }
  })
  doc = view.state.doc.toString()
  const testSuite = testSuites[k].trim()
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
    if (!test.result) {
      fails.push(test.test.value)
    }
  }
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

const exercisesToTest = Object.keys(solutions)

function testExercises() {
  let passes = []
  let fails = []
  for (let exercise = 0; exercise < exercisesToTest.length; exercise++) {
    console.log("Testing ", exercisesToTest[exercise])
    testSolution(exercisesToTest[exercise])
    if (results.innerHTML === "Passed 😍") {
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

testExercises()