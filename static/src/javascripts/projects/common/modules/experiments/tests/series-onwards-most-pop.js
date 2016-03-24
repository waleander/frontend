define([
    'common/utils/$',
    'common/utils/config',
    'common/utils/detect'
], function (
    $,
    config,
    detect
) {
    return function () {

        this.id = 'SeriesOnwardsMostPop';
        this.start = '2016-03-23';
        this.expiry = '2016-04-20';
        this.author = 'Gareth Trufitt';
        this.description = '';
        this.audience = 0.0;
        this.audienceOffset = 0.0;
        this.successMeasure = '';
        this.audienceCriteria = '';
        this.dataLinkNames = '';
        this.idealOutcome = '';

        this.canRun = function () {
            return true;
        };

        var series = {
            seriesName: [
                {
                    headline: ,
                    url: ,
                    image:,
                    date
                }
            ]
        }

        this.variants = [
            {
                id: 'control',
                test: function () {}
            }, {
                id: 'variant',
                test: function () {

                }
            }
        ];
    };
});
