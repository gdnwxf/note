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
  >  *  获取RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName); 
  >
  >  *  getBean(beanName);  通过getBean 实例化
  >
  >  ```java
  >  <T> T doGetBean(
  >       final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
  >       throws BeansException 
  >  ```
  >
  >  1 从缓存中获取
  >
  >  2 缓存获取不到 进行实例化 
  >
  >   > a, 创建bean 并实例化bean 之后
  >   >
  >   > b, 通过geanObject() 获取bean
  >   >
  >   > c,  放到singletonObjects 里面 

  ​    3 判断单利/prototype ….. ??

  ```java
  @SuppressWarnings("unchecked")
  protected <T> T doGetBean(
        final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
        throws BeansException {
  
     final String beanName = transformedBeanName(name);
     Object bean;
  
     // Eagerly check singleton cache for manually registered singletons.
     Object sharedInstance = getSingleton(beanName);
     if (sharedInstance != null && args == null) {
        if (logger.isDebugEnabled()) {
           if (isSingletonCurrentlyInCreation(beanName)) {
              logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
                    "' that is not fully initialized yet - a consequence of a circular reference");
           }
           else {
              logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
           }
        }
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
     }
  
     else {
        // Fail if we're already creating this bean instance:
        // We're assumably within a circular reference.
        //判断是否是 prototype 进行循环依赖校验
        if (isPrototypeCurrentlyInCreation(beanName)) {
           throw new BeanCurrentlyInCreationException(beanName);
        }
  
        // Check if bean definition exists in this factory.
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
           // Not found -> check parent.
           String nameToLookup = originalBeanName(name);
           if (args != null) {
              // Delegation to parent with explicit args.
              return (T) parentBeanFactory.getBean(nameToLookup, args);
           }
           else {
              // No args -> delegate to standard getBean method.
              return parentBeanFactory.getBean(nameToLookup, requiredType);
           }
        }
  
        if (!typeCheckOnly) {
           markBeanAsCreated(beanName);
        }
  
        try {
           final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
           checkMergedBeanDefinition(mbd, beanName, args);
  
           // Guarantee initialization of beans that the current bean depends on.
           String[] dependsOn = mbd.getDependsOn();
           if (dependsOn != null) {
              for (String dep : dependsOn) {
                 if (isDependent(beanName, dep)) {
                    throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                          "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                 }
                 registerDependentBean(dep, beanName);
                 getBean(dep);
              }
           }
  
           // Create bean instance.
           if (mbd.isSingleton()) {
              sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
                 @Override
                 public Object getObject() throws BeansException {
                    try {
                       return createBean(beanName, mbd, args);
                    }
                    catch (BeansException ex) {
                       // Explicitly remove instance from singleton cache: It might have been put there
                       // eagerly by the creation process, to allow for circular reference resolution.
                       // Also remove any beans that received a temporary reference to the bean.
                       destroySingleton(beanName);
                       throw ex;
                    }
                 }
              });
              bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
           }
  
           else if (mbd.isPrototype()) {
              // It's a prototype -> create a new instance.
              Object prototypeInstance = null;
              try {
                 beforePrototypeCreation(beanName);
                 prototypeInstance = createBean(beanName, mbd, args);
              }
              finally {
                 afterPrototypeCreation(beanName);
              }
              bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
           }
  
           else {
              String scopeName = mbd.getScope();
              final Scope scope = this.scopes.get(scopeName);
              if (scope == null) {
                 throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
              }
              try {
                 Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
                    @Override
                    public Object getObject() throws BeansException {
                       beforePrototypeCreation(beanName);
                       try {
                          return createBean(beanName, mbd, args);
                       }
                       finally {
                          afterPrototypeCreation(beanName);
                       }
                    }
                 });
                 bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
              }
              catch (IllegalStateException ex) {
                 throw new BeanCreationException(beanName,
                       "Scope '" + scopeName + "' is not active for the current thread; consider " +
                       "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                       ex);
              }
           }
        }
        catch (BeansException ex) {
           cleanupAfterBeanCreationFailure(beanName);
           throw ex;
        }
     }
  
     // Check if required type matches the type of the actual bean instance.
     if (requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())) {
        try {
           return getTypeConverter().convertIfNecessary(bean, requiredType);
        }
        catch (TypeMismatchException ex) {
           if (logger.isDebugEnabled()) {
              logger.debug("Failed to convert bean '" + name + "' to required type '" +
                    ClassUtils.getQualifiedName(requiredType) + "'", ex);
           }
           throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
     }
     return (T) bean;
  }
  ```

  

### createBean(beanName, mbd, args);

创建bean的方法

```java
@Override
protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
   if (logger.isDebugEnabled()) {
      logger.debug("Creating instance of bean '" + beanName + "'");
   }
   RootBeanDefinition mbdToUse = mbd;

   // Make sure bean class is actually resolved at this point, and
   // clone the bean definition in case of a dynamically resolved Class
   // which cannot be stored in the shared merged bean definition.
   Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
   if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
      mbdToUse = new RootBeanDefinition(mbd);
      mbdToUse.setBeanClass(resolvedClass);
   }

   // Prepare method overrides.
   try {
      mbdToUse.prepareMethodOverrides();
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
            beanName, "Validation of method overrides failed", ex);
   }

   try {
      // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
      Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
      if (bean != null) {
         return bean;
      }
   }
   catch (Throwable ex) {
      throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
            "BeanPostProcessor before instantiation of bean failed", ex);
   }
   //正式创建bean的时候
   Object beanInstance = doCreateBean(beanName, mbdToUse, args);
   if (logger.isDebugEnabled()) {
      logger.debug("Finished creating instance of bean '" + beanName + "'");
   }
   return beanInstance;
}
```

