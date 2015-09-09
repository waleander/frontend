define([], function () {

    return function () {

        this.id = 'ipad-core-fronts';
        this.start = '2015-09-08';
        this.expiry = '2015-10-31';
        this.author = 'Justin Pinner';
        this.description = 'Test if serving ipad users the core experience for fronts extends their visit time';
        this.audience = 10;
        this.audienceOffset = 0;
        this.successMeasure = 'iPad users will still be on site after one minute';
        this.audienceCriteria = 'n% of iPad users will see the core version of fronts';
        this.dataLinkNames = '';
        this.idealOutcome = 'Fewer iPad users will crash on fronts.';

        this.canRun = function () {
            // is an iPad2 or later with iOS 6,7 or 8
            return navigator.platform === 'iPad'
                && window.devicePixelRatio === 2
                && /.*iPad; CPU OS ([678])_\d+.*/.test(navigator.userAgent)
                //  and is not already opted in to core
                && ((window.localStorage.getItem('gu.prefs.force-core') || 'off') === 'off');
        };

        this.variants = [
            {
                id: 'control',
                test: function () {}
            },
            {
                id: 'A',
                test: function () {}
            }
        ];

    };

});
