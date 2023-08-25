// Keywords
export const _keyword = obj => _keyword_Q(obj) ? obj : '\u029e' + obj
export const _keyword_Q = obj => typeof obj === 'string' && obj[0] === '\u029e'

// Vectors
export class Vector extends Array { }

// Maps
export function _assoc_BANG(hm, ...args) {
    if (args.length % 2 === 1) {
        throw new Error('Odd number of assoc arguments')
    }
    for (let i=0; i<args.length; i+=2) { hm.set(args[i], args[i+1]) }
    return hm
}