define([
    'qwery',
    'common/utils/config'
], function (
    qwery,
    config
) {
    return function () {
        this.id = 'VideoMainMediaToThirdItem';
        this.start = '2016-06-08';
        this.expiry = '2016-06-17';
        this.author = 'Akash Askoolum';
        this.description = 'Test moving main media to third item. Does gaining context lead to more starts?';
        this.showForSensitive = true;
        this.audience = 1;
        this.audienceOffset = 0;
        this.successMeasure = '';
        this.audienceCriteria = 'Articles with news tone videos as main media';
        this.dataLinkNames = '';
        this.idealOutcome = '';

        this.canRun = function () {
            return config.page.contentType === 'Article'
                && qwery('[data-component="main video"]').length > 0
                && qwery('.content__article-body p').length >= 3;
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
