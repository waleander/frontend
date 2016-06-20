define([
    'Promise',
    'common/utils/config',
    'common/utils/template',
    'common/utils/fastdom-promise',
    'common/views/svgs',
    'common/modules/commercial/creatives/template-preprocessor',

    // require templates, so they're bundled up as part of the build
    'text!common/views/commercial/creatives/logo.html',
    'text!common/views/commercial/creatives/manual-inline.html',
    'text!common/views/commercial/creatives/gimbap.html',
    'text!common/views/commercial/creatives/gimbap-simple.html',
    'text!common/views/commercial/creatives/gimbap-richmedia.html',
    'text!common/views/commercial/creatives/manual-container.html'
], function (
    Promise,
    config,
    template,
    fastdom,
    svgs,
    templatePreprocessor
) {
    return Template;

    /**
     * Create simple templated creatives
     *
     * * https://www.google.com/dfp/59666047#delivery/CreateCreativeTemplate/creativeTemplateId=10021527
     * * https://www.google.com/dfp/59666047#delivery/CreateCreativeTemplate/creativeTemplateId=10028127
     */
    function Template(adSlot, params) {
        if (params.Toneclass) {
            params.isSoulmates = params.Toneclass.indexOf('soulmates') !== -1;
            params.isMembership = params.Toneclass.indexOf('membership') !== -1;
            params.HeaderToneclass = 'commercial__header--' + params.Toneclass.replace('commercial--tone-', '');
        }

        params.marque36icon = svgs('marque36icon');
        params.marque54icon = svgs('marque54icon');
        params.logosoulmates = svgs('logosoulmates');
        params.logosoulmatesjoin = svgs('logosoulmatesjoin');
        params.logomembership = svgs('logomembershipwhite');
        params.logosoulmateshorizontal = svgs('logosoulmates');
        params.logomasterclasseshorizontal = svgs('logomasterclasseshorizontal');
        params.logomembershorizontal = svgs('logomembershiphorizontal');
        params.logojobshorizontal = svgs('logojobshorizontal');
        params.logobookshophorizontal = svgs('logobookshophorizontal');
        params.logojobs = svgs('logojobs');
        params.logomasterclasses = svgs('logomasterclasses');
        params.arrowRight = svgs('arrowRight', ['i-right']);
        params.logoguardian = svgs('logoguardian');
        params.marque36iconCreativeMarque = svgs('marque36icon', ['creative__marque']);

        if( params.creative === 'manual-single') {
            params.type = 'single';
            params.creative = 'manual-container';
            params.creativeCard = 'manual-card-large';
            params.classNames = ['legacy', 'legacy-single', params.toneClass.replace('commercial--', ''), params.toneClass.replace('commercial--tone-', '')];
        } else if (params.creative === 'manual-multiple') {
            // harmonise attribute names until we do this on the DFP side
            params.toneClass = params.Toneclass;
            params.baseUrl = params.base__url;
            params.offerLinkText = params.offerlinktext;

            params.type = 'multiple';
            params.creative = 'manual-container';
            params.creativeCard = 'manual-card';
            params.classNames = ['legacy', params.toneClass.replace('commercial--', ''), params.toneClass.replace('commercial--tone-', '')];
        } else if (params.creative === 'manual-inline' && config.switches.refactorInlineComponent) {
            params.omnitureId = params.omniture_id;
            params.toneClass = params.Toneclass;
            params.baseUrl = params.base_url;
            params.offerTitle = params.offer_title;
            params.offerUrl = params.offer_url;
            params.offerImage = params.offer_image;
            params.offerText = params.offer_meta;

            params.creative = 'manual-container';
            params.creativeCard = 'manual-card';
            params.type = 'inline';
            params.classNames = ['legacy-inline', params.toneClass.replace('commercial--', ''), params.toneClass.replace('commercial--tone-', '')];
        } else if (params.creative === 'logo-ad-feature') {
            params.creative = 'logo';
            params.type = 'ad-feature';
        } else if (params.creative === 'logo-sponsored') {
            params.creative = 'logo';
            params.type = 'sponsored';
        }

        return new Promise(function (resolve) {
            require(['text!common/views/commercial/creatives/' + params.creative + '.html'], function (creativeTpl) {
                if (templatePreprocessor[params.creative]) {
                    templatePreprocessor[params.creative](params);
                }

                var creativeHtml = template(creativeTpl, params);

                resolve(fastdom.write(function () {
                    adSlot.insertAdjacentHTML('beforeend', creativeHtml);
                    return true;
                }));
            });
        });
    }
});
