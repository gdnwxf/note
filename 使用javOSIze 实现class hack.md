###使用javOSIze 实现class hack

#####测试代码

```java

/**
 * #author wangluo
 * #date 2019-03-10 00:06
 */
public class TaskTest {


    public static void main(String[] args) throws InterruptedException {

        User user1 = new User();
        Thread nihao = new Thread("dsad") {
            @Override
            public void run() {

                while (true) {
                    user1.doWork();

                    User user2 = new User();
                    user2.doWork2();
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        nihao.start();

        nihao.join();
    }

    static class  User {
        public   void doWork() {
            System.out.println("dsadsa ");
        }

        public   void doWork2() {
            System.out.println("-------dsadsada ");
        }
    }
}
```

#### 测试工具截图

![image-20190310004755812](/Users/wch/Library/Application Support/typora-user-images/image-20190310004755812.png)

#### 动态修改后代码

```java
// 
// Decompiled by Procyon v0.5.30
// 

package com.yt.icp.biz.redis;

import java.util.concurrent.TimeUnit;

public class TaskTest
{
    public static void main(final String[] args) throws InterruptedException {
        final User user1 = new User();
        final Thread nihao = new Thread("dsad") {
            @Override
            public void run() {
                while (true) {
                    user1.doWork();
                    final User user2 = new User();
                    user2.doWork2();
                    try {
                        TimeUnit.SECONDS.sleep(3L);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        nihao.start();
        nihao.join();
    }
    
    static class User
    {
        public void doWork() {
            System.out.println("dhsahdsahdhsa dshad sadhsa  ------>  ");
        }
        
        public void doWork2() {
            System.out.println("我去呜呜呜 !!!!");
        }
    }
}
```

#### 修改前后变化

![image-20190310004938351](https://github.com/gdnwxf/note/blob/master/assets/image-20190310004938351.png)
