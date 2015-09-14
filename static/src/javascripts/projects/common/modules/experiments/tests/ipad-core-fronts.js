define([], function () {

    return function () {

        this.id = 'IpadCoreFronts';
        this.start = '2015-09-08';
        this.expiry = '2015-10-31';
        this.author = 'Justin Pinner';
        this.description = 'Test if serving ipad users the core experience for fronts extends their visit time';
        this.audience = 0.75;
        this.audienceOffset = 0;
        this.successMeasure = 'iPad users will still be on site after one minute';
        this.audienceCriteria = 'n% of iPad users will see the core version of fronts';
        this.dataLinkNames = '';
        this.idealOutcome = 'Fewer iPad users will crash on fronts.';

        // TODO: REMOVE dev!
        var dev = true;

        var coreOptedIn = function () {
            try {
                var corePref = window.localStorage.getItem('gu.prefs.force-core');
                if (corePref) {
                    if ((JSON.parse(corePref).value) === 'on') {
                        return true;
                    }
                }
                return false;
            } catch (e) {
                return false;
            }
        };

        this.canRun = function () {
            // is an iPad 3 or later with iOS 7, 8 or 9
            // TODO: REMOVE dev!
            return (dev || (navigator.platform === 'iPad'
                && (!coreOptedIn())
                && window.devicePixelRatio === 2
                && /.*iPad; CPU OS ([789])_\d+.*/.test(navigator.userAgent)));
        };

        this.variants = [
            {
                id: 'control',
                test: function () {}
            },
            {
                id: 'core',
                test: function () {}
            }
        ];

    };

});
