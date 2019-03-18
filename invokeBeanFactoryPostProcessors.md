#### spring 中 invokeBeanFactoryPostProcessors(beanFactory);方法的处理

```mermaid
graph LR
   refresh --> invokeBeanFactoryPostProcessors 
   invokeBeanFactoryPostProcessors -->  PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors
```



处理流程如下

```mermaid
graph TB
  invokeBeanFactoryPostProcessors --> BeanDefinitionRegistryPostProcessor
  BeanDefinitionRegistryPostProcessor --> BeanFactoryPostProcessor
  
```

##### 处理 BeanDefinitionRegistryPostProcessor

*  First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
*  Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
*  Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.

#####处理 BeanFactoryPostProcessor

*  First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
*  Next, invoke the BeanFactoryPostProcessors that implement Ordered.
*  Finally, invoke all other BeanFactoryPostProcessors.

