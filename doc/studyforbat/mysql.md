## Mysql

#### 事务及其ACID属性
- 事务是由一组SQL语句组成的逻辑处理单元,事务具有以下4个属性,通常简称为事务的ACID属性。
  - 原子性(Atomicity) ：事务是一个原子操作单元,其对数据的修改,要么全都执行,要么全都不执行。
  - 一致性(Consistent) ：在事务开始和完成时,数据都必须保持一致状态。这意味着所有相关的数据规
    则都必须应用于事务的修改,以保持数据的完整性。
  - 隔离性(Isolation) ：数据库系统提供一定的隔离机制,保证事务在不受外部并发操作影响的“独
    立”环境执行。这意味着事务处理过程中的中间状态对外部是不可见的,反之亦然。
  - 持久性(Durable) ：事务完成之后,它对于数据的修改是永久性的,即使出现系统故障也能够保持。
- 并发事务处理带来的问题
  - 更新丢失(Lost Update)或脏写：最后的更新覆盖了由其他事务所做的更新。
  - 脏读（Dirty Reads）：：事务A读取到了事务B已经修改但尚未提交的数据
  - 不可重读（Non-Repeatable Reads）：一个事务在读取某些数据后的某个时间，再次读取以前读过的数据，却发现其读出的数据已经发生了改
    变、或某些记录已经被删除了！这种现象就叫做“不可重复读。事务A内部的相同查询语句在不同时刻读出的结果不一致，不符合隔离性
  - 幻读（Phantom Reads）：一个事务按相同的查询条件重新读取以前检索过的数据，却发现其他事务插入了满足其查询条件的新数
    据，这种现象就称为“幻读”。事务A读取到了事务B提交的新增数据，不符合隔离性
- 隔离级别
  - ![](./pic/isolation.png)
  - Mysql默认的事务隔离级别是可重复读，用Spring开发程序时，如果不设置隔离级别默认用Mysql设置的隔
    离级别，如果Spring设置了就用已经设置的隔离级别
- 锁分类
  - 从性能上分为乐观锁(用版本对比来实现)和悲观锁
  - 从对数据库操作的类型分，分为读锁和写锁(都属于悲观锁)
    - 读锁（共享锁，S锁(Shared)）：针对同一份数据，多个读操作可以同时进行而不会互相影响
    - 写锁（排它锁，X锁(eXclusive)）：当前写操作没有完成前，它会阻断其他写锁和读锁
  - 从对数据操作的粒度分，分为表锁和行锁
  - 表锁;每次操作锁住整张表。开销小，加锁快；不会出现死锁；锁定粒度大，发生锁冲突的概率最高，并发度最低；
    一般用在整表数据迁移的场景。
    - 1、对MyISAM表的读操作(加读锁) ,不会阻寒其他进程对同一表的读请求,但会阻赛对同一表的写请求。只有当
      读锁释放后,才会执行其它进程的写操作。
      2、对MylSAM表的写操作(加写锁) ,会阻塞其他进程对同一表的读和写操作,只有当写锁释放后,才会执行其它进
      程的读写操作
  - 行锁：每次操作锁住一行数据。开销大，加锁慢；会出现死锁；锁定粒度最小，发生锁冲突的概率最低，并发度最
    高。
    - InnoDB与MYISAM的最大不同有两点：
      - nnoDB支持事务（TRANSACTION）
      - InnoDB支持行级锁
    - 一个session开启事务更新不提交，另一个session更新同一条记录会阻塞，更新不同记录不会阻塞
  - 总结：MyISAM在执行查询语句SELECT前，会自动给涉及的所有表加读锁,在执行update、insert、delete操作会自
    动给涉及的表加写锁。
    InnoDB在执行查询语句SELECT时(非串行隔离级别)，不会加锁。但是update、insert、delete操作会加行
    锁。
    简而言之，就是读锁会阻塞写，但是不会阻塞读。而写锁则会把读和写都阻塞。
#### 优化
- 1、MySQL支持两种方式的排序filesort和index，Using index是指MySQL扫描索引本身完成排序。index 效率高，filesort效率低。 
- 2、order by满足两种情况会使用Using index。 1) order by语句使用索引最左前列。 2) 使用where子句与order by子句条件列组合满足索引最左前列。 
- 3、尽量在索引列上完成排序，遵循索引建立（索引创建的顺序）时的最左前缀法则。 
- 4、如果order by的条件不在索引列上，就会产生Using filesort。
- 5、能用覆盖索引尽量用覆盖索引 
- 6、group by与order by很类似，其实质是先排序后分组，遵照索引创建顺序的最左前缀法则。  
对于group by的优化如果不需要排序的可以加上order by null禁止排序。注意，where高于having，能写在where中 的限定条件就不要去having限定了。
##### Using filesort文件排序原理详解
- 单路排序会把所有需要查询的字段都放到 sort buffer 中，而双路排序只会把主键 和需要排序的字段放到 sort buffer 中进行排序，然后再通过主键回到原表查询需要的字段。
  - 单路排序：是一次性取出满足条件行的所有字段，然后在sort buffer中进行排序；用trace工具可 以看到sort_mode信息里显示< sort_key, additional_fields >  
  或者< sort_key, packed_additional_fields >
  - 双路排序（又叫回表排序模式）：是首先根据相应的条件取出相应的排序字段和可以直接定位行 数据的行 ID，然后在 sort buffer 中进行排序，排序完后需要再次取回其它  
  需要的字段；用trace工具 可以看到sort_mode信息里显示< sort_key, rowid >
#### 死锁
[mysql死锁](https://segmentfault.com/a/1190000037510033)