## dubbo 源码分析

application.getBean(interface)

com.alibaba.dubbo.config.spring.ReferenceBean#getObject

com.alibaba.dubbo.config.ReferenceConfig#get

com.alibaba.dubbo.config.ReferenceConfig#init

com.alibaba.dubbo.config.ReferenceConfig#createProxy

* ExtensionLoader 机制

> 当以extName getExtension(extName) 找不到时创建一个createExtension (实例化自身的时候需要按照set 方法注入自身框架的参数 通常是 xx$Adaptice 或是 ?从spring中来实现类? )
>
> 如 registerProtocol 
>
> instance = {RegistryProtocol@2093} 
>  overrideListeners = {ConcurrentHashMap@2169}  size = 0
>  bounds = {ConcurrentHashMap@2170}  size = 0
>  cluster = {Cluster$Adaptive@2522} 
>  protocol = {Protocol$Adaptive@1998} 
>  registryFactory = {RegistryFactory$Adaptive@2267} 
>  proxyFactory = {ProxyFactory$Adaptive@2444} 

> 把自己实例化作为构造函数的参数传入到wrapClass 中 并返回 wrapClass 做为instance 

```java
private T createExtension(String name) {
    Class<?> clazz = getExtensionClasses().get(name);
    if (clazz == null) {
        throw findException(name);
    }
    try {
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            EXTENSION_INSTANCES.putIfAbsent(clazz, (T) clazz.newInstance());
            instance = (T) EXTENSION_INSTANCES.get(clazz);
        }
        injectExtension(instance);
        Set<Class<?>> wrapperClasses = cachedWrapperClasses;
        if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
            for (Class<?> wrapperClass : wrapperClasses) {
                instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance)); // 在此实现循环嵌套注入instance 
            }
        }
        return instance; // 此时只需要最后一个instance既可以实现链式调用啦
    } catch (Throwable t) {
        throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                type + ")  could not be instantiated: " + t.getMessage(), t);
    }
}
```

*  配置覆盖策略

