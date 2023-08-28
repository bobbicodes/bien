import { Fraction } from 'fraction.js'
import { PRINT } from './interpreter.js'

export function _obj_type(obj) {
    //console.log("obj_type:", typeof obj)
    if (_symbol_Q(obj)) { return 'symbol'; }
    else if (_hash_map_Q(obj)) { return 'hash-map'; }
    else if (_list_Q(obj)) { return 'list'; }
    else if (_vector_Q(obj)) { return 'vector'; }
    else if (_ratio_Q(obj)) { return 'ratio'; }
    else if (_lazy_range_Q(obj)) { return 'lazy-range'; }
    else if (_iterate_Q(obj)) { return 'iterate'; }
    else if (_cycle_Q(obj)) { return 'cycle'; }
    else if (_function_Q(obj)) { return 'function'; }
    else if (_set_Q(obj)) { return 'set'; }
    else if (_nil_Q(obj)) { return 'nil'; }
    else if (_regex_Q(obj)) { return 'regex'; }
    else if (_true_Q(obj)) { return 'true'; }
    else if (_false_Q(obj)) { return 'false'; }
    else if (_atom_Q(obj)) { return 'atom'; }
    else {
        switch (typeof (obj)) {
            case 'number': return 'number';
            case 'function': return 'function';
            case 'string': return obj[0] == '\u029e' ? 'keyword' : 'string';
            default: throw new Error("Unknown type '" + typeof (obj) + "'");
        }
    }
}

export function _iterate_Q(x) {
    if (x === null) {
        return false
    }
    if (typeof (x) === "object") {
        return Object.hasOwn(x, 'name') && x.name === 'Iterate'
    }
    return false
}

export function _cycle_Q(x) {
    if (x === null) {
        return false
    }
    if (typeof (x) === "object") {
        return Object.hasOwn(x, 'name') && x.name === 'Cycle'
    }
    return false
}

export function _lazy_range_Q(x) {
    if (x === null) {
        return false
    }
    if (typeof (x) === "object") {
        return Object.hasOwn(x, 'name') && x.name === 'lazyRange'
    }
    return false
}

export function _sequential_Q(lst) { return _list_Q(lst) || _vector_Q(lst); }


export function _equal_Q(a, b) {
    var ota = _obj_type(a), otb = _obj_type(b);
    if (!(ota === otb || (_sequential_Q(a) && _sequential_Q(b)))) {
        return false;
    }
    switch (ota) {
        case 'symbol': return a.value === b.value;
        case 'list':
        case 'vector':
        case 'set':
            //console.log("comparing", ota, "and", otb)
            if (a.length !== b.length) { return false; }
            for (var i = 0; i < a.length; i++) {
                if (!_equal_Q(a[i], b[i])) { return false; }
            }
            return true;
        case 'hash-map':
            if (a.size !== b.size) { return false; }
            for (var [key, value] of a) {
                if (!_equal_Q(a.get(key), b.get(key))) { return false; }
            }
            return true;
        default:
            return a === b;
    }
}

export function allEqual() {
    const args = Array.from(arguments)
    return args.every(v => _equal_Q(v, args[0]))
}

export function _clone(obj) {
    //console.log("cloning", obj)
    var new_obj;
    switch (_obj_type(obj)) {
        case 'list':
            new_obj = obj.slice(0);
            break;
        case 'vector':
            new_obj = obj.slice(0);
            new_obj.__isvector__ = true;
            break;
        case 'hash-map':
            new_obj = new Map(obj);
            break;
        case 'function':
            new_obj = obj.clone();
            break;
        default:
            throw new Error("clone of non-collection: " + _obj_type(obj));
    }
    Object.defineProperty(new_obj, "__meta__", {
        enumerable: false,
        writable: true
    });
    return new_obj;
}


