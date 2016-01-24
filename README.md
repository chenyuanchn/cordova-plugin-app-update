## cordova-plugin-app-update ##

###感谢
		本代码是根据south-pacific/cordova-plugin-updateapp和vaenow/cordova-plugin-app-update的插件进行的修改
		有更新需要的同学直接调用这两个插件，本插件仅测试
		主要功能是：进行检查更新时从服务器读取version.xml 进行版本检查，
		更新时下载ftp上的apk文件，下载时通知栏进度条，下载完成后点击通知栏安装
	
###准备工作

 - 这里我默认环境已经安装完毕，只需要进行插件安装即可
		

###Cordova/Phonegap 安装 （仅支持Android）

 1. 将cordova-plugin-app-update插件放在本地目录,将这个目录标记为`$PLUGIN_DIR`

 2. 添加插件: 

		cordova plugin add $PLUGIN_DIR
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
    <url>http://xxx.xxx.xxx.xxx:xxxx/xx/xx/xx.apk</url>
</update>
```
2.发布version.xml和apk到服务器，这里我使用ftp上传到http://xxx.xxx.xxx.xxx:xxxx/xx/xx/目录下

3.js调用插件方法
```js
var updateUrl = "http://xxx.xxx.xxx.xxx:xxxx/xx/xx/version.xml";
AppUpdate.checkAppUpdate(function(e) {}, function(e) {}, updateUrl);
```