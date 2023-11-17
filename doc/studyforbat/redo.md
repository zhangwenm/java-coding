## redo  
#### binlog server层
- binlog用于记录数据库执行的写入性操作(不包括查询)信息，以二进制的形式保存在磁盘中。binlog是mysql的逻辑日志，并且由Server层进行记录，  
使用任何存储引擎的mysql数据库都会记录binlog日志。binlog是通过追加的方式进行写入的，可以通过max_binlog_size参数设置每个binlog文件的  
大小，当文件大小达到给定值之后，会生成新的文件来保存日志。
  - 逻辑日志：可以简单理解为记录的就是sql语句。
  - 因为mysql数据最终是保存在数据页中的，物理日志记录的就是数据页变更。
#### redo日志 持久性 引擎层
- 产生背景
  - 因为Innodb是以页为单位进行磁盘交互的，而一个事务很可能只修改一个数据页里面的几个字节，这个时候将完整的数据页刷到磁盘的话，太浪费资源了！
  - 一个事务可能涉及修改多个数据页，并且这些数据页在物理上并不连续，使用随机IO写入性能太差！
- 组成
  - redo log包括两部分：一个是内存中的日志缓冲(redo log buffer)，另一个是磁盘上的日志文件(redo log file)。mysql每执行一条DML语句，  
  先将记录写入redo log buffer，后续某个时间点再一次性将多个操作记录写到redo log file。这种先写日志，再写磁盘的技术就是MySQL里经常说到的  
  WAL(Write-Ahead Logging) 技术。
- redo log记录形式
  - 前面说过，redo log实际上记录数据页的变更，而这种变更记录是没必要全部保存，因此redo log实现上采用了大小固定，循环写入的方式，当写到结尾时，  
  会回到开头循环写日志。如下图：  
   - ![](./pic/redo.png)
  - 同时我们很容易得知，在innodb中，既有redo log需要刷盘，还有数据页也需要刷盘，redo log存在的意义主要就是降低对数据页刷盘的要求。在上图中，  
  write pos表示redo log当前记录的LSN(逻辑序列号)位置，check point表示数据页更改记录刷盘后对应redo log所处的LSN(逻辑序列号)位置
  - write pos到check point之间的部分是redo log空着的部分，用于记录新的记录；check point到write pos之间是redo log待落盘的数据页更改记录。  
  当write pos追上check point时，会先推动check point向前移动，空出位置再记录新的日志。
  - 启动innodb的时候，不管上次是正常关闭还是异常关闭，总是会进行恢复操作。因为redo log记录的是数据页的物理变化，因此恢复的时候速度比逻辑日志(如binlog)要快很多。  
  重启innodb时，首先会检查磁盘中数据页的LSN，如果数据页的LSN小于日志中的LSN，则会从checkpoint开始恢复。还有一种情况，在宕机前正处于checkpoint的刷盘过程，且数  
  据页的刷盘进度超过了日志页的刷盘进度，此时会出现数据页中记录的LSN大于日志中的LSN，这时超出日志进度的部分将不会重做，因为这本身就表示已经做过的事情，无需再重做。
- 我们只是想让已经提交了的事务对数据库中数据所做的修改永久生效，即使后来系统崩溃，在重启后也能把这种修改恢复出来。所以
我们其实没有必要在每次事务提交时就把该事务在内存中修改过的全部页面刷新到磁盘，只需要把修改了哪些东西记录一下就好，比方说某个事务将系统表空间中的
第100号页面中偏移量为1000处的那个字节的值1改成2我们只需要记录一下：
将第0号表空间的100号页面的偏移量为1000处的值更新为2。
这样我们在事务提交时，把上述内容刷新到磁盘中，即使之后系统崩溃了，重启之后只要按照上述内容所记录的步骤重新更新一下数据页，那么该事务对数据库中所
做的修改又可以被恢复出来，也就意味着满足持久性的要求。因为在系统奔溃重启时需要按照上述内容所记录的步骤重新更新数据页，所以上述内容也被称之为重做日
志，英文名为redo log，我们也可以土洋结合，称之为redo日志。与在事务提交时将所有修改过的内存中的页面刷新到磁盘中相比，只将该事务执行过程中产生
的redo日志刷新到磁盘的好处如下：  
  - redo日志占用的空间非常小 存储表空间ID、页号、偏移量以及需要更新的值。所需的存储空间是很小的，关于redo日志的格式我们稍后会详细唠叨，  
  现在只要知道一条redo日志占用的空间 不是很大就好了。
  - redo日志是顺序写入磁盘的
    在执行事务的过程中，每执行一条语句，就可能产生若干条redo日志，这些日志是按照产生的顺序写入磁盘的，也就是使用顺序IO。
- redo日志会把事务在执行过程中 对数据库所做的所有修改都记录下来，在之后系统崩溃重启后可以把事务所做的任何修改都恢复出来。
- 为了节省redo日志占用的存储空间大小，设计InnoDB的大叔对redo日志中的某些数据还可能进行压缩处理，比方说spacd ID和page number一般占用
  4个字节来存储，但是经过压缩后，可能使用更小的空间来存储。
