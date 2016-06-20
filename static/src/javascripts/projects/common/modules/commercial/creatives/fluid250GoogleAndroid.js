define([
    'Promise',
    'common/utils/template',
    'text!common/views/commercial/creatives/fluid250GoogleAndroid.html',
    'common/modules/commercial/creatives/add-tracking-pixel'
], function (
    Promise,
    template,
    fluid250GoogleAndroidTpl,
    addTrackingPixel
) {
    return Fluid250GoogleAndroid;

    function Fluid250GoogleAndroid(adSlot, params) {
        adSlot.insertAdjacentHTML('beforeend', template(fluid250GoogleAndroidTpl, params));

        if (this.params.trackingPixel) {
            addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
        }

        return Promise.resolve(true);
    }
});
