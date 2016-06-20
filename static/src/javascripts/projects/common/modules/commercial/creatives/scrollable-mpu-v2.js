define([
    'Promise',
    'fastdom',
    'common/utils/$',
    'common/utils/detect',
    'common/utils/mediator',
    'common/utils/template',
    'text!common/views/commercial/creatives/scrollable-mpu-v2.html',
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
            layer1Image:      hasScrollEnabled ? params.layer1Image : params.mobileImage,
            backgroundImage:  hasScrollEnabled && params.backgroundImage ?
                '<div class="creative--scrollable-mpu-image" style="background-image: url(' + params.backgroundImage + ');"></div>' : '',
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
            switch (params.backgroundImagePType) {
                case 'fixed matching fluid250':
                    fastdom.write(function () {
                        $('.creative--scrollable-mpu-image', adSlot).addClass('creative--scrollable-mpu-image-fixed');
                    });
                    break;
                case 'parallax':
                    var scrollAmount = Math.ceil(adSlot.getBoundingClientRect().top * 0.3 * -1) + 20;
                    var scrollAmountP = scrollAmount + '%';
                    fastdom.write(function () {
                        $('.creative--scrollable-mpu-image', adSlot).addClass('creative--scrollable-mpu-image-parallax').css('background-position', '50%' + scrollAmountP);
                    });
                    break;
                default:
                    var position = scrollableMpu.getBoundingClientRect().top;
                    fastdom.write(function () {
                        $('.creative--scrollable-mpu-image', adSlot).css('background-position', '100% ' + position + 'px');
                    });
            }
        }

    }

});
