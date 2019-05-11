### spring boot 分析

*  通过注解了SpringBootApplication 启动

```java
@SpringBootApplication // that is @Configuration @EnableAutoConfiguration @ComponentScan
@EnableSpringConfigured // for spring-aspects
public class Application 
```

* 执行静态run方法  讲当前的注解了SpringBootApplication 的类作为参数传入

```java
SpringApplication.run(Application.class, args);
```

* 实例化过程

```java
{
  ....
    //实例化applicationArguments参数
  ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
  // 配置环境
  ConfigurableEnvironment environment = prepareEnvironment(listeners,applicationArguments);
  // 打印任务
  Banner printedBanner = printBanner(environment);
  // 创建ApplicationContext
  context = createApplicationContext();
  analyzers = new FailureAnalyzers(context);
  // context.refresh() 前的准备
  // SpringBootApplication 是一个复合注解 (启动类是放到applicationArguments中)
  // BeanDefinitionLoader loader = createBeanDefinitionLoader(getBeanDefinitionRegistry(context), sources);
  // loader.load(); 讲的传入的applicationArguments load到beandefinetion 
  prepareContext(context, environment, listeners, applicationArguments,printedBanner);
  // 执行spring 的abstractApplicationContext的刷新
  //  刷新里面调用关键方法
  // Invoke factory processors registered as beans in the context.
	// invokeBeanFactoryPostProcessors(beanFactory);
  // 调用BeanFactoryPostProcessors处理
  // PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
  // 调用关键方法如下
  refreshContext(context);
  afterRefresh(context, applicationArguments);
  listeners.finished(context, null);
}
```

* createApplicationContext

```java
protected ConfigurableApplicationContext createApplicationContext() {
    Class<?> contextClass = this.applicationContextClass;
    if (contextClass == null) {
        try {
            contextClass = Class.forName(this.webEnvironment 
                                         //判断是否是web应用 实例化不同的 ApplicationContext
                                         ? "org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext" 
                                         // AnnotationConfigApplicationContext 
                                         : "org.springframework.context.annotation.AnnotationConfigApplicationContext");
        } catch (ClassNotFoundException var3) {
            throw new IllegalStateException("Unable create a default ApplicationContext, please specify an ApplicationContextClass", var3);
        }
    }

    return (ConfigurableApplicationContext)BeanUtils.instantiate(contextClass);
}
```

*  prepareContext 方法

```java
private void prepareContext(ConfigurableApplicationContext context,
      ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
      ApplicationArguments applicationArguments, Banner printedBanner) {
   context.setEnvironment(environment);
  // 给context 注入loader
   postProcessApplicationContext(context);
  // 执行ApplicationContextInitializer.initialize 方法
   applyInitializers(context);
  // context 准备 扩展点 里面是空方法
   listeners.contextPrepared(context);
   if (this.logStartupInfo) {
      logStartupInfo(context.getParent() == null);
      logStartupProfileInfo(context);
   }

   // Add boot specific singleton beans
   context.getBeanFactory().registerSingleton("springApplicationArguments",
         applicationArguments);
   if (printedBanner != null) {
      context.getBeanFactory().registerSingleton("springBootBanner", printedBanner);
   }

   // Load the sources
   Set<Object> sources = getSources();
   Assert.notEmpty(sources, "Sources must not be empty");
  // 将启动类 load进 context
   load(context, sources.toArray(new Object[sources.size()]));
  // 将listener 加入到context 中
   listeners.contextLoaded(context);
}
```

* 调用的关键方法

```java
public static void invokeBeanFactoryPostProcessors(
  ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
  ......
    //调用非常重要的 BeanFactoryPostProcessor
    // < ConfigurationClassPostProcessor >
   
  ......
  
}

 public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry){
    //调用此方法
     processConfigBeanDefinitions(registry);
 }
//第三个重要的方法
org.springframework.context.annotation.ConfigurationClassUtils#checkConfigurationClassCandidate 
//里面有个判断 会给之前加载 启动类的beanDefinition加上如下的Attrite
// org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass full的属性
if (isFullConfigurationCandidate(metadata)) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
}
	/**
	  //  该Configuration 是复合注解SpringBootApplication 上注解的信息
	 * Check the given metadata for a full configuration class candidate
	 * (i.e. a class annotated with {@code @Configuration}).
	 * @param metadata the metadata of the annotated class
	 * @return {@code true} if the given class is to be processed as a full
	 * configuration class, including cross-method call interception
	 */
	public static boolean isFullConfigurationCandidate(AnnotationMetadata metadata) {
		return metadata.isAnnotated(Configuration.class.getName());
	}
 
```

