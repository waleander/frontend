define([
    'common/utils/fastdom-promise',
    'common/utils/$',
    'common/utils/detect',
    'common/utils/mediator',
    'common/utils/config',
    'common/utils/template',
    'common/views/svgs',
    'common/modules/commercial/gustyle/gustyle',
    'text!common/views/commercial/creatives/gu-style-comcontent.html',
    'text!common/views/commercial/creatives/gu-style-hosted.html',
    'lodash/objects/merge',
    'common/modules/commercial/creatives/add-tracking-pixel'
], function (
    fastdom,
    $,
    detect,
    mediator,
    config,
    template,
    svgs,
    GuStyle,
    gustyleComcontentTpl,
    gustyleHostedTpl,
    merge,
    addTrackingPixel
) {
    return GustyleComcontent;

    function GustyleComcontent(adSlot, params) {
        var externalLinkIcon = svgs('externalLink', ['gu-external-icon']),
            templateOptions = {
                articleContentColor: 'gu-display__content-color--' + params.articleContentColor,
                articleContentPosition: 'gu-display__content-position--' + params.articleContentPosition,
                articleHeaderFontSize: 'gu-display__content-size--' + params.articleHeaderFontSize,
                articleTextFontSize: 'gu-display__content-size--' + params.articleTextFontSize,
                brandLogoPosition: 'gu-display__logo-pos--' + params.brandLogoPosition,
                externalLinkIcon: externalLinkIcon,
                isHostedBottom: params.adType === 'gu-style-hosted-bottom'
            };
        var templateToLoad = params.adType === 'gu-style' ? gustyleComcontentTpl : gustyleHostedTpl;

        var title = params.articleHeaderText || 'unknown';
        var sponsor = 'Renault';
        params.linkTracking = 'Labs hosted native traffic card' +
            ' | ' + config.page.edition +
            ' | ' + config.page.section +
            ' | ' + title +
            ' | ' + sponsor;

        var markup = template(templateToLoad, { data: merge(params, templateOptions) });
        var gustyle = new GuStyle(adSlot, params);

        return fastdom.write(function () {
            adSlot.insertAdjacentHTML('beforeend', markup);

            if (params.trackingPixel) {
                addTrackingPixel(adSlot, params.trackingPixel + params.cacheBuster);
            }
        }).then(function () {
            return gustyle.addLabel();
        }).then(function () {
            return true;
        });
    }

});
