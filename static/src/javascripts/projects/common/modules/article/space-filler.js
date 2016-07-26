define([
    'common/utils/fastdom-promise',
    'common/utils/QueueAsync',
    'Promise',
    'raven',
    'common/modules/article/spacefinder',
    'common/utils/steady-page'
], function (
    fastdom,
    QueueAsync,
    Promise,
    raven,
    spacefinder,
    steadyPage
) {
    var queue;

    function SpaceFiller() {
        queue = new QueueAsync(onError);
    }

    /**
     * A safer way of using spacefinder.
     * Given a set of spacefinder rules, applies a writer to the first matching paragraph.
     * Uses fastdom to avoid layout-thrashing, but queues up asynchronous writes to avoid race conditions. We don't
     * seek a slot for a new component until all the other component writes have finished.
     *
     * @param rules - a spacefinder ruleset
     * @param writer - function, takes a para element and injects a container for the new content synchronously. It should NOT use Fastdom.
     * @param options - Options
     * @param options.useSteadyPage - True if we should use steadypage instead of fastdom to insert els
     *
     * @returns {Promise} - when insertion attempt completed, resolves 'true' if inserted, or 'false' if no space found
     */
    SpaceFiller.prototype.fillSpace = function (rules, writer, options) {
        return queue.add(insertNextContent);

        function insertNextContent() {
            return spacefinder.findSpace(rules, options).then(onSpacesFound, onNoSpacesFound);
        }

        function onSpacesFound(paragraphs) {
            if (options.useSteadyPage) {
                // The writer function should return an array containing an array
                // of containers and an array of callbacks to insert elements
                // as the steadypage util expects
                var steadyPageParams = writer(paragraphs);
                return steadyPage.insert(steadyPageParams[0], steadyPageParams[1]);
            } else {
                return fastdom.write(function () {
                    return writer(paragraphs);
                });
            }
        }

        function onNoSpacesFound(ex) {
            if (ex instanceof spacefinder.SpaceError) {
                return false;
            } else {
                throw ex;
            }
        }
    };

    function onError(e) {
        // e.g. if writer fails
        raven.captureException(e);
        return false;
    }

    return new SpaceFiller();
});
