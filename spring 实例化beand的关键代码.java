Abstract 


DefaultSingletonBeanRegistry.singletonObjects  这个地方存放的是BeanFactory 也即时ObjectFactory
DefaultListableBeanFactory表示的是 Factory  FactoryBean 表示这个Bean是特殊的bean
1 从singletonObjects拿到FactoryBean  
DefaultListableBeanFactory.preInstantiateSingletons

protected Object getObjectForBeanInstance(
      Object beanInstance, String name, String beanName, RootBeanDefinition mbd) {

   // Don't let calling code try to dereference the factory if the bean isn't a factory.
   if (BeanFactoryUtils.isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
      throw new BeanIsNotAFactoryException(transformedBeanName(name), beanInstance.getClass());
   }

   // Now we have the bean instance, which may be a normal bean or a FactoryBean.
   // If it's a FactoryBean, we use it to create a bean instance, unless the
   // caller actually wants a reference to the factory.
   if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
      return beanInstance;
   }

   Object object = null;
   if (mbd == null) {
      object = getCachedObjectForFactoryBean(beanName); // 这个是从 FactoryBeanRegistrySupport.factoryBeanObjectCache 拿到数据
   6,   if (object == null) {
      // Return bean instance from factory.
      FactoryBean<?> factory = (FactoryBean<?>) beanInstance; // 传入FactoryBean 即是Spring中的Factory
      // Caches object obtained from FactoryBean if it is a singleton.
      if (mbd == null && containsBeanDefinition(beanName)) {  // 判断 DefaultListableBeanFactory.beanDefinitionMap 是否含有这个bean
         mbd = getMergedLocalBeanDefinition(beanName); // 和AbstractBeanFactory.mergedBeanDefinitions 进行合并
      }
      boolean synthetic = (mbd != null && mbd.isSynthetic());
      object = getObjectFromFactoryBean(factory, beanName, !synthetic);
   }
   return object;
}
 
  合并成一个 RootBeanDefinition 下面的是合并BeanDefinition 

   /**
    * Return a RootBeanDefinition for the given bean, by merging with the
    * parent if the given bean's definition is a child bean definition.
    * @param beanName the name of the bean definition
    * @param bd the original bean definition (Root/ChildBeanDefinition)
    * @param containingBd the containing bean definition in case of inner bean,
    * or {@code null} in case of a top-level bean
    * @return a (potentially merged) RootBeanDefinition for the given bean
    * @throws BeanDefinitionStoreException in case of an invalid bean definition
    */
   protected RootBeanDefinition getMergedBeanDefinition(
         String beanName, BeanDefinition bd, BeanDefinition containingBd)
         throws BeanDefinitionStoreException {

      synchronized (this.mergedBeanDefinitions) {
         RootBeanDefinition mbd = null;

         // Check with full lock now in order to enforce the same merged instance.
         if (containingBd == null) {
            mbd = this.mergedBeanDefinitions.get(beanName);
         }

         if (mbd == null) {
            if (bd.getParentName() == null) {
               // Use copy of given root bean definition.
               if (bd instanceof RootBeanDefinition) {
                  mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
               }
               else {
                  mbd = new RootBeanDefinition(bd);
               }
            }
            else {
               // Child bean definition: needs to be merged with parent.
               BeanDefinition pbd;
               try {
                  String parentBeanName = transformedBeanName(bd.getParentName());
                  if (!beanName.equals(parentBeanName)) {
                     pbd = getMergedBeanDefinition(parentBeanName);
                  }
                  else {
                     BeanFactory parent = getParentBeanFactory();
                     if (parent instanceof ConfigurableBeanFactory) {
                        pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
                     }
                     else {
                        throw new NoSuchBeanDefinitionException(parentBeanName,
                              "Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
                              "': cannot be resolved without an AbstractBeanFactory parent");
                     }
                  }
               }
               catch (NoSuchBeanDefinitionException ex) {
                  throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
                        "Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
               }
               // Deep copy with overridden values.
               mbd = new RootBeanDefinition(pbd);
               mbd.overrideFrom(bd);
            }

            // Set default singleton scope, if not configured before.
            if (!StringUtils.hasLength(mbd.getScope())) {
               mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
            }

            // A bean contained in a non-singleton bean cannot be a singleton itself.
            // Let's correct this on the fly here, since this might be the result of
            // parent-child merging for the outer bean, in which case the original inner bean
            // definition will not have inherited the merged outer bean's singleton status.
            if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
               mbd.setScope(containingBd.getScope());
            }

            // Cache the merged bean definition for the time being
            // (it might still get re-merged later on in order to pick up metadata changes)
            if (containingBd == null && isCacheBeanMetadata()) {
               this.mergedBeanDefinitions.put(beanName, mbd);
            }
         }

         return mbd;
      }
   }


   从Factory中拿到对象 (包含对象的实例化)
   /**
    * Obtain an object to expose from the given FactoryBean.
    * @param factory the FactoryBean instance
    * @param beanName the name of the bean
    * @param shouldPostProcess whether the bean is subject to post-processing
    * @return the object obtained from the FactoryBean
    * @throws BeanCreationException if FactoryBean object creation failed
    * @see org.springframework.beans.factory.FactoryBean#getObject()
    */
   protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
      if (factory.isSingleton() && containsSingleton(beanName)) {
         synchronized (getSingletonMutex()) {
            Object object = this.factoryBeanObjectCache.get(beanName);
            if (object == null) {
               object = doGetObjectFromFactoryBean(factory, beanName);
               // Only post-process and store if not put there already during getObject() call above
               // (e.g. because of circular reference processing triggered by custom getBean calls)
               Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
               if (alreadyThere != null) {
                  object = alreadyThere;
               }
               else {
                  if (object != null && shouldPostProcess) {
                     try {
                        object = postProcessObjectFromFactoryBean(object, beanName);
                     }
                     catch (Throwable ex) {
                        throw new BeanCreationException(beanName,
                              "Post-processing of FactoryBean's singleton object failed", ex);
                     }
                  }
                  this.factoryBeanObjectCache.put(beanName, (object != null ? object : NULL_OBJECT));
               }
            }
            return (object != NULL_OBJECT ? object : null);
         }
      }
      else {
         Object object = doGetObjectFromFactoryBean(factory, beanName);
         if (object != null && shouldPostProcess) {
            try {
               object = postProcessObjectFromFactoryBean(object, beanName);
            }
            catch (Throwable ex) {
               throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
            }
         }
         return object;
      }
   }
