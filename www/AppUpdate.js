var exec = require('cordova/exec');

exports.checkAppUpdate = function(updateUrl, success, error) {
    exec(success, error, "AppUpdate", "checkAppUpdate", [updateUrl]);
};



