define([
    'common/views/svgs',
    'common/utils/config',
    'common/utils/template',
    'lodash/objects/assign',
    'lodash/utilities/identity',
    'text!common/views/commercial/creatives/logo-header.html',
    'text!common/views/commercial/creatives/logo-link.html',
    'text!common/views/commercial/creatives/logo-about.html',
    'text!common/views/commercial/creatives/manual-inline-button.html',
    'text!common/views/commercial/creatives/gimbap/gimbap-simple-blob.html',
    'text!common/views/commercial/creatives/gimbap/gimbap-richmedia-blob.html',
    'text!common/views/commercial/creatives/manual-card.html',
    'text!common/views/commercial/creatives/manual-card-large.html',
    'text!common/views/commercial/creatives/manual-card-cta.html',
    'text!common/views/commercial/creatives/manual-container-button.html',
    'text!common/views/commercial/creatives/manual-container-cta.html',
    'text!common/views/commercial/creatives/manual-container-cta-soulmates.html',
    'text!common/views/commercial/creatives/manual-container-cta-membership.html'
], function (
    svgs,
    config,
    template,
    assign,
    identity,
    logoHeaderStr,
    logoLinkStr,
    logoAboutStr,
    manualInlineButtonStr,
    gimbapSimpleStr,
    gimbapRichmediaStr,
    manualCardStr,
    manualCardLargeStr,
    manualCardCtaStr,
    manualContainerButtonStr,
    manualContainerCtaStr,
    manualContainerCtaSoulmatesStr,
    manualContainerCtaMembershipStr
) {
    var logoAboutTpl;
    var logoLinkTpl;
    var logoHeaderTpl;
    var manualInlineButtonTpl;
    var gimbapSimpleTpl;
    var gimbapRichmediaTpl;
    var manualCardStrs = {
        'manual-card': manualCardStr,
        'manual-card-large': manualCardLargeStr
    };
    var manualCardTpls = {};
    var manualCardCtaTpl;
    var manualContainerButtonTpl;
    var manualContainerCtaTpl;
    var manualContainerCtaSoulmatesTpl;
    var manualContainerCtaMembershipTpl;

    function preprocessLogo(params) {
        logoHeaderTpl || (logoHeaderTpl = template(logoHeaderStr));
        logoLinkTpl || (logoLinkTpl = template(logoLinkStr));
        logoAboutTpl || (logoAboutTpl = template(logoAboutStr));
        if (params.type === 'ad-feature') {
            params.header = logoHeaderTpl({ header: 'Paid for by' });
            params.logo = logoLinkTpl(params);
            params.partners = '';
            params.aboutLink = '';
        } else if (params.type === 'sponsored') {
            params.header = logoHeaderTpl({ header: 'Supported by' });
            params.logo = logoLinkTpl(params);
            params.partners = '';
            params.aboutLink = logoAboutTpl(params);
        } else if (params.type === 'funded'){
            params.header = logoHeaderTpl({
                header: !config.page.isFront && config.page.sponsorshipTag ?
                    config.page.sponsorshipTag + ' is supported by' :
                    'Supported by'
            });
            params.logo = logoLinkTpl(params);
            params.partners = !params.hasPartners ? '' :
                logoHeaderTpl({ header: 'In partnership with:' }) +
                logoLinkTpl({
                    clickMacro: params.clickMacro,
                    logoUrl: params.partnerOneLogoUrl,
                    logoImage: params.partnerOneLogoImage }) +
                logoLinkTpl({
                    clickMacro: params.clickMacro,
                    logoUrl: params.partnerTwoLogoUrl,
                    logoImage: params.partnerTwoLogoImage });
            params.aboutLink = logoAboutTpl(params);
        }
    }

    function preprocessManualInline(params) {
        if (!manualInlineButtonTpl) {
            manualInlineButtonTpl = template(manualInlineButtonStr);
        }
        // having a button is the default state, that is why we expressely
        // test for when *not* to display one
        params.offerButton = params.show_button === 'no' ?
            '' :
            manualInlineButtonTpl(params);
    }

    function preprocessGimbap(params) {
        params.headless = params.headless === 'true';

        // SVGs
        params.marque36icon = svgs('marque36icon', ['gimbap-wrap__mainlogo']);
        params.inlineQuote = svgs('quoteIcon', ['gimbap__quote']);
        params.arrowRight = (params.linksWithArrows.indexOf('yes') !== -1) ? svgs('arrowRight', ['gimbap__arrow']) : '';

        // Make sure we include right logo to the right card
        params.offer1logo = params['logo' + params.offer1tone + 'horizontal'];
        params.offer2logo = params['logo' + params.offer2tone + 'horizontal'];
        params.offer3logo = params['logo' + params.offer3tone + 'horizontal'];
        params.offer4logo = params['logo' + params.offer4tone + 'horizontal'];

        params.gimbapLogoStyle = (params.style === 'reversed') ? ' gimbap-logo--reversed': '';

        // Include quotes into title only if it is allowed in DFP line item
        params.offer1HasQuotes = (params.offer1quotes.indexOf('yes') !== -1) ? params.inlineQuote : '';
        params.offer2HasQuotes = (params.offer2quotes.indexOf('yes') !== -1) ? params.inlineQuote : '';
        params.offer3HasQuotes = (params.offer3quotes.indexOf('yes') !== -1) ? params.inlineQuote : '';
        params.offer4HasQuotes = (params.offer4quotes.indexOf('yes') !== -1) ? params.inlineQuote : '';

        // Test for Author image
        params.hasAuthorImage = params.offer1authorimage
                                        && params.offer1authorimage.length > 0
                                        && params.layout !== '1x1x1x1';
    }

    function preprocessGimbapSimple(params) {
        if (!gimbapSimpleTpl) {
            gimbapSimpleTpl = template(gimbapSimpleStr);
        }
        // SVGs
        params.marque36icon = svgs('marque36icon', ['gimbap-wrap__mainlogo']);
        params.arrowRight = (params.linksWithArrows.indexOf('yes') !== -1) ? svgs('arrowRight', ['gimbap__arrow', 'gimbap__arrow--styled']) : '';
        params.logo = params['logo' + params.componenttone + 'horizontal'];

        params.gimbapEffects = params.componenteffects === 'yes' ? ' ' + 'gimbap--effects' : '';

        params.gimbapSimple = '';
        for (var i = 1; i <= 4; i++) {
            params.gimbapSimple += gimbapSimpleTpl(assign(params, {
                offerurl: params['offer' + i + 'url'],
                offertitle: params['offer' + i + 'title'],
                offerimage: params['offer' + i + 'image']
            }));
        }
    }

    function preprocessManualContainer(params) {
        var stems = {
            jobs: 'job',
            books: 'book',
            masterclasses: 'masterclass',
            travels: 'travel',
            soulmates: 'soulmate',
            subscriptions: 'subscription',
            networks: 'network'
        };
        manualContainerButtonTpl || (manualContainerButtonTpl = template(manualContainerButtonStr));
        manualCardTpls[params.creativeCard] || (manualCardTpls[params.creativeCard] = template(manualCardStrs[params.creativeCard]));
        manualCardCtaTpl || (manualCardCtaTpl = template(manualCardCtaStr));
        params.classNames = ['manual'].concat(params.classNames).map(function (cn) { return 'adverts--' + cn; }).join(' ');
        params.title || (params.title = '');

        if (params.isSoulmates) {
            manualContainerCtaSoulmatesTpl || (manualContainerCtaSoulmatesTpl = template(manualContainerCtaSoulmatesStr));
            params.title = params.marque54icon + params.logosoulmates + '<span class="u-h">The Guardian Soulmates</span>';
            params.blurb = 'Meet someone <em>worth</em> meeting';
            params.ctas = manualContainerCtaSoulmatesTpl(params);

        } else if (params.isMembership) {
            manualContainerCtaMembershipTpl || (manualContainerCtaMembershipTpl = template(manualContainerCtaMembershipStr));
            params.blurb = params.title;
            params.title = params.logomembership + '<span class="u-h">The Guardian Membership</span>';
            params.ctas = manualContainerCtaMembershipTpl(params);

        } else if (params.type !== 'inline'){
            manualContainerCtaTpl || (manualContainerCtaTpl = template(manualContainerCtaStr));
            params.title = params.marque54icon + params.logoguardian + '<span class="u-h">The Guardian</span>' + params.title;
            params.blurb = params.explainer || '';
            params.ctas = params.viewalltext ? manualContainerCtaTpl(params) : '';

        } else {
            params.title = params.marque36icon + params.component_title;
            params.blurb = params.ctas = '';
        }

        if (params.type === 'multiple') {
            params.row = true;
            params.innards = [1, 2, 3, 4].map(function(index) {
                return params['offer' + index + 'url'] ? manualCardTpls[params.creativeCard]({
                    clickMacro:          params.clickMacro,
                    offerUrl:            params['offer' + index + 'url'],
                    offerImage:          params['offer' + index + 'image'],
                    offerTitle:          params['offer' + index + 'title'],
                    offerText:           params['offer' + index + 'meta'],
                    cta:                 params.showCtaLink !== 'hide-cta-link' && (params['offer' + index + 'linktext'] || params.offerLinkText) ? manualCardCtaTpl({
                        offerLinkText:       params['offer' + index + 'linktext'] || params.offerLinkText,
                        arrowRight:          params.arrowRight,
                        classNames:          ''
                    }) : '',
                    classNames:          [index > 2 ? 'hide-until-tablet' : ''].concat(['manual', params.toneClass.replace('commercial--tone-', '')].map(function (cn) { return 'advert--' + (stems[cn] || cn); })).join(' ')
                }) : null;
            }).filter(identity).join('');
        } else if (params.type === 'single') {
            params.row = true;
            params.innards = manualCardTpls[params.creativeCard]({
                clickMacro:          params.clickMacro,
                offerUrl:            params.offerUrl,
                offerImage:          params.offerImage,
                offerTitle:          params.offerTitle,
                offerText:           params.offerText,
                cta:                 params.showCtaLink !== 'hide-cta-link' && params.viewAllText ? manualCardCtaTpl({
                    offerLinkText:       params.viewAllText,
                    arrowRight:          params.arrowRight,
                    classNames:          'button--tertiary'
                }) : '',
                classNames:          ['single', 'landscape', 'large', 'inverse', params.toneClass.replace('commercial--tone-', '')].map(function (cn) { return 'advert--' + (stems[cn] || cn); }).join(' ')
            }) + manualContainerButtonTpl({
                baseUrl:             params.baseUrl,
                clickMacro:          params.clickMacro,
                offerLinkText:       params.offerLinkText,
                arrowRight:          params.arrowRight
            });
        } else {
            params.row = false;
            params.innards = manualCardTpls[params.creativeCard]({
                clickMacro:          params.clickMacro,
                offerUrl:            params.offerUrl,
                offerImage:          params.offerImage,
                offerTitle:          params.offerTitle,
                offerText:           params.offerText,
                cta:                 params.show_button === 'no' ? '' : manualCardCtaTpl({
                    offerLinkText:       'Click here',
                    arrowRight:          params.arrowRight,
                    classNames:          'button--primary'
                }),
                classNames:          ['inline', params.toneClass.replace('commercial--tone-', '')].map(function (cn) { return 'advert--' + (stems[cn] || cn); }).join(' ')
            });
        }
    }

    function preprocessGimbapRichmedia(params) {
        if (!gimbapRichmediaTpl) {
            gimbapRichmediaTpl = template(gimbapRichmediaStr);
        }
        // SVGs
        params.marque36icon = svgs('marque36icon', ['gimbap-wrap__mainlogo']);
        params.logo = params['logo' + params.componenttone + 'horizontal'];
        params.iconClock = svgs('iconClock', ['gimbap-richmedia__icon']);
        params.iconLocation = svgs('iconLocation', ['gimbap-richmedia__icon']);
        params.iconBasket = svgs('iconBasket', ['gimbap-richmedia__icon']);

        params.gimbapEffects = params.componenteffects === 'yes' ? ' ' + 'gimbap--effects' : '';

        params.gimbapRichmedia = '';
        for (var i = 1; i <= 2; i++) {
            params.gimbapRichmedia += gimbapRichmediaTpl(assign(params, {
                offerurl: params['offer' + i + 'url'],
                offertitle: params['offer' + i + 'title'],
                offerimage: params['offer' + i + 'image'],
                offerHighlight: params['offer' + i + 'highlight'],
                offerTitle: params['offer' + i + 'title'],
                offerHeadline: params['offer' + i + 'headline'],
                offerDate: params['offer' + i + 'date'],
                offerPlace: params['offer' + i + 'place'],
                offerPrice: params['offer' + i + 'price'] !== '0' ? params['offer' + i + 'price'] : '',
                offerDiscount: params['offer' + i + 'discount']
            }));
        }
    }

    return {
        'logo': preprocessLogo,
        'manual-inline': preprocessManualInline,
        'gimbap': preprocessGimbap,
        'gimbap-simple': preprocessGimbapSimple,
        'gimbap-richmedia': preprocessGimbapRichmedia,
        'manual-container': preprocessManualContainer
    };
});
