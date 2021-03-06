## GAutomatorView

![symbol](https://img.shields.io/badge/qintianchen-GautomatorView-orange)

GAutomator的辅助工具GAutomatorView的个人开发版本，基于javaFX，代码全部开源。它可以帮助你获取游戏中的控件信息。当前仅支持Unity游戏，而且使用工具的时候，游戏需要运行在一个Android设备或者模拟器上。腾讯推荐的GAView不仅不开源，而且有一些问题。比如在一台主机上连接着多台Android设备的时候，会出现连接错误的问题。这也是我打算自己写一个开源版本的原因。

使用的时候，先获取设备列表，然后再同步游戏状态。

为了方便测试，这个[仓库 - UnityDemo-TestGAutomatorView](https://github.com/qintianchen/UnityDemo-TestGAutomatorView)是一个现成的Unity项目，里面有一个已经构建好的[apk](https://github.com/qintianchen/UnityDemo-TestGAutomatorView/blob/master/Demo.apk)。你可以选择下载这个apk，安装到安卓设备上进行测试。如下图所示：

![图片一](./Doc/Pictures/3.png)

## Build Runnable Jar

这个版本基于java 8 + intellij idea开发。java 11以前的版本sdk里面都是自带javaFX的库，如果你用的是java 11（>=）的版本，需要自行去官网下载javaFX的库导入并按教程配置configuration。

生成可执行jar的方法如下：

1. File
2. Project Structure
3. Artifacts
4. \+
5. Jar
6. From modules with dependences 
7. 选择sample.Main主类，选择Extract to target Jar，Apply
8. Build
9. Build Artifacts

如果你配置了java 8的环境变量，就可以直接点击jar文件执行了。否则就需要一个整合了java运行环境的exe，但是那玩意儿确实太大了。

## Build Exe

1. File
2. Project Structure
3. Artifacts
4. \+
5. JavaFxApplication/From modules...
6. Output Layout 中添加引用第三方库(libs 目录)
7. JavaFx 标签页下设置Application Class为sample.Main，设置Native Bundle为all
8. Build
9. Build Artifacts

Build好Exe之后，拷贝[exe文件的父级目录](./out/artifacts/GAutomatorView_javafx/bundles/GAutomatorView_javafx)到任何地方，然后运行里面的Exe文件就行了。

正如上面所说的一样，这个目录99%的大小都是打包进里面的Java运行时贡献的，剩下1%才是真正的主体工程。

## 后话

第一版是2020五一五天假期抽空写的。第一天用WPF写，最后竟然卡在了TreeView不知道如何遍历元素的问题上；

第二第三天经过调研，于是临时学了html,css,javascript，然后又学了vue.js，打算用electron+vue.js写一套，卡在了构建环境的问题上，不仅如此，javascript弱类型，原型链设计模式，ES6对模块化编程的支持让我很感到蛋疼。

最后两天终于像javaFX妥协，不过据说Oracle到2022年就不再对javaFX进行商业支持了。无论如何，学习javaFX只是为了解决燃眉之急，现在大型的软件开发几乎都是使用QT开发，而一些轻量级的软件则用类似electron这样的技术，毕竟html + css那套绘制一套漂亮的客户端太方便了。等有时间，会好好抽空学学前端技术，再用electron做一个版本。