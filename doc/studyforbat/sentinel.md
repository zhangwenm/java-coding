 
## sentinel

#### 限流类型
- 流量控制主要有两种统计类型，一种是统计并发线程数，另外一种则是统计 
  - QPS 每秒请求数，就是说服务器在一秒的时间内处理了多少个请求
  - Sentinel 并发控 制不负责创建和管理线程池，而是简单统计当前请求上下文的线程数目（正在执行的调用数目），如果超 出阈值，  
    新的请求会被立即拒绝，效果类似于信号量隔离。并发数控制通常在调用端进行配置。
- 流控模式
  - 直接：资源调用达到设置的阈值后直接被流控抛出异常
  - 关联：可使用关联限流来避免具有关联关系的资 源之间过度的争抢
  - 链路：根据调用链路入口限流。
- 流控效果
  - 当 QPS 超过某个阈值的时候，则采取措施进行流量控制。流量控制的效果包括以下几种：快速失败（直接 拒绝）、Warm Up（预热）、匀速排队（排队等待）
    - 快速失败方式是默认的流量控制方式。当QPS超过任意规则的阈值后，新 的请求就会被立即拒绝，拒绝方式为抛出FlowException。
    - Warm Up方式，即预热/冷启动方式。当系统长期处于低水位的情 况下，当流量突然增加时，直接把系统拉升到高水位可能瞬间把系统压垮。通过"冷启动"，
    让通过的流量缓 慢增加，在一定时间内逐渐增加到阈值上限，给冷系统一个预热的时间，避免冷系统被压垮
    - 匀速排队方式会严格控制请求通过的间隔时 间，也即是让请求以均匀的速度通过，对应的是漏桶算法。这种方式主要用于处理间隔性突发的流量，例如消息队列。想象一下这样的场景，  
    在某一秒有大量的请求 到来，而接下来的几秒则处于空闲状态，我们希望系统能够在接下来的空闲期间逐渐处理这些请求，而不是在第一秒直接拒绝多余的请求。  
    匀速排队模式暂时不支持 QPS > 1000 的场景。
#### 降级
- 降级规则除了流量控制以外，对调用链路中不稳定的资源进行熔断降级也是保障高可用的重要措施之一。我们 需要对不稳定的弱依赖服务调用进行熔断降级，暂时切断不稳定调用，  
避免局部不稳定因素导致整体的雪 崩。熔断降级作为保护自身的手段，通常在客户端（调用端）进行配置。
  - 慢调用比例：选择以慢调用比例作为阈值，需要设置允许的慢调用 RT（即最大的 响应时间），请求的响应时间大于该值则统计为慢调用。当单位统计时长（statIntervalMs）  
  内请求数目大 于设置的最小请求数目，并且慢调用的比例大于阈值，则接下来的熔断时长内请求会自动被熔断。经过熔 断时长后熔断器会进入探测恢复状态（HALF-OPEN 状态），  
  若接下来的一个请求响应时间小于设置的慢 调用 RT 则结束熔断，若大于设置的慢调用 RT 则会再次被熔断
  - 异常比例：异常比例 (ERROR_RATIO)：当单位统计时长（statIntervalMs）内请求数目大于设置的最小请求数目，并 且异常的比例大于阈值，则接下来的熔断时长内请求会自动被熔断。  
  经过熔断时长后熔断器会进入探测恢 复状态（HALF-OPEN 状态），若接下来的一个请求成功完成（没有错误）则结束熔断，否则会再次被熔 断。异常比率的阈值范围是 [0.0, 1.0]，代表 0% - 100%。
  - 异常数：异常数 (ERROR_COUNT)：当单位统计时长内的异常数目超过阈值之后会自动进行熔断。经过熔断时长后 熔断器会进入探测恢复状态（HALF-OPEN 状态），若接下来的一个请求成功完成  
  （没有错误）则结束熔 断，否则会再次被熔断。异常降级仅针对业务异常，对 Sentinel 限流降级本身的异常（BlockException）不生效。
#### 限流算法
- 计数器法：在一开始的时候，我们可以设置一个计 数器counter，每当一个请求过来的时候，counter就加1，如果counter的值大于100并且该请求 与第一个 请求的间隔时间还在1分钟之内，  
那么说明请求数过多；如果该请求与第一个请求的间 隔时间大于1分钟，且counter的值还在限流范围内，那么就重置 counter
- 滑动时间窗口算法。为了解决计数器法统计精度太低的问题，引入了滑动窗口算法。一个时间窗口就是一个时间段。 然后我们将时间窗口进行划分，每个切分的单元都有一个计数器。
  由此可见，当滑动窗口的格子划分的越多，那么滑动窗口的滚动就越平滑，限流的统计就会越精确
  - 滑动窗口由于需要存储多份的计数器（每一个格子存一份），所以滑动窗口在实现上需要更多的存 储空间。
- 漏桶算法：这个桶可以固定水流出的速率。而且，当桶满了之后，多余的 水将会溢出。
- 令牌桶算法：桶一开始是空的，token以 一个固定的速率r往桶里填充，直到达到桶的 容量，多余的令牌将会被丢弃。每当一个请求过来时，就会尝试从桶里移除一个令牌，如果没有  
令牌的话，请求无法通过。
  - 漏桶算法和令牌桶算法最明显的区别是令牌桶算法允许流量一定程度的突发。
  - 因为默认的令牌桶算法，取走token是不需要耗费时间的，也就是说，假设桶内有100个token时， 那么可以瞬间允许100个请求通过。



