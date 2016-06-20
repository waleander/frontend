define([
    'common/utils/fastdom-promise',
    'common/utils/config',
    'common/utils/template',
    'common/modules/commercial/hosted-video',
    'common/modules/commercial/creatives/add-tracking-pixel',
    'text!common/views/commercial/creatives/hosted-thrasher.html'
], function (
    fastdom,
    config,
    template,
    hostedVideo,
    addTrackingPixel,
    hostedThrasherStr
) {
    var hostedThrasherTemplate;

    return HostedThrasher;

    function HostedThrasher(adSlot, params) {
        hostedThrasherTemplate = hostedThrasherTemplate || template(hostedThrasherStr);

        return fastdom.write(function () {
            var title = params.header2 || 'unknown';
            var sponsor = 'Renault';
            var videoLength = params.videoLength;
            if(videoLength){
                var seconds = videoLength % 60;
                var minutes = (videoLength - seconds) / 60;
                params.timeString = minutes + (seconds < 10 ? ':0' : ':') + seconds;
            }

            params.linkTracking = 'Labs hosted container' +
                ' | ' + config.page.edition +
                ' | ' + config.page.section +
                ' | ' + title +
                ' | ' + sponsor;
            adSlot.insertAdjacentHTML('beforeend', hostedThrasherTemplate({ data: params }));
            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }
        }).then(hostedVideo.init).then(function () {
            return true;
        });
    }
});
