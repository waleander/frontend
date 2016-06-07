module.exports = function (grunt, options) {
    return {
        js: {
            src: [
                options.staticTargetDir + 'javascripts/*.js',
                options.staticTargetDir + 'javascripts/bootstraps/**/*.js'
            ]
        }
    };
};
