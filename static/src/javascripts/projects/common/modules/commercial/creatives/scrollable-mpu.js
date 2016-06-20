define([
    'Promise',
    'fastdom',
    'common/utils/$',
    'common/utils/detect',
    'common/utils/mediator',
    'common/utils/template',
    'text!common/views/commercial/creatives/scrollable-mpu.html',
    'text!common/views/commercial/tracking-pixel.html'
], function (
    Promise,
    fastdom,
    $,
    detect,
    mediator,
    template,
    scrollableMpuTpl,
    trackingPixelStr
) {
    var hasScrollEnabled = !detect.isIOS() && !detect.isAndroid();

    return ScrollableMpu;

    /**
     * https://www.google.com/dfp/59666047#delivery/CreateCreativeTemplate/creativeTemplateId=10026567
     */
    function ScrollableMpu(adSlot, params) {
        var templateOptions = {
            clickMacro:       params.clickMacro,
            destination:      params.destination,
            image:            hasScrollEnabled ? params.image : params.staticImage,
            stillImage:       hasScrollEnabled && params.stillImage ?
                '<div class="creative--scrollable-mpu-static-image" style="background-image: url(' + params.stillImage + ');"></div>' : '',
            trackingPixelImg: params.trackingPixel ? template(trackingPixelStr, { url: encodeURI(params.trackingPixel) }) : ''
        };

        adSlot.insertAdjacentHTML('beforeend', template(scrollableMpuTpl, templateOptions));
        var scrollableMpu = adSlot.lastElementChild;

        if (hasScrollEnabled) {
            // update bg position
            fastdom.read(updateBgPosition);

            mediator.on('window:throttledScroll', updateBgPosition);
            // to be safe, also update on window resize
            mediator.on('window:resize', updateBgPosition);
        }

        return Promise.resolve(true);

        function updateBgPosition() {
            var position = scrollableMpu.getBoundingClientRect().top;
            fastdom.write(function () {
                $('.creative--scrollable-mpu-image').css('background-position', '100% ' + position + 'px');
            });
        }
    }
});
