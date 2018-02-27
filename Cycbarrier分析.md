## CycBarrier 分析

 线程进入Lock的AQS队列 然后state =  state -1  

--> AQS队列中的节点进入ConditionObject 队列 

--> 然后当state == 0 时候就 -> signalAll ->unparkSucessor -> unlock 所有节点从ConditionObject 队列进入到AQS的队列中 结束当前卡口

* 重要参数分析

```Java
//循环使用的实例
private static class Generation {
    boolean broken = false;
}

/** The lock for guarding barrier entry */
private final ReentrantLock lock = new ReentrantLock();
/** Condition to wait on until tripped */
private final Condition trip = lock.newCondition();
/** The number of parties */
// 总共多少个线程
private final int parties;
/* The command to run when tripped */
private final Runnable barrierCommand;
/** The current generation */
private Generation generation = new Generation();

/**
 *  有多少个parties在等待  
 * Number of parties still waiting. Counts down from parties to 0
 * on each generation.  It is reset to parties on each new
 * generation or when broken.
 */
private int count;
```

* 重要方法分析 dowait 方法

```java
 private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        final ReentrantLock lock = this.lock;
                 // 进入AQS队列
        lock.lock();
                 // 单线程下执行
        try {
            final Generation g = generation;

            if (g.broken)
                throw new BrokenBarrierException();

            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }
			//  当 count 减到0 时候 breakBarrier() or  nextGeneration(); 释放Node 到AQS队列中执行
             
            int index = --count;
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // loop until tripped, broken, interrupted, or timed out
            for (;;) {
                try {
                    if (!timed)
                        trip.await(); // 到达时 然后 lock.unlock();
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();

                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }


```

下一次卡口nextGeneration 方法的

```java
/**
 * Updates state on barrier trip and wakes up everyone.
 * Called only while holding lock.
 */
private void nextGeneration() {
  // signal completion of last generation
  trip.signalAll();
  // set up next generation
  count = parties;
  generation = new Generation();
}
```
* 打破当前阻塞 breakBarrier

```Java
   private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }
```