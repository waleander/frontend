define([
    'common/utils/$',
    'common/utils/config',
    'common/utils/fastdom-promise',
    'common/utils/detect',
    'common/utils/proximity-loader',
    'common/modules/onward/inject-container',
    'lodash/utilities/noop'
], function (
    $,
    config,
    fastdom,
    proximityLoader,
    injectContainer,
    noop
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

        this.variants = [
            {
                id: 'control',
                test: function () {}
            }, {
                id: 'variant',
                test: function () {
                    var $onward = $('.js-onward');

                    proximityLoader.add($onward, 1500, function () {
                        fastdom.write(function () {
                            injectContainer.injectContainer('/series/popular/football/series/thefiver.json', $onward, 'inject-popular-series', noop);
                        });
                    });
                }
            }
        ];
    };
});
