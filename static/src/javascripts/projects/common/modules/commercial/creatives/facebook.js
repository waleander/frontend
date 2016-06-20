/* global FB */
define([
    'Promise',
    'fastdom',
    'common/utils/config',
    'common/utils/assign',
    'common/views/svgs',
    'common/utils/template',
    'common/utils/load-script',
    'common/utils/report-error',
    'common/modules/ui/toggles',
    'common/modules/commercial/creatives/add-tracking-pixel',
    'text!common/views/commercial/creatives/facebook.html',
    'text!common/views/commercial/gustyle/label.html'
], function(Promise, fastdom, config, assign, svgs, template, loadScript, reportError, Toggles, addTrackingPixel, facebookStr, labelStr) {
    var scriptId = 'facebook-jssdk';
    var scriptSrc = '//connect.facebook.net/en_US/sdk/xfbml.ad.js#xfbml=1&version=v2.5';
    var adUnits = {
        mpu: { placementId: '180444840287_10154600557405288', adId: 'fb_ad_root_mpu' }
    };
    var facebookTpl;
    var labelTpl;

    return Facebook;

    function Facebook(adSlot, params) {
        facebookTpl || (facebookTpl = template(facebookStr));
        labelTpl || (labelTpl = template(labelStr));

        return new Promise(function (resolve, reject) {
            window.fbAsyncInit || (window.fbAsyncInit = function() {
                FB.Event.subscribe(
                    'ad.loaded',
                    function(placementID) {
                        var interim = document.querySelector('[data-placementid="' + placementID + '"]');
                        var ad = document.getElementById(interim.getAttribute('data-nativeadid'));
                        if (ad) {
                            fastdom.write(function () {
                                ad.style.display = 'block';
                                resolve(true);
                            });
                        } else {
                            resolve(false);
                        }
                    }
                );

                FB.Event.subscribe(
                    'ad.error',
                    function(errorCode, errorMessage, placementID) {
                        reportError(new Error('Facebook returned an empty ad response'), {
                            feature: 'commercial',
                            placementID: placementID,
                            errorMessage: errorMessage
                        }, false);
                        reject();
                    }
                );
            });

            var markup = facebookTpl(assign({ externalLink: svgs('externalLink') }, adUnits[params.placement]));
            var labelMarkup = labelTpl({ data: {
                buttonTitle: 'Ad',
                infoTitle: 'Advertising on the Guardian',
                infoText: 'is created and paid for by third parties.',
                infoLinkText: 'Learn more about how advertising supports the Guardian.',
                infoLinkUrl: 'https://www.theguardian.com/advertising-on-the-guardian',
                icon: svgs('arrowicon', ['gu-comlabel__icon']),
                dataAttr: adSlot.id
            }});

            fastdom.write(function () {
                adSlot.insertAdjacentHTML('beforeend', markup);
                adSlot.lastElementChild.insertAdjacentHTML('afterbegin', labelMarkup);
                adSlot.classList.add('ad-slot--facebook');
                if (params.trackingPixel) {
                    addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
                }
                new Toggles(adSlot).init();
                loadScript({ id: scriptId, src: scriptSrc + '&appId=' + config.page.fbAppId});
            });
        });
    }
});
