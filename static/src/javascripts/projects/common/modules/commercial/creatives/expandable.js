define([
    'bean',
    'bonzo',
    'common/utils/fastdom-promise',
    'common/utils/$',
    'common/utils/mediator',
    'common/utils/storage',
    'common/utils/template',
    'text!common/views/commercial/creatives/expandable.html',
    'common/modules/commercial/creatives/add-tracking-pixel'
], function (
    bean,
    bonzo,
    fastdom,
    $,
    mediator,
    storage,
    template,
    expandableTpl,
    addTrackingPixel
) {

    return Expandable;

    /**
     * https://www.google.com/dfp/59666047#delivery/CreateCreativeTemplate/creativeTemplateId=10028247
     */
    function Expandable(adSlot, params) {
        var isClosed     = true;
        var closedHeight = Math.min(bonzo.viewport().height / 3, 300);
        var openedHeight = Math.min(bonzo.viewport().height * 2 / 3, 600);

        var $expandable = $.create(template(expandableTpl, { data: params }));

        var $ad     = $('.ad-exp--expand', $expandable);
        var $button = $('.ad-exp__close-button', $expandable);

        if (!storage.local.get('gu.commercial.expandable.an-expandable')) {
            mediator.on('window:throttledScroll', listener);
        }

        bean.on($button[0], 'click', function () {
            $button.toggleClass('button-spin');
            $ad.css('height', isClosed ? openedHeight : closedHeight);
            isClosed = !isClosed;
        });

        return fastdom.write(function () {
            $ad.css('height', closedHeight);
            $('.ad-exp-collapse__slide', $expandable).css('height', closedHeight);
            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }
            $expandable.appendTo(adSlot);
            return true;
        });

        function listener() {
            if ((window.pageYOffset + bonzo.viewport().height) > ($ad.offset().top + openedHeight)) {
                // expires in 1 week
                var week = 1000 * 60 * 60 * 24 * 7;

                // TODO - needs to have a creative-specific id
                storage.local.set('gu.commercial.expandable.an-expandable', true, { expires: Date.now() + week });

                fastdom.write(function () {
                    $button.toggleClass('button-spin');
                    $ad.css('height', openedHeight);
                    isClosed = false;
                });
            }
        }
    }
});
