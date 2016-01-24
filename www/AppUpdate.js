var cordova = require('cordova'),exec = require('cordova/exec');

var AppUpdate = function() {
};

AppUpdate.prototype = {
		
		checkAppUpdate : function(success, error, updateUrl) {
			updateUrl = updateUrl ? [updateUrl] : [];
			exec(success, error, 'AppUpdate', 'checkAppUpdate', updateUrl);
		}		
}

        
        
        
var appUpdate = new AppUpdate();
module.exports = appUpdate;

