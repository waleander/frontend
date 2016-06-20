define([
    'qwery',
    'bonzo',
    'Promise',
    'fastdom',
    'common/utils/detect',
    'common/utils/template',
    'common/utils/mediator',
    'common/modules/commercial/creatives/add-tracking-pixel',
    'text!common/views/commercial/creatives/fluid250.html',
    'text!common/views/commercial/creatives/iframe-video.html',
    'text!common/views/commercial/creatives/scrollbg.html',
    'lodash/objects/merge'
], function (
    $,
    bonzo,
    Promise,
    fastdom,
    detect,
    template,
    mediator,
    addTrackingPixel,
    fluid250Str,
    iframeVideoStr,
    scrollBgStr,
    merge
) {
    var hasScrollEnabled = !detect.isIOS() && !detect.isAndroid();
    var isEnhanced = detect.isEnhanced();
    var isIE9OrLess = detect.getUserAgent.browser === 'MSIE' && (detect.getUserAgent.version === '9' || detect.getUserAgent.version === '8');

    var fluid250Tpl;
    var iframeVideoTpl;
    var scrollBgTpl;

    return Fluid250;

    function Fluid250(adSlot, params) {
        var scrollingBg, layer2;

        if (!fluid250Tpl) {
            fluid250Tpl = template(fluid250Str);
            iframeVideoTpl = template(iframeVideoStr);
            scrollBgTpl = template(scrollBgStr);
        }

        var position = {
            position: params.videoPositionH === 'left' || params.videoPositionH === 'right' ?
                params.videoPositionH + ':' + params.videoHorizSpace + 'px;' :
                ''
        };

        var templateOptions = {
            creativeHeight: params.creativeHeight || '',
            isFixedHeight: params.creativeHeight === 'fixed',
            showLabel: params.showAdLabel !== 'hide',
            video: params.videoURL ? iframeVideoTpl(merge(params, position)) : '',
            hasContainer: 'layerTwoAnimation' in params,
            layerTwoBGPosition: params.layerTwoBGPosition && (
                !params.layerTwoAnimation ||
                params.layerTwoAnimation === 'disabled' ||
                (!isEnhanced && params.layerTwoAnimation === 'enabled')
            ) ?
                params.layerTwoBGPosition :
                '0% 0%',
            scrollbg: params.backgroundImagePType && params.backgroundImagePType !== 'none' ?
                scrollBgTpl(params) :
                false
        };

        adSlot.insertAdjacentHTML('beforeend', fluid250Tpl({ data: merge(params, templateOptions) }));
        if (templateOptions.scrollbg) {
            scrollingBg = $('.ad-scrolling-bg', adSlot);
            layer2 = $('.hide-until-tablet .fluid250_layer2', adSlot);

            if (hasScrollEnabled) {
                // update bg position
                fastdom.read(updateBgPosition);
                mediator.on('window:throttledScroll', updateBgPosition);
                // to be safe, also update on window resize
                mediator.on('window:resize', updateBgPosition);
            }
        }

        if (params.trackingPixel) {
            addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
        }

        return Promise.resolve(true);


        function updateBgPosition() {
            if (params.backgroundImagePType === 'parallax') {
                var scrollAmount = Math.ceil(adSlot.getBoundingClientRect().top * 0.3 * -1) + 20;
                fastdom.write(function () {
                    bonzo(scrollingBg)
                        .addClass('ad-scrolling-bg-parallax')
                        .css('background-position', '50% ' + scrollAmount + '%');
                });
            }

            layer2Animation();
        }

        function layer2Animation() {
            var inViewB;
            if (params.layerTwoAnimation === 'enabled' && isEnhanced && !isIE9OrLess) {
                inViewB = bonzo.viewport().height > adSlot.getBoundingClientRect().top;
                fastdom.write(function () {
                    bonzo(layer2).addClass('ad-scrolling-text-hide' + (params.layerTwoAnimationPosition ? '-' + params.layerTwoAnimationPosition : ''));
                    if (inViewB) {
                        bonzo(layer2).addClass('ad-scrolling-text-animate' + (params.layerTwoAnimationPosition ? '-' + params.layerTwoAnimationPosition : ''));
                    }
                });
            }
        }
    }
});