* 启动类本身会分装到 org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition

  ![image-20190314000517945](https://github.com/gdnwxf/note/blob/master/assets/image-20190314000517945.png)

* 然后执行.parse(AnnotatedGenericBeanDefinition)的方法

```java
processConfigurationClass(new ConfigurationClass(metadata, beanName));
```



* 转成 Source来处理

```java
SourceClass sourceClass = asSourceClass(configClass);
do {
  //递归处理 里面解析各种注解 ImportResource ComponentScan bean PropertySources 
  sourceClass = doProcessConfigurationClass(configClass, sourceClass);
}
while (sourceClass != null);

this.configurationClasses.put(configClass, configClass);
```

* 具体如下 至此 springboot 的解析封装基本完成

```java
/**
 * Apply processing and build a complete {@link ConfigurationClass} by reading the
 * annotations, members and methods from the source class. This method can be called
 * multiple times as relevant sources are discovered.
 * @param configClass the configuration class being build
 * @param sourceClass a source class
 * @return the superclass, or {@code null} if none found or previously processed
 */
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
   // Recursively process any member (nested) classes first
   processMemberClasses(configClass, sourceClass);

   // Process any @PropertySource annotations
   for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
         sourceClass.getMetadata(), PropertySources.class, org.springframework.context.annotation.PropertySource.class)) {
      if (this.environment instanceof ConfigurableEnvironment) {
         processPropertySource(propertySource);
      }
      else {
         logger.warn("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
               "]. Reason: Environment must implement ConfigurableEnvironment");
      }
   }

   // Process any @ComponentScan annotations
   Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
         sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
   if (!componentScans.isEmpty() && !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
      for (AnnotationAttributes componentScan : componentScans) {
         // The config class is annotated with @ComponentScan -> perform the scan immediately
         Set<BeanDefinitionHolder> scannedBeanDefinitions =
               this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
         // Check the set of scanned definitions for any further config classes and parse recursively if necessary
         for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
            if (ConfigurationClassUtils.checkConfigurationClassCandidate(holder.getBeanDefinition(), this.metadataReaderFactory)) {
               parse(holder.getBeanDefinition().getBeanClassName(), holder.getBeanName());
            }
         }
      }
   }

   // Process any @Import annotations
   processImports(configClass, sourceClass, getImports(sourceClass), true);

   // Process any @ImportResource annotations
   if (sourceClass.getMetadata().isAnnotated(ImportResource.class.getName())) {
      AnnotationAttributes importResource =
            AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
      String[] resources = importResource.getStringArray("locations");
      Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
      for (String resource : resources) {
         String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
         configClass.addImportedResource(resolvedResource, readerClass);
      }
   }

   // Process individual @Bean methods
   Set<MethodMetadata> beanMethods = sourceClass.getMetadata().getAnnotatedMethods(Bean.class.getName());
   for (MethodMetadata methodMetadata : beanMethods) {
      configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
   }

   // Process default methods on interfaces
   processInterfaces(configClass, sourceClass);

   // Process superclass, if any
   if (sourceClass.getMetadata().hasSuperClass()) {
      String superclass = sourceClass.getMetadata().getSuperClassName();
      if (!superclass.startsWith("java") && !this.knownSuperclasses.containsKey(superclass)) {
         this.knownSuperclasses.put(superclass, configClass);
         // Superclass found, return its annotation metadata and recurse
         return sourceClass.getSuperClass();
      }
   }

   // No superclass -> processing is complete
   return null;
}
```

* ApplicationContextInitializer 的作用

```java
// 二维火那边写的ContextInitalizer 就是处理扫描 添加BeanFactoryPostProcessor的处理
public class DubboConfigurationApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public DubboConfigurationApplicationContextInitializer() {
    }

    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String scan = env.getProperty("spring.dubbo.scan");
        if (scan != null) {
            AnnotationBean scanner = (AnnotationBean)BeanUtils.instantiate(AnnotationBean.class);
            scanner.setPackage(scan);
            scanner.setApplicationContext(applicationContext);
            applicationContext.addBeanFactoryPostProcessor(scanner);
            applicationContext.getBeanFactory().addBeanPostProcessor(scanner);
            applicationContext.getBeanFactory().registerSingleton("annotationBean", scanner);
        }

    }
}
```

