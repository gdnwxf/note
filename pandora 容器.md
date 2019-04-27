## pandora 容器

>  核心jar 文件 taobao-hsf.sar-dev-SNAPSHOT.jar 该jar文件位于 taobao-hsf.sar 中的 root jar文件

![image-20190423111208859](/Users/wch/opensource/note/assets/image-20190423111208859.png)

##### 关键调用路径

com.taobao.pandora.boot.PandoraBootstrap#run

```java
public static void run(String[] args) {
    if (argsConatinPandoraLocation(args)) {
        AnsiLog.error("[ERROR] Please don't set -Dpandora.location in Program arguments, set in VM arguments. Google 'Program arguments VM arguments'.");
    }

    PandoraLazyExportUtils.tryEnablePandoraLazyExport();
    VersionUtils.setPandoraBootStarterVersionSystemProperty();
    if (SarLoaderUtils.unneedLoadSar()) { //第二次会调用 第二次无需加载sar了 在reLanuch的时候调用的
        LogConfigUtil.initLoggingSystem();
    } else {
        URL[] urls = ClassLoaderUtils.getUrls(PandoraBootstrap.class.getClassLoader());
        if (urls == null) {
            throw new IllegalStateException("Can not find urls from the ClassLoader of PandoraBootstrap. ClassLoader: " + PandoraBootstrap.class.getClassLoader());
        } else {
            urls = AutoConfigWrapper.autoConfig(urls);
            ReLaunchMainLauncher.launch(args, deduceMainApplicationClass().getName(), urls);
        }
    }
}
```

-> com.taobao.pandora.boot.loader.util.ClassLoaderUtils#getUrls

->com.taobao.pandora.boot.loader.ReLaunchMainLauncher#launch

​	->reLaunch(args, mainClass, createClassLoader(urls)); System.exit(0); //relaunch之后就退出了

​		-> 实例化pandora的classloader

```java
static ClassLoader createClassLoader(URL[] urls) {
    long t1 = System.nanoTime();
    Health.registMBean();
    SystemPrintUtil.switchSystemPrint();
  // 实例化 reLaunchURLClassLoader
  // (父)ExtClassLoader -> (子) ReLaunchURLClassLoader 
  // 传入ClassLoader.getSystemClassLoader().getParent() 的作用是获取到rootClassLoader
    ReLaunchURLClassLoader reLaunchClassLoader = new ReLaunchURLClassLoader(cleanJavaAgentUrls(urls),ClassLoader.getSystemClassLoader().getParent()); 

    try {
      // 通过配置查询 pandoraLocation = System.getProperty("pandora.location");查找taobao-hsf.sar-dev-SNAPSHOT.jar 
        Archive sar = SarLoaderUtils.findExternalSar();
        if (sar == null) {
          // 如果找不到从
            sar = SarLoaderUtils.findFromClassPath(urls);
            if (sar == null) {
                if ("true".equalsIgnoreCase(System.getProperty("pandora.boot.failFast"))) {
                    throw new RuntimeException("can not load taobao-hsf.sar, please check your config!");
                }

                AnsiLog.error("Can not load taobao-hsf.sar! If you do not use taobao-hsf.sar, ignore this. Otherwise please check '-Dpandora.location=' or maven dependencies if there contains taobao-hsf.sar!");
            }
        }

        if (sar != null) {
            Map<String, Class<?>> classCache = SarLoaderUtils.getClassCache(sar,reLaunchClassLoader); // 将pandora的加载出来的class放到的reLaunchClassLoader的缓存中
            reLaunchClassLoader.setClassCache(classCache);
        }

        SarLoaderUtils.markSarLoaderUtils(reLaunchClassLoader, "sarLoaded", true);
        SarLoaderUtils.markSarLoaderUtils(reLaunchClassLoader, "t1", t1);
        reLaunchClassLoader.collectStaticClassInfo();//将classinfo 实际是的jar 文件url放到 reLaunchClassLoader的url 中
        return reLaunchClassLoader;
    } catch (Exception var6) {
        throw new RuntimeException("load pandora error!", var6);
    }
}
```

 #### getClassCache的实现