- 我们前边介绍InnoDB的记录行格式的时候说过，如果我们没有为某个表显式的定义主键，并且表中也没有定义Unique键，那么InnoDB会自动的为表添加一个称之
  为row_id的隐藏列作为主键。为这个row_id隐藏列赋值的方式如下：
  - 服务器会在内存中维护一个全局变量，每当向某个包含隐藏的row_id列的表中插入一条记录时，就会把该变量的值当作新记录的row_id列的值，并且把该变量
    自增1。
  - 每当这个变量的值为256的倍数时，就会将该变量的值刷新到系统表空间的页号为7的页面中一个称之为Max Row ID的属性处（我们前边介绍表空间结构时详细
    说过）
  - 当系统启动时，会将上边提到的Max Row ID属性加载到内存中，将该值加上256之后赋值给我们前边提到的全局变量（因为在上次关机时该全局变量的值可能大
    于Max Row ID属性值）。
- 磁盘上的redo日志文件不只一个，而是以一个日志文件组的形式出现的。这些文件以ib_logfile[数字]（数字可以是0、1、2...）的形式进行命名。
  在将redo日志写入日志文件组时，是从ib_logfile0开始写，如果ib_logfile0写满了，就接着ib_logfile1写，同理，ib_logfile1写满了就去写ib_logfile2，依此类推。如
  果写到最后一个文件该咋办？那就重新转到ib_logfile0继续写，所以整个过程如下图所示：
- InnoDB为记录已经写入的redo日志量，设计了一个称之为Log Sequeue Number的全局变量，翻译过来就是：日志序列号，简称lsn。
- 每一组生成的redo日志都有一个唯一的LSN值与其对应，LSN值越小，说明redo日志产生的越早
- 因为lsn的值是代表系统写入的redo日志量的一个总和，产生多少日志，lsn的值就增加多少（当然有时候要加上log block header和log block trailer的大
  小），日志写到磁盘中时，很容易计算某一个lsn值在redo日志文件组中的偏移
- InnoDB提出了一个全局变量checkpoint_lsn来代表当前系统中可以被覆盖的redo日志总量是多少，
- checkpoint_lsn之前的redo日志都可以被覆盖，也就是说这些redo日志对应的脏页都已经被刷新到磁盘中了，既然它们已经被刷盘，我们就没必要恢复它
  们了。对于checkpoint_lsn之后的redo日志，它们对应的脏页可能没被刷盘，也可能被刷盘了，我们不能确定，所以需要从checkpoint_lsn开始读取redo  
  日志来恢复页面。
#### undo 原子性 引擎层
- undo log主要记录了数据的逻辑变化，比如一条INSERT语句，对应一条DELETE的undo log，对于每个UPDATE语句，对应一条相反的UPDATE的undo log，  
这样在发生错误时，就能回滚到事务之前的数据状态。undo log也是MVCC(多版本并发控制)实现的关键
- 这个事务id本质上就是一个数字，它的分配策略和我们前边提到的对隐藏列row_id（当用户没有为表创建主键和UNIQUE键时InnoDB自动创建的列）的分配策略大
  抵相同，具体策略如下：
  - 服务器会在内存中维护一个全局变量，每当需要为某个事务分配一个事务id时，就会把该变量的值当作事务id分配给该事务，并且把该变量自增1。
  每当这个变量的值为256的倍数时，就会将该变量的值刷新到系统表空间的页号为5的页面中一个称之为Max Trx ID的属性处，这个属性占用8个字节的存储
  空间。
  - 当系统下一次重新启动时，会将上边提到的Max Trx ID属性加载到内存中，将该值加上256之后赋值给我们前边提到的全局变量（因为在上次关机时该全局
  变量的值可能大于Max Trx ID属性值）。
- 聚簇索引的记录除了会保存完整的用户数据以外，而且还会自动添加名为trx_id、roll_pointer的隐藏列，如果
  用户没有在表中定义主键以及UNIQUE键，还会自动添加一个名为row_id的隐藏列。为了实现事务的原子性，InnoDB存储引擎在实际进行增、删、改一条记录时，  
都需要先把对应的undo日志记下来。一般每对一条记录做一次改动，就对应着一条undo日志，但在某些更新记录的操作中，也可能会对应着2条undo日志（更改主键  
值或者不更新主键但任意被更新列数据占用内存变化会先删再插入），这个  
我们后边会仔细唠叨。一个事务在执行过程中可能新增、删除、更新若干条记录，
  也就是说需要记录很多条对应的undo日志，这些undo日志会被从0开始编号，也就是说根据生成的顺序分别被称为第0号undo日志、第1号undo日志、...、第n号undo日
  志等，这个编号也被称之为undo no。
- roll_pointer本质上就是一个指向记录对应的undo日志的一个指针
- 删除
- 
  - 仅仅将记录的delete_mask标识位设置为1，其他的不做修改（其实会修改记录的trx_id、roll_pointer这些隐藏列的值） 
  - 当该删除语句所在的事务提交之后，会有专门的线程后来真正的把记录删除掉。所谓真正的删除就是把该记录从正常记录链表中移除，并且加入到垃
    圾链表中，
- 在对一条记录进行delete mark操作前，需要把该记录的旧的trx_id和roll_pointer隐藏列的值都给记到对应的undo日志中来，就是我们图中显示的old
  trx_id和old roll_pointer属性。这样有一个好处，那就是可以通过undo日志的old roll_pointer找到记录在修改之前对应的undo日志（即插入undo）。
#### double write
- [double write](https://www.cnblogs.com/geaozhang/p/7241744.html).