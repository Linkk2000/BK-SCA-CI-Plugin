export function urlJoin (...args) {
    return args.filter(arg => arg).join('/').replace(/([^:]\/)\/+/g, '$1')
}

