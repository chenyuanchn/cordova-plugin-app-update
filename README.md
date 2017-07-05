## cordova-plugin-app-update ##

###主要功能

 - 进行检查更新时从服务器读取version.xml 进行版本检查，
 	 更新时下载ftp上的apk文件，下载时通知栏进度条，下载完成后点击通知栏安装
	
###准备工作

 - 这里我默认环境已经安装完毕，只需要进行插件安装即可
		
###Cordova/Phonegap 安装 （仅支持Android）

   cordova plugin add https://github.com/chenyuanchn/cordova-plugin-app-update.git
   
### 支持平台

		Android only
		
### Android API

+ 检测更新软件 API
    		
1.编写version.xml
```xml
<update>
    <version>1</version>
    <name>my app name</name>
    <title>新版本：0.1</title>
    <description>Test to the latest version, please update!</description>
    <url>http://yyy.yyy.yyy.yyy/test.apk</url>
</update>
```
2.发布version.xml到http://xxx.xxx.xxx.xxx/目录下
	发布apk到http://yyy.yyy.yyy.yyy/目录下

3.js调用插件方法
```js
var updateUrl = "http://xxx.xxx.xxx.xxx/version.xml";
cordova.plugins.AppUpdate.checkAppUpdate(updateUrl,function(e){},function(e){});
```
