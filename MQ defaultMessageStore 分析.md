###  MQ defaultMessageStore 分析

```java
public DefaultMessageStore(final MessageStoreConfig messageStoreConfig, final BrokerStatsManager brokerStatsManager,
    final MessageArrivingListener messageArrivingListener, final BrokerConfig brokerConfig) throws IOException {
    this.messageArrivingListener = messageArrivingListener;
    this.brokerConfig = brokerConfig;
    this.messageStoreConfig = messageStoreConfig;
    this.brokerStatsManager = brokerStatsManager;
    this.allocateMappedFileService = new AllocateMappedFileService(this);
    if (messageStoreConfig.isEnableDLegerCommitLog()) {
        this.commitLog = new DLedgerCommitLog(this);
    } else {
        this.commitLog = new CommitLog(this);
    }
    this.consumeQueueTable = new ConcurrentHashMap<>(32);

    this.flushConsumeQueueService = new FlushConsumeQueueService();
    this.cleanCommitLogService = new CleanCommitLogService();
    this.cleanConsumeQueueService = new CleanConsumeQueueService();
    this.storeStatsService = new StoreStatsService();
    this.indexService = new IndexService(this);
    if (!messageStoreConfig.isEnableDLegerCommitLog()) {
        this.haService = new HAService(this);
    } else {
        this.haService = null;
    }
    this.reputMessageService = new ReputMessageService();

    this.scheduleMessageService = new ScheduleMessageService(this);

    this.transientStorePool = new TransientStorePool(messageStoreConfig);

    if (messageStoreConfig.isTransientStorePoolEnable()) {
        this.transientStorePool.init();
    }

    this.allocateMappedFileService.start();

    this.indexService.start();

    this.dispatcherList = new LinkedList<>();
    this.dispatcherList.addLast(new CommitLogDispatcherBuildConsumeQueue());
    this.dispatcherList.addLast(new CommitLogDispatcherBuildIndex());

    File file = new File(StorePathConfigHelper.getLockFile(messageStoreConfig.getStorePathRootDir()));
    MappedFile.ensureDirOK(file.getParent());
    lockFile = new RandomAccessFile(file, "rw");
}
```