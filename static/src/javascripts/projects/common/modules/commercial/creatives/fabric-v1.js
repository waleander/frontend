define([
    'qwery',
    'bonzo',
    'common/utils/fastdom-promise',
    'common/utils/detect',
    'common/utils/template',
    'common/utils/mediator',
    'common/modules/commercial/creatives/add-tracking-pixel',
    'text!common/views/commercial/creatives/fabric-v1.html',
    'text!common/views/commercial/creatives/iframe-video.html',
    'text!common/views/commercial/creatives/scrollbg.html',
    'lodash/objects/merge'
], function (
    $,
    bonzo,
    fastdom,
    detect,
    template,
    mediator,
    addTrackingPixel,
    fabricV1Html,
    iframeVideoStr,
    scrollBgStr,
    merge
) {
    var hasBackgroundFixedSupport = !detect.isAndroid();
    var isEnhanced = detect.isEnhanced();
    var isIE10OrLess = detect.getUserAgent.browser === 'MSIE' && (parseInt(detect.getUserAgent.version) <= 10);

    var fabricV1Tpl;
    var iframeVideoTpl;
    var scrollBgTpl;

    // This is a hasty clone of fluid250.js

    return FabricV1;

    function FabricV1(adSlot, params) {
        var scrollingBg, layer2, scrollType;

        adSlot.classList.add('ad-slot__fabric-v1');
        adSlot.classList.add('content__mobile-full-width');

        if (!fabricV1Tpl) {
            fabricV1Tpl = template(fabricV1Html);
            iframeVideoTpl = template(iframeVideoStr);
            scrollBgTpl = template(scrollBgStr);
        }

        var videoPosition = {
            position: params.videoPositionH === 'left' || params.videoPositionH === 'right' ?
            params.videoPositionH + ':' + params.videoHorizSpace + 'px;' : ''
        };

        var templateOptions = {
            showLabel: params.showAdLabel !== 'hide',
            video: params.videoURL ? iframeVideoTpl(merge(params, videoPosition)) : '',
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

        if (templateOptions.scrollbg) {
            // update bg position
            fastdom.read(updateBgPosition);
            mediator.on('window:throttledScroll', updateBgPosition);
            // to be safe, also update on window resize
            mediator.on('window:resize', updateBgPosition);
        }

        if (params.trackingPixel) {
            addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
        }

        return fastdom.write(function () {
            adSlot.insertAdjacentHTML('beforeend', fabricV1Tpl({data: merge(params, templateOptions)}));
            scrollingBg = $('.ad-scrolling-bg', adSlot);
            layer2 = $('.hide-until-tablet .fabric-v1_layer2', adSlot);
            scrollType = params.backgroundImagePType;

            // layer two animations must not have a background position, otherwise the background will
            // be visible before the animation has been initiated.
            if (params.layerTwoAnimation === 'enabled' && isEnhanced && !isIE10OrLess) {
                bonzo(layer2).css('background-position', '');
            }

            if (scrollType === 'fixed' && hasBackgroundFixedSupport) {
                bonzo(scrollingBg).css('background-attachment', 'fixed');
            }

            return true;
        });

        function updateBgPosition() {
            if (scrollType === 'parallax') {
                var scrollAmount = Math.ceil(adSlot.getBoundingClientRect().top * 0.3 * -1) + 20;
                fastdom.write(function () {
                    bonzo(scrollingBg)
                        .addClass('ad-scrolling-bg-parallax')
                        .css('background-position', '50% ' + scrollAmount + '%');
                });
            } else if (scrollType === 'fixed' && !hasBackgroundFixedSupport) {
                var adRect = adSlot.getBoundingClientRect();
                var vPos = (window.innerHeight - adRect.bottom + adRect.height / 2) / window.innerHeight * 100;
                fastdom.write(function () {
                    bonzo(scrollingBg).css('background-position', '50% ' + vPos + '%');
                });
            }
            layer2Animation();
        }

        function layer2Animation() {
            var inViewB;
            if (params.layerTwoAnimation === 'enabled' && isEnhanced && !isIE10OrLess) {
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
