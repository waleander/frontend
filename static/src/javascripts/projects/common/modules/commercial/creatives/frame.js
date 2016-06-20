define([
    'common/utils/fastdom-promise',
    'common/utils/template',
    'common/views/svgs',
    'common/modules/ui/toggles',
    'common/modules/commercial/creatives/add-tracking-pixel',
    'text!common/views/commercial/creatives/frame.html',
    'text!common/views/commercial/gustyle/label.html'
], function (
    fastdom,
    template,
    svgs,
    Toggles,
    addTrackingPixel,
    frameStr,
    labelStr
) {

    return Frame;

    function Frame(adSlot, params) {
        params.externalLinkIcon = svgs('externalLink', ['gu-external-icon']);
        params.target = params.newWindow === 'yes' ? '_blank' : '_self';

        var frameMarkup = template(frameStr, { data: params });
        var labelMarkup = template(labelStr, { data: {
            buttonTitle: 'Ad',
            infoTitle: 'Advertising on the Guardian',
            infoText: 'is created and paid for by third parties.',
            infoLinkText: 'Learn more about how advertising supports the Guardian.',
            infoLinkUrl: 'https://www.theguardian.com/advertising-on-the-guardian',
            icon: svgs('arrowicon', ['gu-comlabel__icon']),
            dataAttr: adSlot.id
        }});
        return fastdom.write(function () {
            adSlot.insertAdjacentHTML('beforeend', frameMarkup);
            adSlot.lastElementChild.insertAdjacentHTML('afterbegin', labelMarkup);
            adSlot.classList.add('ad-slot--frame');
            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }
            new Toggles(adSlot).init();
            return true;
        });
    }

});
