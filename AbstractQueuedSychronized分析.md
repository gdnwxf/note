## 整体分析

* 重点  state  控制整体并发   waitStatus 控制节点是否能释放 是否 park  , unparkSuccessor 会吧waitStatus 设置成0  

* 处于cancel节点可能原因 当前节点被 unparkSuccessor 唤醒 

* 如果遇到 thread interupte 则抛出异常 if shouldParkAfterFailedAcquire(p, node) &&

  parkAndCheckInterrupt())  throw new InterruptedException(); // 然后就会取消任务

*  cancelAcquire 节点的情况  呗唤醒 park超时

> shared 和 exclusive 仅仅只是标识节点的状态  用来设置node的nextWaiter
>
> 初次入队列永远是哑节点(即head节点为一个 enq(node) 决定 这个节点会在AQS后面的操作中丢失)
>
> acquireQueue() 是吧队列中当前节点去竞争队列的head节点

节点的呗唤醒后的关键方法

```java
private void cancelAcquire(Node node) {
    // Ignore if node doesn't exist
    if (node == null)
        return;

    node.thread = null;

    // Skip cancelled predecessors
    Node pred = node.prev;
    while (pred.waitStatus > 0) // 跳过队列中 waitStatus > 0 cancel 节点
        node.prev = pred = pred.prev;
	
    // predNext is the apparent node to unsplice. CASes below will
    // fail if not, in which case, we lost race vs another cancel
    // or signal, so no further action is necessary.
    Node predNext = pred.next;

    // Can use unconditional write instead of CAS here.
    // After this atomic step, other Nodes can skip past us.
    // Before, we are free of interference from other threads.
    node.waitStatus = Node.CANCELLED;

    // If we are the tail, remove ourselves.
    if (node == tail && compareAndSetTail(node, pred)) {
        compareAndSetNext(pred, predNext, null);
    } else {
        // If successor needs signal, try to set pred's next-link
        // so it will get one. Otherwise wake it up to propagate.
        int ws;
        if (pred != head &&
            ((ws = pred.waitStatus) == Node.SIGNAL ||
             (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
            pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0)
                compareAndSetNext(pred, predNext, next);  // 将中间waitStatus > 0 的节点从队列中移除
        } else {
            unparkSuccessor(node);
        }

        node.next = node; // help GC
    }
}
```

### 从ReentranLock分析入手 (可重入锁分析)

shared 和 exclusive 仅仅只是标识节点的状态

```java
/** Marker to indicate a node is waiting in shared mode */
static final Node SHARED = new Node();
/** Marker to indicate a node is waiting in exclusive mode */
static final Node EXCLUSIVE = null;
```

aqs中一个Node 代表一个线程  head 节点的线程是null 代表的是当前线程

* reentranLock的构造函数来看  默认非公平锁  

