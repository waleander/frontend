define([
    'bean',
    'qwery',
    'common/utils/$',
    'fastdom',
    'Promise'
], function (
    bean,
    qwery,
    $,
    fastdom,
    Promise
) {
    var omniture;

    /**
     * The omniture module depends on common/modules/experiments/ab, so trying to
     * require omniture directly inside an AB test gives you a circular dependency.
     *
     * This is a workaround to load omniture without making it a dependency of
     * this module, which is required by an AB test.
     */
    function getOmniture() {
        return new Promise(function (resolve) {
            if (omniture) {
                return resolve(omniture);
            }

            require('common/modules/analytics/omniture', function (omnitureM) {
                omniture = omnitureM;
                resolve(omniture);
            });
        });
    }

    function postMessageToIframe(message, src, iframeEl) {
        return new Promise(function (resolve) {
            iframeEl.contentWindow.postMessage(message, src);
            resolve();
        });
    }

    function checkIframeType() {
        return new Promise(function (resolve) {
            resolve('footer__email-form');
        });
    }

    function bindListeners() {
        bean.on(window, 'onmessage message', function (event) {
            var messageOrigin = event.origin.replace(/.*?:\/\//g, ''),
                eventType = event.data.type,
                eventData = event.data.data;

            // Only listen to message from the same domain and check the source is our email iframe
            if (messageOrigin === window.location.host && event.source && event.source.frameElement.classList.contains('js-email-sub__iframe')) {
                if (eventType === 'omniture') {
                    getOmniture().then(function (omniture) {
                        omniture.trackLinkImmediate(eventData);
                    });
                }

                if (eventType === 'status' && eventData === 'setupDone') {
                    checkIframeType(event).then(function (iframeId) {
                        updateFormWithData(iframeId);
                    });

                }

            }
        });
    }

    function updateFormWithData(iframeId) {
        var iframeEl = document.getElementById(iframeId),
            iframeSrc = iframeEl.src;

        postMessageToIframe({
            type: 'ui',
            data: {
                formTitle: 'New form title',
                formDescription: 'New form Description',
                removeComforter: false,
                formCampaignCode: 'test',
                referrer: window.location.href
            }
        }, iframeSrc, iframeEl);
    }

    return {
        init: function (el) {
            bindListeners();
            updateFormWithData(el.id);
        }
    };
});
