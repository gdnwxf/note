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