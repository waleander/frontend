define(
    [
        'common/utils/config'
    ], function (
        config
    ) {
        return function () {
            this.id = 'HideEvenComments';
            this.start = '2016-05-15';
            this.expiry = '2016-06-05';
            this.author = 'Nathaniel Bennett';
            this.description = 'Hide comments for a percentage of users to determine what effect it has on their dwell time and loyalty ';
            this.audience = 0.1;
            this.audienceOffset = 0.5;
            this.successMeasure = 'We want to guage how valuable comments actually are to us';
            this.audienceCriteria = 'All users';
            this.dataLinkNames = '';
            this.idealOutcome = 'DO we want to turn comments up or down';

            this.canRun = function () {
                return true;
            };

            this.variants [
                {
                    id: 'hide-comments',
                    test: function(){ /*Shizzle goes dizzle*/ }
                },
                {
                    id: 'control',
                    test: function(){}
                }

            ]
        }
    }
);
