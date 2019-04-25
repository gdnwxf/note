## Diamond 配置中心的使用

> diamond 借助edas的配置中心来实现

####MessageAdapter 的实现

```java
package com.meeruu.supplier.service.facade.listener;

import com.taobao.diamond.manager.ManagerListenerAdapter;

public class MyListener extends ManagerListenerAdapter {
    @Override
    public void receiveConfigInfo(String configInfo) {
        System.out.println(configInfo);
    }
}
```

##### Diamond的使用

```java
public static void main(String[] args) {
    Diamond.addListener( "com.meeruu.sg:supplier.properties",  //dataId
                        "DEFAULT_GROUP", // group
                        new MyListener()); //上面实现的listener
    System.out.println("dsa");
}
```

![image-20190425101746530](/Users/wch/opensource/note/assets/image-20190425101746530.png)修改后的打印结果



![image-20190425101911130](/Users/wch/opensource/note/assets/image-20190425101911130.png)

![image-20190425102238230](/Users/wch/opensource/note/assets/image-20190425102238230.png)

修改前后对比