```java
public static Map<String, Class<?>> getClassCache(Archive sar, ClassLoader bizClassLoader) throws Exception {
    JarFile.registerUrlProtocolHandler();
    printBanner(bizClassLoader);
    configHostType();
    configTddlVersionCheck();
    configureHeadlessProperty();
    Map<String, Archive> pluginsFromSar = loadPlugins(sar);
    List<URL> pluginJarUrls = Collections.emptyList();
    if (!ignorePackagedPlugins()) {
        pluginJarUrls = new ArrayList();
        Enumeration pluginPropertiesResources = bizClassLoader.getResources("com/taobao/pandora/plugin.guide.properties");

        while(pluginPropertiesResources.hasMoreElements()) {
            URL guidePropertiesUrl = (URL)pluginPropertiesResources.nextElement();
            String pluginArtifactId = readPluginArtifactId(guidePropertiesUrl);
            if (pluginArtifactId != null && !pluginsFromSar.containsKey(pluginArtifactId)) {
                ((List)pluginJarUrls).add(ArchiveUtils.createArchiveFromUrl(guidePropertiesUrl).getUrl());
            }
        }
    }

    URL url = sar.getUrl();
    List<Archive> jars = sar.getNestedArchives(new EntryFilter() {
        public boolean matches(Entry entry) {
            String entryName = entry.getName();
            if (entryName.length() > "lib/".length() && entryName.charAt("lib/".length()) == '.') {
                System.out.println("entryName is a hidden directory in sar, ignore: " + entryName);
                return false;
            } else {
                return !entry.isDirectory() && entryName.startsWith("lib/");
            }
        }
    });
    URL[] urls = new URL[jars.size()];

    for(int i = 0; i < jars.size(); ++i) {
        urls[i] = ((Archive)jars.get(i)).getUrl();
    }

    ClassLoader classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader().getParent());
    String defaultInitOverride = System.getProperty("log4j.defaultInitOverride");
    if (defaultInitOverride == null) {
        System.out.println("Set log4j.defaultInitOverride to true.");
        System.setProperty("log4j.defaultInitOverride", "true");
    }
    //保存当前线程的classloader
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    Map var14;
    try {
      //使用pandoraContainer来代理加载
        URL[] pluginUrls = (URL[])((List)pluginJarUrls).toArray(new URL[((List)pluginJarUrls).size()]);
        Class<?> pandora = classLoader.loadClass("com.taobao.pandora.PandoraContainer");
      //传入 pandora的classloader
        Thread.currentThread().setContextClassLoader(pandora.getClassLoader());
        Constructor<?> constructor = pandora.getConstructor(URL.class, URL[].class, ClassLoader.class);
        Object instance = constructor.newInstance(url, pluginUrls, bizClassLoader);
      // 调用pandora去加载
        invokeStart(instance);
      // 校验加载的文件的信息
        checkFileDescriptorCount();
      // 加载出class文件 并放入Map<className, class> 
        var14 = invokeGetExportedClasses(instance);
    } finally {
      // pandora 加载完后将原来的classloader放入
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    return var14;
}
```

​		->  实例化

```java
public static void reLaunch(String[] args, String mainClass, ClassLoader classLoader) {
  // 隔离线程组 
    IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(mainClass);
  // 启动一个线程去再次掉用main方法去加载spring的东西 
    Thread launchThread = new Thread(threadGroup, new LaunchRunner(mainClass, args),"main");
    launchThread.setContextClassLoader(classLoader); //
    launchThread.start();
    LaunchRunner.join(threadGroup);
    threadGroup.rethrowUncaughtException();
}
```

 PandoraBootstrap.markStartupAndWait(); 

//标记服务启动完成,并设置线程 wait。防止业务代码运行完毕退出后，导致容器退出。

```java
public static void markStartupAndWait() {
    long t1 = SarLoaderUtils.t1(); //统计开始启动时间
    long t2 = System.nanoTime(); //统计结束启动时间
    AnsiLog.info("Service(pandora boot) startup in " + (t2 - t1) / 1000000L + " ms");
    Health.markStartup(); // 健康检查
    if ("true".equalsIgnoreCase(System.getProperty("pandora.boot.wait", "true"))) {
        try {
            Health.markAwait();
            AnsiLog.info("Service(pandora boot) receive shutdown command, ready to shutdown...");
        } catch (InterruptedException var5) {
            throw new RuntimeException(var5);
        }
    }

    LogConfigUtil.destoryLoggingSystem();// 销毁日志系统
    System.exit(0);
}
```