```java
/**
 * Creates an instance of {@code ReentrantLock}.
 * This is equivalent to using {@code ReentrantLock(false)}.
 */
public ReentrantLock() {
    sync = new NonfairSync();
}

/**
 * Creates an instance of {@code ReentrantLock} with the
 * given fairness policy.
 *
 * @param fair {@code true} if this lock should use a fair ordering policy
 */
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

* 分析 lock() 和 unlock() 方法

  > 分析之前看看看AbstractQueuedSychronized的Node节点的代码
  >
  > ```java
  >   		/** Marker to indicate a node is waiting in shared mode */
  >         static final Node SHARED = new Node();
  >         /** Marker to indicate a node is waiting in exclusive mode */
  >         static final Node EXCLUSIVE = null;
  >
  >         /** waitStatus value to indicate thread has cancelled */
  >         static final int CANCELLED =  1;
  >         /** waitStatus value to indicate successor's thread needs unparking */
  >         static final int SIGNAL    = -1;
  >         /** waitStatus value to indicate thread is waiting on condition */
  >         static final int CONDITION = -2;
  >         /**
  >          * waitStatus value to indicate the next acquireShared should
  >          * unconditionally propagate
  >          */
  >         static final int PROPAGATE = -3;
  >
  >         /**
  >          * Status field, taking on only the values:
  >          *   SIGNAL:     The successor of this node is (or will soon be)
  >          *               blocked (via park), so the current node must
  >          *               unpark its successor when it releases or
  >          *               cancels. To avoid races, acquire methods must
  >          *               first indicate they need a signal,
  >          *               then retry the atomic acquire, and then,
  >          *               on failure, block.
  >          *   CANCELLED:  This node is cancelled due to timeout or interrupt.
  >          *               Nodes never leave this state. In particular,
  >          *               a thread with cancelled node never again blocks.
  >          *   CONDITION:  This node is currently on a condition queue.
  >          *               It will not be used as a sync queue node
  >          *               until transferred, at which time the status
  >          *               will be set to 0. (Use of this value here has
  >          *               nothing to do with the other uses of the
  >          *               field, but simplifies mechanics.)
  >          *   PROPAGATE:  A releaseShared should be propagated to other
  >          *               nodes. This is set (for head node only) in
  >          *               doReleaseShared to ensure propagation
  >          *               continues, even if other operations have
  >          *               since intervened.
  >          *   0:          None of the above
  >          *
  >          * The values are arranged numerically to simplify use.
  >          * Non-negative values mean that a node doesn't need to
  >          * signal. So, most code doesn't need to check for particular
  >          * values, just for sign.
  >          *
  >          * The field is initialized to 0 for normal sync nodes, and
  >          * CONDITION for condition nodes.  It is modified using CAS
  >          * (or when possible, unconditional volatile writes).
  >          */
  >         volatile int waitStatus;
  > ```

  * SIGNAL 

    > 这个节点的后继者（或将很快）被阻塞（通过停泊），因此当前节点在释放或取消时必须取消停放其后继者。为了避免竞争，获得方法必须首先表明他们需要一个信号，然后重试原子获取，然后在失败时阻塞。


  * CANCELLED

  >  由于超时或中断，该节点被取消。节点不会离开这个状态。特别是，一个被取消节点的线程不会再被阻塞。

  * CONDITION 

    > 这个节点当前正在一个条件队列中。它不会被用作一个同步队列节点，直到被转移，此时状态将被设置为0.（这里使用这个值与该字段的其他用途无关 ，但是简化了机制。）

  * PROPAGATE 

    > releaseShared应该传播到其他节点。 这在doReleaseShared中设置（仅用于头节点）以确保传播继续，即使其他操作已经介入。

    * 状态小结

      >这些值按数字排列以简化使用。 非负值意味着节点不需要信号。 所以，大多数代码不需要检查特定的值，只是为了标志。
      >
      >正常同步节点的字段初始化为0，条件节点的CONDITION字段初始化为0。 它使用CAS进行修改（或者在可能的情况下，无条件的volatile写入）。

      ​

    ## 公平锁和非公平锁的Sync控制 

    > 都继承于AbstractQueuedSynchronizer

    ```java
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * Performs {@link Lock#lock}. The main reason for subclassing
         * is to allow fast path for nonfair version.
         */
        abstract void lock();

        /**
         * Performs non-fair tryLock.  tryAcquire is implemented in
         * subclasses, but both need nonfair try for trylock method.
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }
    ```

    #### 非公平锁

    * 入口lock() 

      ```java
      /**
       * Sync object for non-fair locks
       */
      static final class NonfairSync extends Sync {
          private static final long serialVersionUID = 7316153563782823691L;

          /**
           * Performs lock.  Try immediate barge, backing up to normal
           * acquire on failure.
           */
         // 非公平锁lock时先直接取设置 state = 1  如果当前是 0 的状态时(其他节点正好入队列) 然后设置排他线程
         // 否则同公平锁
          final void lock() {
              if (compareAndSetState(0, 1))
                  setExclusiveOwnerThread(Thread.currentThread());
              else
                  acquire(1);
          }

          protected final boolean tryAcquire(int acquires) {
              return nonfairTryAcquire(acquires);
          }
      }
      ```


#### 公平锁

* 入口lock() 

```java
/**
 * Sync object for fair locks
 */
static final class FairSync extends Sync {
    private static final long serialVersionUID = -3000897897090466540L;

    final void lock() {
        acquire(1);
    }

    /**
     * Fair version of tryAcquire.  Don't grant access unless
     * recursive call or no waiters or is first.
     */
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
}
```

shouldParkAfterFailedAcquire 方法

```java
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        /*
         * This node has already set status asking a release
         * to signal it, so it can safely park.
         */
        return true;
    if (ws > 0) {
        /*
         * Predecessor was cancelled. Skip over predecessors and
         * indicate retry.
         */
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        /*
         * waitStatus must be 0 or PROPAGATE.  Indicate that we
         * need a signal, but don't park yet.  Caller will need to
         * retry to make sure it cannot acquire before parking.
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}
```