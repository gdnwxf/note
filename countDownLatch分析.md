### countDownLatch分析

* 运行时状态  队列中只存在一个节点 
* 使用场景 主线程等待子线程完成之后统一执行



countDown  设置队列中cas ( state = state - 1 )  成功后尝试唤醒 shared 的Node 也就是头节点 ( 直到state == 0 时才能够唤醒头结点) 

```Java
public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)  // state = state - 1 return state == 0
        doAcquireSharedInterruptibly(arg); //state == 0 时候唤醒头结点
}
```

* 函数入口

```Java
/**
 * Constructs a {@code CountDownLatch} initialized with the given count.
 *
 * @param count the number of times {@link #countDown} must be invoked
 *        before threads can pass through {@link #await}
 * @throws IllegalArgumentException if {@code count} is negative
 */
public CountDownLatch(int count) {
    if (count < 0) throw new IllegalArgumentException("count < 0");
    this.sync = new Sync(count);  // 实例化的时候 指定 state 的值 
    //   Sync(int count) {
    //       setState(count);
    //    }
}
```

* 主要方法

  ```java
  public void await() { sync.acquireSharedInterruptibly(1); }
  public void countDown() { sync.releaseShared(1); }  state - 1 直到cas 成功 
  ```

* 同步器

```java
private static final class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = 4982264981922014374L;

    Sync(int count) {
        setState(count);
    }

    int getCount() {
        return getState();
    }

    protected int tryAcquireShared(int acquires) {
        return (getState() == 0) ? 1 : -1;
    }

    protected boolean tryReleaseShared(int releases) {
        // Decrement count; signal when transition to zero
        for (;;) {
            int c = getState();
            if (c == 0)
                return false;
            int nextc = c-1;
            if (compareAndSetState(c, nextc))
                return nextc == 0;
        }
    }
}
```