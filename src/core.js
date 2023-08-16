import { read_str } from './reader.js';
import { _pr_str, _println } from './printer.js'
import * as types from './types.js'

// Errors/Exceptions
function mal_throw(exc) { throw new Error(exc); }


// String functions
function pr_str() {
    return Array.prototype.map.call(arguments, function (exp) {
        return _pr_str(exp, true);
    }).join(" ");
}

function str() {
    return Array.prototype.map.call(arguments, function (exp) {
        return _pr_str(exp, false);
    }).join("");
}

function prn() {
    println.apply({}, Array.prototype.map.call(arguments, function (exp) {
        return _pr_str(exp, true);
    }));
}

function println() {
    println.apply({}, Array.prototype.map.call(arguments, function (exp) {
        return _pr_str(exp, false);
    }));
}

function slurp(f) {
    if (typeof require !== 'undefined') {
        return require('fs').readFileSync(f, 'utf-8');
    } else {
        var req = new XMLHttpRequest();
        req.open("GET", f, false);
        req.send();
        if (req.status == 200) {
            return req.responseText;
        } else {
            throw new Error("Failed to slurp file: " + f);
        }
    }
}


// Number functions
function time_ms() { return new Date().getTime(); }


// Hash Map functions
function assoc(src_hm) {
    var hm = types._clone(src_hm);
    var args = [hm].concat(Array.prototype.slice.call(arguments, 1));
    return types._assoc_BANG.apply(null, args);
}

function dissoc(src_hm) {
    var hm = types._clone(src_hm);
    var args = [hm].concat(Array.prototype.slice.call(arguments, 1));
    return types._dissoc_BANG.apply(null, args);
}

function get(hm, key) {
    if (hm != null) {
        return hm.get(key);
    } else {
        return null;
    }
}

function contains_Q(coll, key) {
    if (types._set_Q(coll)) {
        return coll.has(key)
    }
    if (key in coll) { return true; } else { return false; }
}

function keys(hm) { return Object.keys(hm); }
function vals(hm) { return Object.keys(hm).map(function (k) { return hm[k]; }); }


// Sequence functions
export function cons(a, b) { return [a].concat(b); }

function concat(lst) {
    lst = lst || [];
    return lst.concat.apply(lst, Array.prototype.slice.call(arguments, 1));
}
function vec(lst) {
    if (types._list_Q(lst)) {
        var v = Array.prototype.slice.call(lst, 0);
        v.__isvector__ = true;
        return v;
    } else {
        return lst;
    }
}

export function nth(lst, idx) {
    if (idx < lst.length) { return lst[idx]; }
    else { throw new Error("nth: index out of range"); }
}

export function first(lst) { return (lst === null) ? null : seq(lst)[0]; }
export function second(lst) { return (lst === null) ? null : seq(lst)[1]; }
export function last(lst) { return (lst === null) ? null : seq(lst)[seq(lst).length-1]; }

export function rest(lst) { return (lst == null) ? [] : seq(lst).slice(1); }

function empty_Q(lst) { return lst.length === 0; }

export function count(s) {
    if (Array.isArray(s)) { return s.length; }
    else if (s === null) { return 0; }
    else { return Object.keys(s).length; }
}

function conj(lst) {
    //console.log(lst)
    if (types._list_Q(lst)) {
        return Array.prototype.slice.call(arguments, 1).reverse().concat(lst);
    } else if (types._vector_Q(lst)) {
        var v = lst.concat(Array.prototype.slice.call(arguments, 1));
        v.__isvector__ = true;
        return v;
    } else if (types._set_Q(lst)) {
        return lst.add(arguments[1])
    } else if (types._hash_map_Q(lst)) {
        var hm = new Map(lst)
        const args = Array.prototype.slice.call(arguments, 1)
        for (var i=0; i<args.length; i++) {
            var ktoken = args[i][0],
                vtoken = args[i][1]
            hm.set(ktoken, vtoken)
        }
        return hm
    }
}


