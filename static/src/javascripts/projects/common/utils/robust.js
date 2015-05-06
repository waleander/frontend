/*jshint -W024 */

/*
    Swallows (and reports) exceptions. Designed to wrap around modules at the "bootstrap" level.
    For example "comments throwing an exception should not stop auto refresh"
 */
define([
    'raven'
], function (
    raven
) {
    function Robust(name, block, reporter) {

        if (!reporter) {
            reporter = raven.captureException;
        }
        if (document.domain.toLowerCase().indexOf("local") == -1) {
            try {
                block();
            } catch (e) {
                reporter(e, {tags: {module: name}});
            }
        } else {
            block();
        }
    }

    return Robust;

});