// Scalars
export function _nil_Q(a) { return a === null ? true : false; }
export function _true_Q(a) { return a === true ? true : false; }
export function _false_Q(a) { return a === false ? true : false; }
export function _number_Q(obj) { return typeof obj === 'number'; }
export function _string_Q(obj) {
    return typeof obj === 'string' && obj[0] !== '\u029e';
}


// Symbols
function Symbol(name) {
    this.value = name;
    return this;
}
Symbol.prototype.toString = function () { return this.value; }
export function _symbol(name) { return new Symbol(name); }
export function _symbol_Q(obj) { return obj instanceof Symbol; }

// Ratios

export function _ratio(x) {
    return new Fraction(x)
}
export function _ratio_Q(obj) {
    return obj instanceof Fraction
}

// Keywords
export function _keyword(obj) {
    if (typeof obj === 'string' && obj[0] === '\u029e') {
        return obj;
    } else {
        return "\u029e" + obj;
    }
}
export function _keyword_Q(obj) {
    return typeof obj === 'string' && obj[0] === '\u029e';
}


export function _regex_Q(obj) {
    return obj instanceof RegExp
}

// Functions
export function _function(Eval, Env, ast, env, params) {
    var fn = function () {
        return Eval(ast, new Env(env, params, arguments));
    };
    fn.__meta__ = null;
    fn.__ast__ = ast;
    fn.__gen_env__ = function (args) { return new Env(env, params, args); };
    fn._ismacro_ = false;
    return fn;
}

export function multifn(Eval, Env, bodies, env) {
    var fn = function () {
        return Eval(bodies[arguments.length][1], 
            new Env(env, bodies[arguments.length][0], arguments));
    }
    fn.__meta__ = null;
    fn.__multifn__ = true
    fn.__ast__ = bodies;
    fn.__gen_env__ = function (args) {
        return new Env(env, bodies[args.length][0], args)
    }
    fn._ismacro_ = false;
    return fn;
}

export function _function_Q(obj) { return typeof obj == "function"; }
Function.prototype.clone = function () {
    var that = this;
    var temp = function () { return that.apply(this, arguments); };
    for (var key in this) {
        temp[key] = this[key];
    }
    return temp;
};
export function _fn_Q(obj) { return _function_Q(obj) && !obj._ismacro_; }
export function _macro_Q(obj) { return _function_Q(obj) && !!obj._ismacro_; }


// Lists
export function _list() { return Array.prototype.slice.call(arguments, 0); }
export function _list_Q(obj) { return Array.isArray(obj) && !obj.__isvector__; }


// Vectors
export function _vector() {
    var v = Array.prototype.slice.call(arguments, 0);
    v.__isvector__ = true;
    return v;
}
export function _vector_Q(obj) { return Array.isArray(obj) && !!obj.__isvector__; }



// Hash Maps
export function _hash_map() {
    if (arguments.length % 2 === 1) {
        throw new Error("Odd number of hash map arguments");
    }
    const hm = new Map();
    var args = [hm].concat(Array.prototype.slice.call(arguments, 0));
    return _assoc_BANG.apply(null, args);
}

export function _hash_map_Q(hm) {
    return typeof hm === "object" &&
        (hm instanceof Map)
}

export function _assoc_BANG(hm) {
    if (arguments.length % 2 !== 1) {
        throw new Error("Odd number of assoc arguments");
    }
    for (var i = 1; i < arguments.length; i += 2) {
        var ktoken = arguments[i],
            vtoken = arguments[i + 1];
        hm.set(ktoken, vtoken)
    }
    return hm;
}
export function _dissoc_BANG(hm) {
    for (var i = 1; i < arguments.length; i++) {
        var ktoken = arguments[i];
        hm.delete(ktoken)
    }
    return hm;
}

// Sets
export function _set() {
    return new Set(arguments)
}

export function _set_Q(set) {
    return typeof set === "object" &&
        (set instanceof Set)
}

// Atoms
function Atom(val) { this.val = val; }
export function _atom(val) { return new Atom(val); }
export function _atom_Q(atm) { return atm instanceof Atom; }