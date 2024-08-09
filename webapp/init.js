async function init(name) {
    const wasm = await window.kijijune
    const keys = Object.keys(wasm)
    var func = null
    for (var k in keys) {
        var key = keys[k]
        var value = wasm[key]
        if (typeof(value) == "function") {
            if (window[key] === undefined) {
                window[key] = value
                console.log("adding function "+key+"() to window object")
            }
            else
                console.log(key+" function already exists on window object")
            if (key == name)
                func = value
        }
    }
    if (func != null)
        func()
}