### doCreateBean(beanName, mbdToUse, args);

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
      throws BeanCreationException {

   // Instantiate the bean.
   BeanWrapper instanceWrapper = null;
   if (mbd.isSingleton()) {
      instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
   }
   if (instanceWrapper == null) {
      instanceWrapper = createBeanInstance(beanName, mbd, args);
   }
   final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
   Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);
   mbd.resolvedTargetType = beanType;

   // Allow post-processors to modify the merged bean definition.
   synchronized (mbd.postProcessingLock) {
      if (!mbd.postProcessed) {
         try {
            applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
         }
         catch (Throwable ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                  "Post-processing of merged bean definition failed", ex);
         }
         mbd.postProcessed = true;
      }
   }

   // Eagerly cache singletons to be able to resolve circular references
   // even when triggered by lifecycle interfaces like BeanFactoryAware.
   boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
         isSingletonCurrentlyInCreation(beanName));
   if (earlySingletonExposure) {
      if (logger.isDebugEnabled()) {
         logger.debug("Eagerly caching bean '" + beanName +
               "' to allow for resolving potential circular references");
      }
      addSingletonFactory(beanName, new ObjectFactory<Object>() {
         @Override
         public Object getObject() throws BeansException {
            return getEarlyBeanReference(beanName, mbd, bean);
         }
      });
   }

   // Initialize the bean instance.
   Object exposedObject = bean;
   try {
      populateBean(beanName, mbd, instanceWrapper);
      if (exposedObject != null) {
         exposedObject = initializeBean(beanName, exposedObject, mbd);
      }
   }
   catch (Throwable ex) {
      if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
         throw (BeanCreationException) ex;
      }
      else {
         throw new BeanCreationException(
               mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
      }
   }

   if (earlySingletonExposure) {
      Object earlySingletonReference = getSingleton(beanName, false);
      if (earlySingletonReference != null) {
         if (exposedObject == bean) {
            exposedObject = earlySingletonReference;
         }
         else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
            String[] dependentBeans = getDependentBeans(beanName);
            Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
            for (String dependentBean : dependentBeans) {
               if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                  actualDependentBeans.add(dependentBean);
               }
            }
            if (!actualDependentBeans.isEmpty()) {
               throw new BeanCurrentlyInCreationException(beanName,
                     "Bean with name '" + beanName + "' has been injected into other beans [" +
                     StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                     "] in its raw version as part of a circular reference, but has eventually been " +
                     "wrapped. This means that said other beans do not use the final version of the " +
                     "bean. This is often the result of over-eager type matching - consider using " +
                     "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
            }
         }
      }
   }

   // Register bean as disposable.
   try {
     //吧实现Disposable 的bean 注册到 disposableBeans 便于后面执行bean的org.springframework.beans.factory.DisposableBean#destroy方法
      registerDisposableBeanIfNecessary(beanName, bean, mbd);
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
   }

   return exposedObject;
}
```

### initializeBean(beanName, exposedObject, mbd);

```java
protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
   if (System.getSecurityManager() != null) {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
         @Override
         public Object run() {
            invokeAwareMethods(beanName, bean);
            return null;
         }
      }, getAccessControlContext());
   }
   else {
     // 处理 BeanNameAware, BeanClassLoaderAware, BeanFactoryAware setting
      invokeAwareMethods(beanName, bean);
   }

   Object wrappedBean = bean;
  // beanPostProcessorBeforeInitialization 处理
   if (mbd == null || !mbd.isSynthetic()) {
      wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
   }

   try {
      invokeInitMethods(beanName, wrappedBean, mbd);
   }
   catch (Throwable ex) {
      throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            beanName, "Invocation of init method failed", ex);
   }
   // beanPostProcessorAfterInitialization 处理 如aop等的东西
   if (mbd == null || !mbd.isSynthetic()) {
      wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
   }
   return wrappedBean;
}
```

### invokeInitMethods(String beanName, final Object bean, RootBeanDefinition mbd) throws Throwable

```java
protected void invokeInitMethods(String beanName, final Object bean, RootBeanDefinition mbd)
      throws Throwable {

   boolean isInitializingBean = (bean instanceof InitializingBean);
   if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
      if (logger.isDebugEnabled()) {
         logger.debug("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
      }
      if (System.getSecurityManager() != null) {
         try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
               @Override
               public Object run() throws Exception {
                  ((InitializingBean) bean).afterPropertiesSet();
                  return null;
               }
            }, getAccessControlContext());
         }
         catch (PrivilegedActionException pae) {
            throw pae.getException();
         }
      }
      else {
        //处理 实现InitializingBean接口的调用
         ((InitializingBean) bean).afterPropertiesSet();
      }
   }
	 // 处理factory method的调用
   if (mbd != null) {
      String initMethodName = mbd.getInitMethodName();
      if (initMethodName != null && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
            !mbd.isExternallyManagedInitMethod(initMethodName)) {
         invokeCustomInitMethod(beanName, bean, mbd);
      }
   }
}
```