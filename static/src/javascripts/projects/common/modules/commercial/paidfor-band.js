define([
    'fastdom',
    'Promise',
    'common/utils/config',
    'common/modules/ui/sticky'
], function (
    fastdom,
    Promise,
    config,
    Sticky
) {
    function init() {
        return new Promise(function (resolve) {
            var elem = document.querySelector('.facia-page > .paidfor-band, #article > .paidfor-band');
            if (elem && config.page.contentType !=='Interactive') {
                new Sticky(elem).init();
            }
            resolve();
        });
    }

    return {
        init: init
    };
});
