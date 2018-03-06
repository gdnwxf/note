## Semaphore 分析 

*  设置state的值 当state > 0 设置头节点并 Propagate 状态 并释放共享线程 doReleaseShared   

  > 当state 减少到小于0 的时候才会执行 if (tryAcquireShared(arg) < 0)    doAcquireSharedInterruptibly(arg); 当前线程会进入AQS 阻塞
  >
  > 当有线程释放时则会  state + 1 然后unparkSucessor 释放后继节点


* 和 CountDownLatch 一样  doAcquireSharedInterruptibly --> addWaiter(Node.SHARED)

> 不同点  

* Semaphore 的 tryAcquireShared 方法

```java
  final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
  }
```

* CountDownLatch 的  tryAcquireShared

  ```java
  protected int tryAcquireShared(int acquires) {
      return (getState() == 0) ? 1 : -1;
  }
  ```