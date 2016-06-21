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
    'text!common/views/commercial/creatives/expandable-v3.html',
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
    expandableV3Tpl,
    merge,
    addTrackingPixel
) {
    var hasScrollEnabled = !detect.isIOS() && !detect.isAndroid();

    return ExpandableV3;

    /**
     * https://www.google.com/dfp/59666047#delivery/CreateCreativeTemplate/creativeTemplateId=10028247
     */
    function ExpandableV3(adSlot, params) {
        var $ad, $button;

        var isClosed     = true;
        var initialExpandCounter = false;
        var closedHeight, openedHeight;

        if (detect.isBreakpoint({min: 'tablet'})) {
            closedHeight = 250;
            openedHeight = 500;
        } else {
            closedHeight = 150;
            openedHeight = 300;
        }
        var videoHeight = closedHeight - 24,
            videoWidth = (videoHeight * 16) / 9,
            leftMargin = (params.videoPositionH === 'center' ?
                'margin-left: ' + videoWidth / -2 + 'px; ' : ''
            ),
            leftPosition = (params.videoPositionH === 'left' ?
                'left: ' + params.videoHorizSpace + 'px; ' : ''
            ),
            rightPosition = (params.videoPositionH === 'right' ?
                'right: ' + params.videoHorizSpace + 'px; ' : ''
            ),
            videoDesktop = {
                video: (params.videoURL !== '') ?
                    '<iframe id="myYTPlayer" width="' + videoWidth + '" height="' + videoHeight + '" src="' + params.videoURL + '?rel=0&amp;controls=0&amp;showinfo=0&amp;title=0&amp;byline=0&amp;portrait=0" frameborder="0" class="expandable_video expandable_video--horiz-pos-' + params.videoPositionH + '" style="' + leftMargin + leftPosition + rightPosition + '"></iframe>' : ''
            },
            showmoreArrow = {
                showArrow: (params.showMoreType === 'arrow-only' || params.showMoreType === 'plus-and-arrow') ?
                    '<button class="ad-exp__open-chevron ad-exp__open">' + svgs('arrowdownicon') + '</button>' : ''
            },
            showmorePlus = {
                showPlus: (params.showMoreType === 'plus-only' || params.showMoreType === 'plus-and-arrow') ?
                    '<button class="ad-exp__close-button ad-exp__open">' + svgs('closeCentralIcon') + '</button>' : ''
            },
            scrollingbg = {
                scrollbg: (params.backgroundImagePType !== '' || params.backgroundImagePType !== 'none') ?
                    '<div class="ad-exp--expand-scrolling-bg" style="background-image: url(' + params.backgroundImageP + '); background-position: ' + params.backgroundImagePPosition + ' 50%; background-repeat: ' + params.backgroundImagePRepeat + ';"></div>' : ''
            },
            $expandableV3 = $.create(template(expandableV3Tpl, { data: merge(params, showmoreArrow, showmorePlus, videoDesktop, scrollingbg) }));

        mediator.on('window:throttledScroll', listener);

        bean.on(adSlot, 'click', '.ad-exp__open', function () {
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

            $ad     = $('.ad-exp--expand', $expandableV3).css('height', closedHeight);
            $button = $('.ad-exp__open', $expandableV3);

            $('.ad-exp-collapse__slide', $expandableV3).css('height', closedHeight);

            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }

            $expandableV3.appendTo(adSlot);
            return true;
        });

        function updateBgPosition() {
            var scrollAmount, scrollAmountP;
            var scrollY = window.pageYOffset,
                viewportHeight = bonzo.viewport().height,
                adSlotTop = scrollY + adSlot.getBoundingClientRect().top,

                adHeight = (isClosed) ?
                    closedHeight : openedHeight,
                inViewB = ((scrollY + viewportHeight) > adSlotTop),
                inViewT = ((scrollY - (adHeight * 2)) < adSlotTop + 20),
                topCusp = (inViewT &&
                    ((scrollY + (viewportHeight * 0.4) - adHeight) > adSlotTop)) ?
                    'true' : 'false',
                bottomCusp = (inViewB &&
                    (scrollY + (viewportHeight * 0.5)) < adSlotTop) ?
                    'true' : 'false',
                bottomScroll = (bottomCusp === 'true') ?
                    50 - ((scrollY + (viewportHeight * 0.5) - adSlotTop) * -0.2) : 50,
                topScroll = (topCusp === 'true') ?
                    ((scrollY + (viewportHeight * 0.4) - adSlotTop - adHeight) * 0.2) : 0;

            switch (params.backgroundImagePType) {
                case 'split':
                    scrollAmount = bottomScroll + topScroll + '%';
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg', adSlot).css({
                            'background-repeat': 'no-repeat',
                            'background-position': '50%' + scrollAmount
                        });
                    });
                    break;
                case 'fixed':
                    scrollAmount = (scrollY - adSlotTop) + 'px';
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg', adSlot).css('background-position', '50%' + scrollAmount);
                    });
                    break;
                case 'fixed matching fluid250':
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg', adSlot).addClass('ad-exp--expand-scrolling-bg-fixed');
                    });
                    break;
                case 'parallax':
                    scrollAmount = Math.ceil((scrollY - adSlotTop) * 0.3 * -1) + 20;
                    scrollAmountP = scrollAmount + '%';
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg', adSlot).addClass('ad-exp--expand-scrolling-bg-parallax');
                        $('.ad-exp--expand-scrolling-bg', adSlot).css('background-position', '50%' + scrollAmountP);
                    });
                    break;
            }
        }

        function listener() {
            if (!initialExpandCounter && (bonzo.viewport().height) > adSlot.getBoundingClientRect().top + openedHeight) {
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
    }
});
