@(item: model.MetaData)(implicit request: RequestHeader)

(function (navigator, window) {
    // Enable manual optin to core functionality/optout of enhancement
    var personPrefersCore = function () {
        if (window.location.hash === '#core' || window.location.hash === '#gu.prefs.force-core=on') return true;
        if (window.location.hash === '#nocore' || window.location.hash === '#gu.prefs.force-core=off') return false;
        try {
            var preference = window.localStorage.getItem('gu.prefs.force-core') || 'off';
            return /"value":"on"/.test(preference);
        } catch (e) {
            return false;
        }
    };

    // Guess whether the device is too old, regardless of whether it cuts the mustard
    //
    // 'older' iOS normally indicates a device with lower power (they stop being upgradeable at some point).
    // We won't run all javascript on these.
    // For usage stats see http://david-smith.org/iosversionstats/
    //
    // NOTE: this moves people into a category where they do not get important things such as commenting
    var enhanceForDevice = function () {
        if (navigator.platform === 'iPhone' || navigator.platform === 'iPad' || navigator.platform === 'iPod') {
            if (@item.isFront && (navigator.platform === 'iPad')) {
                return false;
            } else {
                // I'm intentionally being a bit over zealous in the detection department here
                return !(/.*(iPhone|iPad; CPU) OS ([3456])_\d+.*/.test(navigator.userAgent));
            }
        } else {
            return true;
        }
    };

    window.shouldEnhance = !personPrefersCore() && enhanceForDevice();

    window.shouldEnhance || console && console.info && console.info("THIS IS CORE");
})(navigator, window);