export function seq(obj) {
    if (types._list_Q(obj)) {
        return obj.length > 0 ? obj : null;
    } else if (types._vector_Q(obj)) {
        return obj.length > 0 ? Array.prototype.slice.call(obj, 0) : null;
    } else if (types._string_Q(obj)) {
        return obj.length > 0 ? obj.split('') : null;
    } else if (types._set_Q(obj)) {
        return Array.from(obj)
    }
    else if (obj === null) {
        return null;
    } else {
        throw new Error("seq: called on non-sequence");
    }
}


function apply(f) {
    var args = Array.prototype.slice.call(arguments, 1);
    return f.apply(f, args.slice(0, args.length - 1).concat(args[args.length - 1]));
}


// Metadata functions
function with_meta(obj, m) {
    var new_obj = types._clone(obj);
    new_obj.__meta__ = m;
    return new_obj;
}

function meta(obj) {
    // TODO: support symbols and atoms
    if ((!types._sequential_Q(obj)) &&
        (!(types._hash_map_Q(obj))) &&
        (!(types._function_Q(obj)))) {
        throw new Error("attempt to get metadata from: " + types._obj_type(obj));
    }
    return obj.__meta__;
}


// Atom functions
function deref(atm) { return atm.val; }
function reset_BANG(atm, val) { return atm.val = val; }
function swap_BANG(atm, f) {
    var args = [atm.val].concat(Array.prototype.slice.call(arguments, 2));
    atm.val = f.apply(f, args);
    return atm.val;
}

function js_eval(str) {
    return js_to_mal(eval(str.toString()));
}

function js_method_call(object_method_str) {
    var args = Array.prototype.slice.call(arguments, 1),
        r = resolve_js(object_method_str),
        obj = r[0], f = r[1];
    var res = f.apply(obj, args);
    return js_to_mal(res);
}

function toSet() {
    return new Set(arguments[0])
}

function _union(setA, setB) {
    const _union = new Set(setA);
    for (const elem of setB) {
        _union.add(elem);
    }
    return _union;
}

function _intersection(setA, setB) {
    const _intersection = new Set();
    for (const elem of setB) {
        if (setA.has(elem)) {
            _intersection.add(elem);
        }
    }
    return _intersection;
}

function _is(a) {
    if (a) {
        return true
    } else {
        return false
    }
}

function resolve_js(str) {
    if (str.match(/\./)) {
        var re = /^(.*)\.[^\.]*$/,
            match = re.exec(str);
        //console.log("match:", match[1])
        return [eval(match[1]), eval(str)];
    } else {
        console.log("no match in", str)
        return [GLOBAL, eval(str)];
    }
}

function js_to_mal(obj) {
    if (obj === null || obj === undefined) {
        return null;
    }
    var cache = [];
    var str = JSON.stringify(obj, function (key, value) {
        if (typeof value === 'object' && value !== null) {
            if (cache.indexOf(value) !== -1) {
                // Circular reference found, discard key
                return;
            }
            // Store value in our collection
            cache.push(value);
        }
        return value;
    });
    cache = null; // Enable garbage collection
    return JSON.parse(str);
}

function map(f, s) {
    if (types._string_Q(s)) {
        s = seq(s)
    }
    return s.map(function (el) { return f(el); });
}

function int(x) {
    if (types._number_Q(x)) {
        return Math.floor(x)
    } else if (x[0] === '\\') {
        // is a char
        return x.charCodeAt(1)
    } else {
        return x.charCodeAt(0)
    }
}

function char(int) {
    return String.fromCharCode(int)
}

function filter(f, lst) {
    return seq(lst).filter(function (el) { return f(el); });
}

function min() {
    return Math.min.apply(null, arguments);
}

function max() {
    return Math.max.apply(null, arguments);
}

function sum() {
    return Array.from(arguments).reduce((acc, a) => acc + a, 0);
}

function product() {
    return Array.from(arguments).reduce((acc, a) => acc * a, 1);
}

function take(n, coll) {
    return coll.slice(0, n)
}

function drop(n, coll) {
    return coll.slice(n)
}

function repeat(n, x) {
    return Array(n).fill(x)
}

function range(start, end, step) {
    if (arguments.length === 0) {
        return new Error("infinite ranges not implemented")
    }
    if (!end) {
        return range(0, start)
    }
    var ans = [];
    if (step) {
        for (let i = start; i < end; i += step) {
            ans.push(i);
        }
        return ans
    }
    for (let i = start; i < end; i++) {
        ans.push(i);
    }
    return ans;
}

