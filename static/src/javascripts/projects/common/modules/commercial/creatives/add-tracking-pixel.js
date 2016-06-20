define([
    'common/utils/template',
    'text!common/views/commercial/tracking-pixel.html'
], function (
    template,
    trackingPixelStr
) {
    var trackingPixelTpl = template(trackingPixelStr);
    function addTrackingPixel(adSlot, url) {
        adSlot.insertAdjacentHTML('beforebegin', trackingPixelTpl({ url: encodeURI(url) }));
    }

    return addTrackingPixel;
});
