## RQ 源码分析

| 模块          | 名称                         |
| ------------- | ---------------------------- |
| acl           | 权限控制                     |
| broker        | broker 存储消息 消费         |
| client        | 客户端 producer and consumer |
| common        |                              |
| dev           |                              |
| distribution  |                              |
| docs          | 文档                         |
| example       | 示例                         |
| filter        | 过滤器                       |
| filtersrv     | 过滤服务 如服务端 MsgTag过滤 |
| logappender   | 日志                         |
| logging       | 日志主键                     |
| namesrv       | 注册中心                     |
| openmessaging | 开放消息                     |
| remoting      | 远程调用                     |
| srvutil       | util 工具                    |
| store         | 存储                         |
| style         |                              |
| test          | 测试                         |
| tools         |                              |

## 整体依赖图如下![rocketmq-broker](/Users/wch/opensource/note/assets/rocketmq-broker.png)

mq 将消息写入好之后放入consumeQueue的过程  reput

- ConsumeQueue.putMessagePositionInfoWrapper(DispatchRequest)  (org.apache.rocketmq.store)   
- DefaultMessageStore.putMessagePositionInfo(DispatchRequest)  (org.apache.rocketmq.store)   
- CommitLogDispatcherBuildConsumeQueue in DefaultMessageStore.dispatch(DispatchRequest)  (org.apache.rocketmq.store)   
- DefaultMessageStore.doDispatch(DispatchRequest)  (org.apache.rocketmq.store)   
- **ReputMessageService** in DefaultMessageStore.doReput()  (org.apache.rocketmq.store)   
- **ReputMessageService** in DefaultMessageStore.run()  (org.apache.rocketmq.store)   
- RequestTask.run()  (org.apache.rocketmq.remoting.netty)   
- CommitLog.recoverAbnormally(long)(2 usages)  (org.apache.rocketmq.store) 

具体如图

![image-20190416232125126](/Users/wch/opensource/note/assets/image-20190416232125126.png)

mq 消息刷盘

- CommitLog.**handleDiskFlush**(AppendMessageResult, PutMessageResult, MessageExt)  (org.apache.rocketmq.store)   
- CommitLog.putMessages(MessageExtBatch)  (org.apache.rocketmq.store)   
- DefaultMessageStore.putMessages(MessageExtBatch)  (org.apache.rocketmq.store)   
- SendMessageProcessor.sendBatchMessage(ChannelHandlerContext, RemotingCommand, SendMessageContext, SendMessageRequestHeader)  (org.apache.rocketmq.broker.processor)   
- SendMessageProcessor.processRequest(ChannelHandlerContext, RemotingCommand)  (org.apache.rocketmq.broker.processor)   
- Anonymous in processRequestCommand() in NettyRemotingAbstract.run()  (org.apache.rocketmq.remoting.netty)   
- CommitLog.putMessage(MessageExtBrokerInner)  (org.apache.rocketmq.store)   
- DefaultMessageStore.putMessage(MessageExtBrokerInner)  (org.apache.rocketmq.store)   
- TransactionalMessageBridge.putMessageReturnResult(MessageExtBrokerInner)  (org.apache.rocketmq.broker.transaction.queue)   
- TransactionalMessageBridge.putMessage(MessageExtBrokerInner)  (org.apache.rocketmq.broker.transaction.queue)   
- SendMessageProcessor.consumerSendMsgBack(ChannelHandlerContext, RemotingCommand)  (org.apache.rocketmq.broker.processor)   
- AbstractPluginMessageStore.putMessage(MessageExtBrokerInner)  (org.apache.rocketmq.broker.plugin)   
- SendMessageProcessor.sendMessage(ChannelHandlerContext, RemotingCommand, SendMessageContext, SendMessageRequestHeader)  (org.apache.rocketmq.broker.processor)   
- TransactionalMessageBridge.putHalfMessage(MessageExtBrokerInner)  (org.apache.rocketmq.broker.transaction.queue)   
- EndTransactionProcessor.sendFinalMessage(MessageExtBrokerInner)  (org.apache.rocketmq.broker.processor)   
- DeliverDelayedMessageTimerTask in ScheduleMessageService.executeOnTimeup()  (org.apache.rocketmq.store.schedule)   
- PullMessageProcessor.generateOffsetMovedEvent(OffsetMovedEvent)  (org.apache.rocketmq.broker.processor) 

如图关键代码

![image-20190416232938199](/Users/wch/opensource/note/assets/image-20190416232938199.png)

刷盘的处理

> 放入requestWrite list

```java
/**
 * GroupCommit Service
 */
class GroupCommitService extends FlushCommitLogService {
    private volatile List<GroupCommitRequest> requestsWrite = new ArrayList<GroupCommitRequest>();
    private volatile List<GroupCommitRequest> requestsRead = new ArrayList<GroupCommitRequest>();

    public synchronized void putRequest(final GroupCommitRequest request) {
        synchronized (this.requestsWrite) {
            this.requestsWrite.add(request);
        }
        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown(); // notify
        }
    }

    private void swapRequests() {
        List<GroupCommitRequest> tmp = this.requestsWrite;
        this.requestsWrite = this.requestsRead;
        this.requestsRead = tmp;
    }

    private void doCommit() {
        synchronized (this.requestsRead) {
            if (!this.requestsRead.isEmpty()) {
                for (GroupCommitRequest req : this.requestsRead) {
                    // There may be a message in the next file, so a maximum of
                    // two times the flush
                    boolean flushOK = false;
                    for (int i = 0; i < 2 && !flushOK; i++) {
                        flushOK = CommitLog.this.mappedFileQueue.getFlushedWhere() >= req.getNextOffset();

                        if (!flushOK) {
                            CommitLog.this.mappedFileQueue.flush(0);
                        }
                    }

                    req.wakeupCustomer(flushOK);
                }

                long storeTimestamp = CommitLog.this.mappedFileQueue.getStoreTimestamp();
                if (storeTimestamp > 0) {
                    CommitLog.this.defaultMessageStore.getStoreCheckpoint().setPhysicMsgTimestamp(storeTimestamp);
                }

                this.requestsRead.clear();
            } else {
                // Because of individual messages is set to not sync flush, it
                // will come to this process
                CommitLog.this.mappedFileQueue.flush(0);
            }
        }
    }

    public void run() {
        CommitLog.log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                this.waitForRunning(10);
                this.doCommit();
            } catch (Exception e) {
                CommitLog.log.warn(this.getServiceName() + " service has exception. ", e);
            }
        }

        // Under normal circumstances shutdown, wait for the arrival of the
        // request, and then flush
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            CommitLog.log.warn("GroupCommitService Exception, ", e);
        }

        synchronized (this) {
            this.swapRequests();
        }

        this.doCommit();

        CommitLog.log.info(this.getServiceName() + " service end");
    }

    @Override
    protected void onWaitEnd() {
        this.swapRequests();
    }

    @Override
    public String getServiceName() {
        return GroupCommitService.class.getSimpleName();
    }

    @Override
    public long getJointime() {
        return 1000 * 60 * 5;
    }
}
```