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
    'text!common/views/commercial/creatives/expandable-video-v2.html',
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
    ExpandableVideoTpl,
    merge,
    addTrackingPixel
) {
    return ExpandableVideo;

    function ExpandableVideo(adSlot, params) {
        var $ad;
        var isClosed     = true;
        var openedHeight, closedHeight;

        if (detect.isBreakpoint({min: 'tablet'})) {
            closedHeight = 250;
            openedHeight = 500;
        } else {
            closedHeight = 150;
            openedHeight = 300;
        }

        var videoHeight = openedHeight,
            showmoreArrow = {
                showArrow: (params.showMoreType === 'arrow-only' || params.showMoreType === 'plus-and-arrow') ?
                    '<button class="ad-exp__open-chevron ad-exp__open">' + svgs('arrowdownicon') + '</button>' : ''
            },
            showmorePlus = {
                showPlus: (params.showMoreType === 'plus-only' || params.showMoreType === 'plus-and-arrow') ?
                    '<button class="ad-exp__close-button ad-exp__open">' + svgs('closeCentralIcon') + '</button>' : ''
            },
            videoSource = {
                videoEmbed: (params.YoutubeVideoURL !== '') ?
                    '<iframe id="YTPlayer" width="100%" height="' + videoHeight + '" src="' + params.YoutubeVideoURL + '?showinfo=0&amp;rel=0&amp;controls=0&amp;fs=0&amp;title=0&amp;byline=0&amp;portrait=0" frameborder="0" class="expandable-video"></iframe>' : ''
            },
            $ExpandableVideo = $.create(template(ExpandableVideoTpl, { data: merge(params, showmoreArrow, showmorePlus, videoSource) }));

        bean.on(adSlot, 'click', '.ad-exp__open', function () {
            fastdom.write(function () {
                var videoSrc = $('#YTPlayer').attr('src'),
                    videoSrcAutoplay = videoSrc;
                if (videoSrc.indexOf('autoplay') === -1) {
                    videoSrcAutoplay = videoSrc + '&amp;autoplay=1';
                } else {
                    videoSrcAutoplay = videoSrcAutoplay.replace(isClosed ? 'autoplay=0' : 'autoplay=1', isClosed ? 'autoplay=1' : 'autoplay=0');
                }
                $('.ad-exp__close-button').toggleClass('button-spin');
                $('.ad-exp__open-chevron').removeClass('chevron-up').toggleClass('chevron-down');
                $ad.css('height', isClosed ? openedHeight : closedHeight);
                $('.slide-video, .slide-video .ad-exp__layer', adSlot).css('height', isClosed ? openedHeight : closedHeight).toggleClass('slide-video__expand');
                isClosed = !isClosed;
                setTimeout(function () {
                    $('#YTPlayer').attr('src', videoSrcAutoplay);
                }, 1000);
            });
        });

        return fastdom.write(function () {
            $ad = $('.ad-exp--expand', $ExpandableVideo).css('height', closedHeight);

            $('.ad-exp-collapse__slide', $ExpandableVideo).css('height', closedHeight);

            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }
            $ExpandableVideo.appendTo(adSlot);
            return true;
        });
    }
});
