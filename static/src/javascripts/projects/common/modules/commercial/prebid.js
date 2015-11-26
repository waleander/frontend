define([
	'common/modules/commercial/dfp-api'
], function (
	dfp
) {
    var PREBID_TIMEOUT = 1000;
 	
    function init() {
    	setTimeout(initAdserver, PREBID_TIMEOUT);
    }

    function initAdserver() {
    	dfp.init();
    }

 	return {
 		init: init
 	};
});