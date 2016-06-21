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
    'text!common/views/commercial/creatives/expandable-v2.html',
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
    expandableV2Tpl,
    merge,
    addTrackingPixel
) {
    var hasScrollEnabled = !detect.isIOS() && !detect.isAndroid();

    return ExpandableV2;

    /**
     * https://www.google.com/dfp/59666047#delivery/CreateCreativeTemplate/creativeTemplateId=10028247
     */
    function ExpandableV2(adSlot, params) {
        var isClosed     = true;
        var closedHeight, openedHeight, $ad, $button;

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
                    '<div class="ad-exp--expand-scrolling-bg" style="background-image: url(' + params.backgroundImageP + '); background-position: ' + params.backgroundImagePPosition + ' 50%;"></div>' : ''
            },
            $expandablev2 = $.create(template(expandableV2Tpl, { data: merge(params, showmoreArrow, showmorePlus, videoDesktop, scrollingbg) }));

        if (!storage.local.get('gu.commercial.expandable.' + params.ecid)) {
            mediator.on('window:throttledScroll', listener);
        }

        bean.on(adSlot, 'click', '.ad-exp__open', function () {
            fastdom.write(function () {
                $('.ad-exp__close-button').toggleClass('button-spin');
                $('.ad-exp__open-chevron').toggleClass('chevron-down');
                $ad.css('height', isClosed ? openedHeight : closedHeight);
                isClosed = !isClosed;
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
            $ad     = $('.ad-exp--expand', $expandablev2).css('height', closedHeight);
            $button = $('.ad-exp__open', $expandablev2);

            $('.ad-exp-collapse__slide', $expandablev2).css('height', closedHeight);

            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }

            $expandablev2.appendTo(adSlot);

            return true;
        });

        function updateBgPosition() {
            var scrollAmount;
            var scrollY = window.pageYOffset,
                viewportHeight = bonzo.viewport().height,
                adSlotTop = scrollY + adSlot.getBoundingClientRect().top;

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

            switch (params.backgroundImagePType) {
                case 'split':
                    scrollAmount = bottomScroll + topScroll + '%';
                    fastdom.write(function () {
                        $('.ad-exp--expand-scrolling-bg').css('background-repeat', 'no-repeat');
                    });
                    break;
                case 'fixed':
                    scrollAmount = (scrollY - adSlotTop) + 'px';
                    break;
                case 'parallax':
                    scrollAmount = ((scrollY - adSlotTop) * 0.15) + '%';
                    break;
            }

            fastdom.write(function () {
                $('.ad-exp--expand-scrolling-bg').css('background-position', '50%' + scrollAmount);
            });
        }

        function listener() {
            if (bonzo.viewport().height > (adSlot.getBoundingClientRect().top + openedHeight)) {
                // expires in 1 week
                var week = 1000 * 60 * 60 * 24 * 7;

                storage.local.set('gu.commercial.expandable.' + params.ecid, true, { expires: Date.now() + week });
                fastdom.write(function () {
                    $button.toggleClass('button-spin');
                    $('.ad-exp__open-chevron').toggleClass('chevron-down');
                    $ad.css('height', openedHeight);
                    isClosed = false;
                });
            }
        }
    }
});
