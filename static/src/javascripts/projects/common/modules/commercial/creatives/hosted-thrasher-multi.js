define([
    'common/utils/fastdom-promise',
    'common/utils/config',
    'common/utils/template',
    'common/modules/commercial/creatives/add-tracking-pixel',
    'text!common/views/commercial/creatives/hosted-thrasher-multi.html'
], function (
    fastdom,
    config,
    template,
    addTrackingPixel,
    hostedThrasherStr
) {
    var hostedThrasherTemplate;

    return HostedThrasherMulti;

    function HostedThrasherMulti(adSlot, params) {
        hostedThrasherTemplate = template(hostedThrasherStr);

        return fastdom.write(function () {
            setAdditionalParams(params);

            adSlot.insertAdjacentHTML('beforeend', hostedThrasherTemplate({ data: params }));
            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }
        });

        function setAdditionalParams() {
            for (var i = 1; i <= params.elementsNo; i++) {
                var videoLength = params['videoLength' + i];
                if (videoLength){
                    var seconds = videoLength % 60;
                    var minutes = (videoLength - seconds) / 60;
                    params['timeString' + i] = minutes + (seconds < 10 ? ':0' : ':') + seconds;
                }

                params['linkTracking' + i] = 'Labs hosted container' +
                ' | ' + config.page.edition +
                ' | ' + config.page.section +
                ' | ' + params['subHeader' + i] +
                ' | ' + params.sponsorName;
            }
        }
    }
});
