import { read_str } from './reader'

// read
const READ = str => read_str(str)

// eval
function EVAL(ast, env) {
    return ast;
}

// print
function PRINT(exp) {
    return exp;
}

export const evalString = function (str) { return PRINT(EVAL(READ(str), {})); };
