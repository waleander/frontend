define([
    'common/utils/config'
], function (
    config
) {
    return function () {

        this.id = 'YoutubeTest';
        this.start = '2016-05-6';
        this.expiry = '2016-06-06';
        this.author = 'Chris J Clarke';
        this.description = 'Replace one video example with YTEP (Qualative test with remote users)';
        this.audience = 0;
        this.audienceOffset = 0;
        this.successMeasure = '';
        this.showForSensitive = true;
        this.audienceCriteria = '';
        this.dataLinkNames = '';
        this.idealOutcome = '';

        this.canRun = function () {
            return config.page.contentType === 'Video' && config.page.seriesTags;
        };

        this.variants = [
            {
                id: 'control',
                test: function () {}
            },
            {
                id: 'variant',
                test: function () {}
            }
        ];
    };
});
