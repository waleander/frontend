define([
    'Promise',
    'common/utils/report-error'
], function (Promise, reportError) {
    var allowedHosts = [
        location.protocol + '//tpc.googlesyndication.com',
        location.protocol + '//' + location.host
    ];
    var listeners = {};
    var registeredListeners = 0;

    var error405 = { code: 405, message: 'Service %% not implemented' };
    var error500 = { code: 500, message: 'Internal server error\n\n%%' };

    return {
        register: register,
        unregister: unregister
    };

    function register(type, callback, _window) {
        if( registeredListeners === 0 ) {
            on(_window || window);
        }

        listeners[type] || (listeners[type] = []);
        if (listeners[type].indexOf(callback) === -1) {
            listeners[type].push(callback);
            registeredListeners += 1;
        }
    }

    function unregister(type, callback, _window) {
        if (callback === undefined) {
            registeredListeners -= listeners[type].length;
            listeners[type].length = 0;
        } else {
            var idx = listeners[type].indexOf(callback);
            if (idx > -1) {
                registeredListeners -= 1;
                listeners[type].splice(idx, 1);
            }
        }

        if (registeredListeners === 0) {
            off(_window || window);
        }
    }

    function on(window) {
        window.addEventListener('message', onMessage);
    }

    function off(window) {
        window.removeEventListener('message', onMessage);
    }

    function onMessage(event) {
        // We only allow communication with ads created by DFP
        if (allowedHosts.indexOf(event.origin) === -1) {
            return;
        }

        try {
            // Even though the postMessage API allows passing objects as-is, the
            // serialisation/deserialisation is slower than using JSON
            // Source: https://bugs.chromium.org/p/chromium/issues/detail?id=536620#c11
            var data = JSON.parse(event.data);
        } catch( ex ) {
            return;
        }

        if (!isValidPayload(data)) {
            return;
        }

        // If there is no routine attached to this event type, we just answer
        // with an error code
        if (!listeners[data.type].length) {
            respond(formatError(error405, data.type), null);
            return;
        }

        // Because any listener can have side-effects (by unregistering itself),
        // we run the promise chain on a copy of the `listeners` array.
        // Hat tip @piuccio
        var promise = listeners[data.type].slice()
        // We offer, but don't impose, the possibility that a listener returns
        // a value that must be sent back to the calling frame. To do this,
        // we pass the cumulated returned value as a second argument to each
        // listener. Notice we don't try some clever way to compose the result
        // value ourselves, this would only make the solution more complex.
        // That means a listener can ignore the cumulated return value and
        // return something else entirely—life is unfair.
        // We don't know what each callack will be made of, we don't want to.
        // And so we wrap each call in a promise chain, in case one drops the
        // occasional fastdom bomb in the middle.
        .reduce(function (promise, listener) {
            return promise.then(function promiseCallback(ret) {
                var thisRet = listener(data.value, ret);
                return thisRet === undefined ? ret : thisRet;
            });
        }, Promise.resolve(true));

        return promise.then(function (response) {
            respond(null, response);
        }).catch(function (ex) {
            reportError(ex, { feature: 'native-ads' });
            respond(formatError(error500, ex), null);
        });

        function respond(error, result) {
            event.source.postMessage(JSON.stringify({ id: data.id, error: error, result: result }), event.origin);
        }
    }

    // Until DFP provides a way for us to identify with 100% certainty our
    // in-house creatives, we are left with doing some basic tests
    // such as validating the anatomy of the payload and whitelisting
    // event type
    function isValidPayload(payload) {
        return 'type' in payload &&
            'value' in payload &&
            payload.type in listeners &&
            (isStandardMessage() || isRubiconMessage());

        function isStandardMessage() {
            return 'id' in payload &&
                /^[a-f0-9]{8}-([a-f0-9]{4}-){3}[a-f0-9]{12}$/.test(payload.id);
        }

        function isRubiconMessage() {
            return payload.type === 'set-ad-height' &&
                'id' in payload.value &&
                'height' in payload.value;
        }
    }

    // Cheap string formatting function. It accepts as its first argument
    // an object `{ code, message }`. `message` is a string where successive
    // occurences of %% will be replaced by the following arguments. e.g.
    //
    // formatError({ message: "%%, you are so %%" }, "Regis", "lovely")
    //
    // returns `{ message: "Regis, you are so lovely" }`. Oh, thank you!
    function formatError() {
        if (arguments.length < 2) {
            return arguments[0] || '';
        }

        var error = arguments[0];
        Array.prototype.slice.call(arguments, 1).forEach(function (arg) {
            // Keep in mind that when the first argument is a string,
            // String.replace only replaces the first occurence
            error.message = error.message.replace('%%', arg);
        });

        return error;
    }
});