function mod(x, y) {
    return x % y
}

function sort(x) {
    if (types._string_Q(x)) {
        return x.split('').sort().join('');
    }
    if (types._list_Q(x)) {
        return x.sort()
    }
    if (types._set_Q(x)) {
        return new Set(Array.from(x).sort())
    } else {
        var v = x.sort()
        v.__isvector__ = true;
        return v;
    }
}

function pop(lst) {
    if (types._list_Q(lst)) {
        return lst.slice(1);
    } else {
        var v = lst.slice(0, -1);
        v.__isvector__ = true;
        return v;
    }
}

function peek(lst) {
    if (types._list_Q(lst)) {
        return lst[0]
    } else {
        return lst[lst.length - 1]
    }
}

function upperCase(s) {
    return s.toUpperCase()
}

function isLetter(c) {
    return c.toLowerCase() != c.toUpperCase();
  }

function lowerCase(s) {
    return s.toLowerCase()
}

function int_Q(x) {
    return Number.isInteger(x)
}

function _join(separator, coll) {
    return coll.join(separator)
}

function rand_int() {
    return Math.floor(Math.random() * arguments[0]);
}

function rand_nth() {
    const n = Math.floor(Math.random() * arguments[0].size)
    return arguments[0].get(n)
}

function _round(n) {
    return Math.round(n)
}

// types.ns is namespace of type functions
export var ns = {
    'type': types._obj_type,
    '=': types._equal_Q,
    'throw': mal_throw,
    'nil?': types._nil_Q,
    'true?': types._true_Q,
    'is': _is,
    'false?': types._false_Q,
    'number?': types._number_Q,
    'string?': types._string_Q,
    'symbol': types._symbol,
    'symbol?': types._symbol_Q,
    'set?': types._set_Q,
    'keyword': types._keyword,
    'keyword?': types._keyword_Q,
    'fn?': types._fn_Q,
    'macro?': types._macro_Q,
    'char': char,
    'int?': int_Q,
    'rand-int': rand_int,
    'rand-nth': rand_nth,
    'Math/round': _round,

    'pr-str': pr_str,
    'str': str,
    'prn': prn,
    'println': println,
    //'readline': readline.readline,
    'read-string': read_str,
    'slurp': slurp,
    '<': function (a, b) { return a < b; },
    '<=': function (a, b) { return a <= b; },
    '>': function (a, b) { return a > b; },
    '>=': function (a, b) { return a >= b; },
    '+': sum,
    '-': function (a, b) { return a - b; },
    '*': product,
    '/': function (a, b) { return a / b; },
    'inc': function (a) { return a + 1; },
    "time-ms": time_ms,
    'max': max,
    'min': min,
    'range': range,
    'sort': sort,
    'peek': peek,
    'pop': pop,
    'lower-case': lowerCase,
    'upper-case': upperCase,
    'Character/isLetter': isLetter,

    'list': types._list,
    'list?': types._list_Q,
    'vector': types._vector,
    'vector?': types._vector_Q,
    'hash-map': types._hash_map,
    'map?': types._hash_map_Q,
    'assoc': assoc,
    'dissoc': dissoc,
    'get': get,
    'contains?': contains_Q,
    'keys': keys,
    'vals': vals,
    'int': int,
    //'mod': mod,
    'rem': mod,

    'sequential?': types._sequential_Q,
    'cons': cons,
    'concat': concat,
    'vec': vec,
    'nth': nth,
    'first': first,
    'second': second,
    'rest': rest,
    'last': last,
    'take': take,
    'drop': drop,
    'empty?': empty_Q,
    'count': count,
    'apply': apply,
    //'map': map,
    'repeat': repeat,
    'str/join': _join,

    'conj': conj,
    'seq': seq,
    'filter': filter,

    'with-meta': with_meta,
    'meta': meta,
    'atom': types._atom,
    'atom?': types._atom_Q,
    "deref": deref,
    "reset!": reset_BANG,
    "swap!": swap_BANG,

    'js-eval': js_eval,
    '.': js_method_call,

    'set': toSet,
    'set/union': _union,
    'set/intersection': _intersection
};