import _ from 'common/utils/_';
import Injector from 'helpers/injector';

describe('Active AB test configurations', function() {
    var testsPromise = new Promise(function(resolve, reject) {
        var injector = new Injector();
        injector.test(['common/modules/experiments/ab'], function() {
            var abSystem = arguments[0];
            var tests = abSystem.getActiveTests();
            if (tests.length > 0) {
                resolve(tests);
            } else {
                reject(Error("AB tests load error"));
            }
        });
    });

    function testId(abTest) {
        console.log('test ID for ' + abTest.id + ' should ' + ((abTest.id.length > 0) ? 'pass' : 'fail'));
        describe('AB test: ' + abTest.id, function() {
            it('AB test: ' + abTest.id + ' should not have a zero-length ID', function() {
                expect(abTest.id.length).toBeGreaterThan(0);
            });
        });
    }

    function testDescription(abTest) {
        console.log('test description for ' + abTest.id + ' should ' + ((abTest.description.length > 0) ? 'pass' : 'fail'));
        describe('AB test: ' + abTest.id, function() {
            it('AB test: ' + abTest.id + ' should have a description', function() {
                expect(abTest.description.length).toBeGreaterThan(0);
            });
        });
    }

    function testAudience(abTest) {
        console.log('test audience for ' + abTest.id + ' should ' + ((abTest.audience > 0) ? 'pass' : 'fail'));
        describe('AB test: ' + abTest.id, function() {
            it('AB test: ' + abTest.id + ' should have a description', function() {
                expect(abTest.audience).toBeGreaterThan(0);
            });
        });
    }

    function testEverything(allTests) {
        _.forEach(allTests, function(t) {
            testId(t);
            testDescription(t);
            testAudience(t);
        });
    }

    it('should test all the AB tests', function() {
        testsPromise.then(function(abTests) {
            testEverything(abTests);
        }, function(err) {
            console.log(err);
        });
    });

});
