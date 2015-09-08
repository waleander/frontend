define([
    'common/utils/config',
    'common/utils/detect',
    'common/utils/storage'
], function (
    config,
    detect,
    storage
) {

    var messageId = 'fronts-ipad';

    return function () {

        this.id = 'IpadCoreFronts';
        this.start = '2015-09-08';
        this.expiry = '2015-10-31';
        this.author = 'Justin Pinner';
        this.description = 'Test if serving ipad users the core experience for fronts increases visit time';
        this.audience = 10;
        this.audienceOffset = 0;
        this.successMeasure = 'iPad users will still be on site after one minute';
        this.audienceCriteria = 'nn% of iPad users will see the core version of fronts';
        this.dataLinkNames = '';
        this.idealOutcome = 'Fewer iPad users will crash on fronts.';

        this.canRun = function () {
            // is an iPad2 or later with iOS 6,7 or 8
            return navigator.platform === 'iPad'
                && window.devicePixelRatio === 2
                && /.*iPad; CPU OS ([678])_\d+.*/.test(navigator.userAgent);
        };

        this.variants = [
            {
                id: 'A',
                test: store.local.set("ab-fronts-ipad", "")
            },
            {
                id: 'B',
                test: store.local.set("ab-fronts-ipad", "core")
            }
        ];

    };

});
