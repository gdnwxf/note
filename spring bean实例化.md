### spring 实例化bean 细节

* refresh() 

  > ```java
  > synchronized (this.startupShutdownMonitor) {
  >    // Prepare this context for refreshing.
  >    prepareRefresh();
  > 
  >    // Tell the subclass to refresh the internal bean factory. 实例化   DefaultListBeanFactory
  >    ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
  > 
  >    // Prepare the bean factory for use in this context.
  >    // Prepare beanFactory
  >    prepareBeanFactory(beanFactory);
  > 
  >    try {
  >       // Allows post-processing of the bean factory in context subclasses.
  >       postProcessBeanFactory(beanFactory);
  > 
  >       // Invoke factory processors registered as beans in the context.
  >       // 调用BeanFatoryPostProcessor
  >       invokeBeanFactoryPostProcessors(beanFactory);
  > 
  >       // Register bean processors that intercept bean creation.
  >       // 注册BeanPostProcessor
  >       registerBeanPostProcessors(beanFactory);
  > 
  >       // Initialize message source for this context.
  >       initMessageSource();
  > 
  >       // Initialize event multicaster for this context.
  >       initApplicationEventMulticaster();
  > 
  >       // Initialize other special beans in specific context subclasses.
  >       onRefresh();
  > 
  >       // Check for listener beans and register them.
  >       registerListeners();
  > 
  >       // Instantiate all remaining (non-lazy-init) singletons.
  >       // 调用所有的单利 bean 的实例化
  >       finishBeanFactoryInitialization(beanFactory);
  > 
  >       // Last step: publish corresponding event.
  >       finishRefresh();
  >    }
  > 
  >    catch (BeansException ex) {
  >       if (logger.isWarnEnabled()) {
  >          logger.warn("Exception encountered during context initialization - " +
  >                "cancelling refresh attempt: " + ex);
  >       }
  > 
  >       // Destroy already created singletons to avoid dangling resources.
  >       destroyBeans();
  > 
  >       // Reset 'active' flag.
  >       cancelRefresh(ex);
  > 
  >       // Propagate exception to caller.
  >       throw ex;
  >    }
  > 
  >    finally {
  >       // Reset common introspection caches in Spring's core, since we
  >       // might not ever need metadata for singleton beans anymore...
  >       resetCommonCaches();
  >    }
  > }
  > ```

*   prepareBeanFactory(beanFactory);  准备beanFactory

*   postProcessBeanFactory(beanFactory); 扩展点

*  invokeBeanFactoryPostProcessors(beanFactory); 调用invokeBeanFactoryPostProcessors

*  registerBeanPostProcessors(beanFactory); 注册beanPostProcessor

*  finishBeanFactoryInitialization(beanFactory); 实例化所有的单利bean

  >  beanFactory.preInstantiateSingletons(); 于实例化单利bean
  >
  > *  获取RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName); 
  >
  > *  getBean(beanName);  通过getBean 实例化
  >
  >   ```java
  >   <T> T doGetBean(
  >         final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
  >         throws BeansException 
  >   ```
  >
  >   1 从缓存中获取
  >
  >   2 缓存获取不到 进行实例化 
  >
  >   > a, 创建bean 并实例化bean 之后
  >   >
  >   > b, 通过geanObject() 获取bean
  >   >
  >   > c,  放到singletonObjects 里面

  ​	   3 判断单利/prototype ….. ??

  > 创建bean 