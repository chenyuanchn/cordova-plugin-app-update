package com.cordova.appUpdate;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

public class AppUpdate extends CordovaPlugin {
	private UpdateManager updateManager;

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		if ("checkAppUpdate".equals(action)) {
			if (updateManager != null && updateManager.isCheckFlag()) {
				return true;
			}
			if (args.length() == 0) {
				callbackContext.error("请传入正确地址");
				return false;
			} else {
				String updateUrl = args.getString(0);
				updateManager = new UpdateManager(cordova, updateUrl);
			}
			updateManager.setCheckFlag(true);
			updateManager.checkUpdate();
			callbackContext.success();
			return true;
		} else {
			callbackContext.error("no such method:" + action);
			return false;
		}
	}

}
