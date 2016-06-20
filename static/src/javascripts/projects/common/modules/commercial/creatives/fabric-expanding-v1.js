define([
    'bean',
    'bonzo',
    'common/utils/fastdom-promise',
    'common/utils/$',
    'common/utils/detect',
    'common/utils/mediator',
    'common/utils/storage',
    'common/utils/template',
    'common/views/svgs',
    'text!common/views/commercial/creatives/fabric-expanding-v1.html',
    'text!common/views/commercial/creatives/fabric-expanding-video.html',
    'lodash/functions/bindAll',
    'lodash/objects/merge',
    'common/modules/commercial/creatives/add-tracking-pixel'
], function (
    bean,
    bonzo,
    fastdom,
    $,
    detect,
    mediator,
    storage,
    template,
    svgs,
    fabricExpandingV1Html,
    fabricExpandingVideoHtml,
    bindAll,
    merge,
    addTrackingPixel
) {
    var hasScrollEnabled = !detect.isIOS() && !detect.isAndroid();

    // Forked from expandable-v3.js
    return FabricExpandingV1;

    function  FabricExpandingV1(adSlot, params) {
        var isClosed = true;
        var initialExpandCounter = false;

        var closedHeight = 250;
        var openedHeight = 500;

        var hasVideo = params.videoURL !== '';
        var videoDesktop = {
            videoDesktop: hasVideo ? buildVideo('js-fabric-video--desktop') : ''
        };
        var videoMobile = {
            videoMobile: hasVideo ? buildVideo('js-fabric-video--mobile') : ''
        };
        var showmoreArrow = {
            showArrow: (params.showMoreType === 'arrow-only' || params.showMoreType === 'plus-and-arrow') ?
            '<button class="ad-exp__open-chevron ad-exp__open">' + svgs('arrowdownicon') + '</button>' : ''
        };
        var showmorePlus = {
            showPlus: (params.showMoreType === 'plus-only' || params.showMoreType === 'plus-and-arrow') ?
            '<button class="ad-exp__close-button ad-exp__open">' + svgs('closeCentralIcon') + '</button>' : ''
        };
        var scrollbgDefaultY = '0%'; // used if no parallax / fixed background scroll support
        var scrollingbg = {
            scrollbg: params.backgroundImagePType !== 'none' ?
            '<div class="ad-exp--expand-scrolling-bg" style="background-image: url(' + params.backgroundImageP + '); background-position: ' + params.backgroundImagePPosition + ' ' + scrollbgDefaultY + '; background-repeat: ' + params.backgroundImagePRepeat + ';"></div>' : ''
        };
        var $fabricExpandingV1 = $.create(template(fabricExpandingV1Html, { data: merge(params, showmoreArrow, showmorePlus, videoDesktop, videoMobile, scrollingbg) }));

        var $ad, $button;

        mediator.on('window:throttledScroll', listener);

        bean.on(adSlot, 'click', '.ad-exp__open', function () {
            if (!isClosed && hasVideo) {
                // wait 1000ms for close animation to finish
                stopVideo(1000);
            }

            fastdom.write(function () {
                $('.ad-exp__close-button').toggleClass('button-spin');
                $('.ad-exp__open-chevron').removeClass('chevron-up').toggleClass('chevron-down');
                $ad.css('height', isClosed ? openedHeight : closedHeight);
                isClosed = !isClosed;
                initialExpandCounter = true;
            });
        });

        if (hasScrollEnabled) {
            // update bg position
            updateBgPosition();

            mediator.on('window:throttledScroll', updateBgPosition);
            // to be safe, also update on window resize
            mediator.on('window:resize', updateBgPosition);
        }

        return fastdom.write(function () {

            $ad     = $('.ad-exp--expand', $fabricExpandingV1).css('height', closedHeight);
            $button = $('.ad-exp__open', $fabricExpandingV1);

            $('.ad-exp-collapse__slide', $fabricExpandingV1).css('height', closedHeight);

            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }

            $fabricExpandingV1.appendTo(adSlot);
            return true;
        });

        function updateBgPosition() {
            var scrollY = window.pageYOffset;
            var viewportHeight = bonzo.viewport().height;
            var adSlotTop = scrollY + adSlot.getBoundingClientRect().top;

            var adHeight = (isClosed) ? closedHeight : openedHeight;
            var inViewB = ((scrollY + viewportHeight) > adSlotTop);
            var inViewT = ((scrollY - (adHeight * 2)) < adSlotTop + 20);
            var topCusp = (inViewT &&
            ((scrollY + (viewportHeight * 0.4) - adHeight) > adSlotTop)) ?
                'true' : 'false';
            var bottomCusp = (inViewB &&
            (scrollY + (viewportHeight * 0.5)) < adSlotTop) ?
                'true' : 'false';
            var bottomScroll = (bottomCusp === 'true') ?
            50 - ((scrollY + (viewportHeight * 0.5) - adSlotTop) * -0.2) : 50;
            var topScroll = (topCusp === 'true') ?
                ((scrollY + (viewportHeight * 0.4) - adSlotTop - adHeight) * 0.2) : 0;

            var scrollAmount;

            switch (params.backgroundImagePType) {
                case 'split':
                    scrollAmount = bottomScroll + topScroll;
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg', adSlot).css({
                            'background-repeat': 'no-repeat',
                            'background-position': '50%' + scrollAmount + '%'
                        });
                    });
                    break;
                case 'fixed':
                    scrollAmount = (scrollY - adSlotTop);
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg', adSlot).css('background-position', '50%' + (scrollAmount  + 'px'));
                    });
                    break;
                case 'fixed matching fluid250':
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg', adSlot).addClass('ad-exp--expand-scrolling-bg-fixed');
                    });
                    break;
                case 'parallax':
                    scrollAmount = Math.ceil((scrollY - adSlotTop) * 0.3 * -1) + 20;
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg', adSlot).addClass('ad-exp--expand-scrolling-bg-parallax');
                        $('.ad-exp--expand-scrolling-bg', adSlot).css('background-position', '50%' + (scrollAmount + '%'));
                    });
                    break;
                case 'none' :
                    break;
            }
        }

        function listener() {
            if (!initialExpandCounter && bonzo.viewport().height > adSlot.getBoundingClientRect().top + openedHeight) {
                var itemId = $('.ad-slot__content', adSlot).attr('id'),
                    itemIdArray = itemId.split('/');

                if (!storage.local.get('gu.commercial.expandable.' + itemIdArray[1])) {
                    // expires in 1 week
                    var week = 1000 * 60 * 60 * 24 * 7;
                    fastdom.write(function () {
                        storage.local.set('gu.commercial.expandable.' + itemIdArray[1], true, { expires: Date.now() + week });
                        $button.addClass('button-spin');
                        $('.ad-exp__open-chevron').removeClass('chevron-up').addClass('chevron-down');
                        $ad.css('height', openedHeight);
                        isClosed = false;
                        initialExpandCounter = true;
                    });
                } else if (isClosed) {
                    fastdom.write(function () {
                        $('.ad-exp__open-chevron').addClass('chevron-up');
                    });
                }
                return true;
            }
        }

        function buildVideo(customClass) {
            var videoAspectRatio = 16 / 9;
            var videoHeight = detect.isBreakpoint({max: 'phablet'})
                ? 125
                : 250;
            var videoWidth = videoHeight * videoAspectRatio;
            var leftMargin = params.videoPositionH === 'center'
                ? 'margin-left: ' + videoWidth / -2 + 'px'
                : '';
            var leftPosition = params.videoPositionH === 'left'
                ? 'left: ' + params.videoHorizSpace + 'px'
                : '';
            var rightPosition = params.videoPositionH === 'right'
                ? 'right: ' + params.videoHorizSpace + 'px'
                : '';

            var viewModel = {
                width : videoWidth,
                height : videoHeight,
                src : params.videoURL + '?rel=0&amp;controls=0&amp;showinfo=0&amp;title=0&amp;byline=0&amp;portrait=0',
                className : [
                    'expandable_video',
                    'expandable_video--horiz-pos-' + params.videoPositionH,
                    customClass
                ].join(' '),
                inlineStyle : [leftMargin, leftPosition, rightPosition].join('; ')
            };

            return template(fabricExpandingVideoHtml, viewModel);
        }

        function stopVideo(delay) {
            delay = delay || 0;

            var videoSelector = detect.isBreakpoint({min: 'tablet'}) ? '.js-fabric-video--desktop' : '.js-fabric-video--mobile';
            var video = $(videoSelector, adSlot);
            var videoSrc = video.attr('src');

            window.setTimeout(function () {
                video.attr('src', videoSrc + '&amp;autoplay=0');
            }, delay);
        }
    }

});
