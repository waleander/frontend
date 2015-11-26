define(['helpers/injector'], function (Injector) {
    var injector = new Injector(),
        clock, dfp;

    describe('Prebid service: ', function () {
        beforeEach(function (done) {
            injector.require([
                'common/modules/commercial/prebid',
                'common/modules/commercial/dfp-api'
            ], function () {
                prebid = arguments[0];
                dfp = arguments[1];

                clock = sinon.useFakeTimers();

                done();
            });
        });

        afterEach(function () {
            clock.restore();
        });

        it('should give ad server 1000ms for response', function () {
            spyOn(dfp, 'init');

            clock.tick(0);
            prebid.init();
            clock.tick(999);
            expect(dfp.init).not.toHaveBeenCalled();
        });

        it('should init DFP after 1000ms', function () {
            spyOn(dfp, 'init');

            clock.tick(0);
            prebid.init();
            clock.tick(1001);
            expect(dfp.init).toHaveBeenCalled();
        });
    });
});