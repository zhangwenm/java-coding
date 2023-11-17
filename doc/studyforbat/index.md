## 索引  

#### 索引概念
Innodb将数据按某些列有序存放在数据页中，而这些数据页会形成一个双向链表并且，     
下一页中该列的值必须大于上一页中该列的值。为了提升查询效率，把每一页中该列  
的最小值和页编号拿出来形成一个个目录项记录，按存储用户记录同样的方式也存放  
数据页中，这些目录项就是索引。只有最下层存放的是用户记录的页，其他层都是目    
录项记录页。这样的树状结构就是B+树    
- ![tree!](./pic/tree.png "树")   

#### B+树
从上图我们可以看出，无论是用户记录的页，还是目录项记录的页我们都把他存放在  
在了树中。我们把这些页叫做节点。最底层的节点为叶子节点，其他非叶子子节点称  
为内节点。最上层那个节点为根节点。非叶子结点不保存数据的指针（指向数据内存中的地址）  
只进行搜索（区别于B-树），而叶子节点则保存了指向数据的指针
- B+树
  - 每个元素不保存数据，只用来索引，所有数据都保存在叶子节点。
  - 且叶子结点本身依关键字的大小自小而大顺序链接。
  - 所有的中间节点元素都同时存在于子节点，在子节点元素中是最大（或最小）元素。
  - 叶子节点双向链表，便于范围查询。
- B树也就是B-树，多路平衡查找树
  - 根节点至少有两个孩子节点
  - 每个中间节点至少含有m-1个元素和m个孩子节点
  - 这个m-1个元素为n个孩子节点的值域划分，自平衡
- 层级更少，减少IO(取决于属的高度)
- 全节点遍历更快（只需要遍历叶子节点 ）
- B树有点：只要找到关键字就可以定位相应数据（每个几点都有指向数据的指针），性能不稳定

#### 聚簇索引  
- 一般为主键或唯一索引列，innodb会自动创建聚簇索引叶子节点内存放了完整的用户记录  
- 使用记录主键或唯一性索引进行记录和页排序。
    - 页内的记录按照主键排序成单项链表
    - 用户记录页之间也按主键排序成双向链表
    - 存放目录项记录的页分为不同层次，同一层次中页也根据页中目录项记录中的主键排序  
    成双向链表
    - 存放目录项记录的页，每条记录内容为页编号和该页中索引列最小值

#### 二级索引  
以其他列作为规则建立起来的B+树索引结构，目录项中除了索引列和页号之外，为了保证目录项  
的唯一性，还会存放主键值。叶子节点存放的是索引列和主键值。如果需要查找用户记录还需拿  
到主键值后再去聚簇索引中进行查询。  

#### 联合索引  
在多个列上建立的二级索引。例如（a,b）这个联合索引，会先根据a进行排序，然后在此基础上  
在按b排序。

#### 页存放
一个页中最少可以存放两条记录

#### Myisam索引  
将数据和索引分别存放在两个文件，把记录按顺序写入数据文件中。索引结构中的叶子节点存放的  
是主键值和记录行号。先通过索引拿到行号，再去数据文件中拿到数据。如果行格式为定长，也就  
是每行占用的空间是固定的，则可以很方便算出数据偏移量，通过偏移量可以很快拿到数据。
#### 索引的代价
- 空间上的代价
> 这个是显而易见的，每建立一个索引都要为它建立一棵B+树，每一棵B+树的每一个节点都是一个数据页，一个页默认会占用16KB的存储空间，一棵很大的B+树由
许多数据页组成，会占用很大的一片存储空间。
- 时间上的代价
> 每次对表中的数据进行增、删、改操作时，都需要去修改各个B+树索引。而且我们讲过，B+树每层节点都是按照索引列的值从小到大的顺序排序而组成了双向链
表。不论是叶子节点中的记录，还是内节点中的记录（也就是不论是用户记录还是目录项记录）都是按照索引列的值从小到大的顺序而形成了一个单向链表。而
增、删、改操作可能会对节点和记录的排序造成破坏，所以存储引擎需要额外的时间进行一些记录移位，页面分裂、页面回收等操作来维护好节点和记录的排
序。如果我们建了许多索引，每个索引对应的B+树都要进行相关的维护操作，会耗费时间
####  B+树索引适用的条件
- 表中的主键是id列，它存储一个自动递增的整数。所以InnoDB存储引擎会自动为id列建立聚簇索引。
- 额外定义了一个二级索引idx_name_birthday_phone_number（name、birthday、phone_number）
- idx_name_birthday_phone_number索引对应的B+树中页面和记录的排序方式就是这样
  的：
> 先按照name列的值进行排序。 如果name列的值相同，则按照birthday列的值进行排序。如果birthday列的值也相同，则按照phone_number的值进行排序。
- 全值匹配 如果我们的搜索条件中的列和索引列一致的话，这种情况就称为全值匹配
  - >因为B+树的数据页和记录先是按照name列的值进行排序的，所以先可以很快定位name列的值是Ashburn的记录位置。
    在name列相同的记录里又是按照birthday列的值进行排序的，所以在name列的值是Ashburn的记录里又可以快速定位birthday列的值是'1990-09-27'的记录。
    如果很不幸，name和birthday列的值都是相同的，那记录是按照phone_number列的值排序的，所以联合索引中的三个列都可能被用到。
- 匹配左边的列 不用包含全部联合索引中的列，只包含左边的就行 如果我们想使用联合索引中尽可能多的列，搜索条件中的各个列必须是联合索引中从最左边连续的列
  - >SELECT * FROM person_info WHERE name = 'Ashburn';
    或者包含多个左边的列也行：
    SELECT * FROM person_info WHERE name = 'Ashburn' AND birthday = '1990-09-27';
    > 下边的语句就用不到这个B+树索引
    SELECT * FROM person_info WHERE birthday = '1990-09-27';
    > B+树的数据页和记录先是按照name列的值排序的，在name列的值相同的情况下才使用birthday列进行排序，也就是说name列的值不同的记录
    中birthday的值可能是无序的。而现在你跳过name列直接根据birthday的值去查找
- 匹配列前缀
  - > 字符串排序的本质就是比较哪个字符串大一点儿，哪个字符串小一点
  - > SELECT * FROM person_info WHERE name LIKE 'As%';
  - >但是需要注意的是，如果只给出后缀或者中间的某个字符串，比如这样：
  - >SELECT * FROM person_info WHERE name LIKE '%As%';
    > MySQL就无法快速定位记录位置
- 匹配范围值 所有记录都是按照索引列的值从小到大的顺序排好序的
  - >SELECT * FROM person_info WHERE name > 'Asa' AND name < 'Barlow';  
    找到name值为Asa的记录。
    找到name值为Barlow的记录。
    由于所有记录都是由链表连起来的（记录之间用单链表，数据页之间用双链表），所以他们之间的记录都可以很容易的取出来
    找到这些记录的主键值，再到聚簇索引中回表查找完整的记录。
  - > 如果对多个列同时进行范围查找的话，只有对索引最左边的那个列进行范围查找的时候才能用到B+树索引
    > SELECT * FROM person_info WHERE name > 'Asa' AND name < 'Barlow' AND birthday > '1980-01-01';
    >通过条件name > 'Asa' AND name < 'Barlow'来对name进行范围，查找的结果可能有多条name值不同的记录， 对这些name  
    > 值不同的记录继续通过birthday > '1980-01-01'条件继续过滤。 这样子对于联合索引idx_name_birthday_phone_number来说，  
    > 只能用到name列的部分，而用不到birthday列的部分，因为只有name值相同的情况下才能用birthday列 的值进行排序，而这个查询中  
    > 通过name进行范围查找的记录中可能并不是按照birthday列进行排序的，所以在搜索条件中继续以birthday列进行查找时是用不到这 个B+树索引的。
- 精确匹配某一列并范围匹配另外一列
  - SELECT * FROM person_info WHERE name = 'Ashburn' AND birthday > '1980-01-01' AND birthday < '2000-12-31' AND phone_number > '15100000000';
    - 这个查询的条件可以分为3个部分：1. name = 'Ashburn'，对name列进行精确查找，当然可以使用B+树索引了。
    - 2birthday > '1980-01-01' AND birthday < '2000-12-31'，由于name列是精确查找，所以通过name = 'Ashburn'条件查找后得到的结果的name值都是相同的，  
    它们会再按照birthday的值进行排序。所以此时对birthday列进行范围查找是可以用到B+树索引的。
    - 3. phone_number > '15100000000'，通过birthday的范围查找的记录的birthday的值可能不同，所以这个条件无法再利用B+树索引了，只能遍历上一步查询得到的
         记录。
- 不可以使用索引进行排序的几种情况
  - ASC、DESC混用 SELECT * FROM person_info ORDER BY name, birthday DESC LIMIT 10;
  - WHERE子句中出现非排序使用到的索引列 SELECT * FROM person_info WHERE country = 'China' ORDER BY name LIMIT 10;
  - 排序列包含非同一个索引的列 SELECT * FROM person_info ORDER BY name, country LIMIT 10;
  - 排序列使用了复杂的表达式 SELECT * FROM person_info ORDER BY UPPER(name) LIMIT 10;
- 用于分组
  - SELECT name, birthday, phone_number, COUNT(*) FROM person_info GROUP BY name, birthday, phone_number
  - 如果没有索引的话，这个分组过程全部需要在内存里实现，而如果有了索引的话，恰巧这个分组顺序又和我们的B+树中的索引列的顺序是一致的，  
  而我们的B+树索引又是按照索引列排好序的，这不正好么，所以可以直接使用B+树索 引进行分组。
- 回表的代价
  - SELECT * FROM person_info WHERE name > 'Asa' AND name < 'Barlow';
    - 在Asa～Barlow之间的记录在磁盘中的存储是相连的，集中分布在一个或几个数据页中，我们可以很快的把这些连着的记录从磁盘中读出来，这种读取方式我们也可以称为顺序I/O。
    - 根据第1步中获取到的记录的id字段的值可能并 不相连，而在聚簇索引中记录是根据id（也就是主键）的顺序排列的，所以根据这些并不连续的id值到聚簇索引中访问完整的用户记  
    录可能分布在不同的数据页中， 这样读取完整的用户记录可能要访问更多的数据页，这种读取方式我们也可以称为随机I/O
    - 顺序I/O比随机I/O的性能高
    - 需要回表的记录越多，使用二级索引的性能就越低，甚至让某些查询宁愿使用全表扫描也不使用二级索引，这个就是查询优化器做的工作
  - SELECT * FROM person_info ORDER BY name, birthday, phone_number;
    - 由于查询列表是*，所以如果使用二级索引进行排序的话，需要把排序完的二级索引记录全部进行回表操作，这样操作的成本还不如直接遍历聚簇索引然后再进行文件
      排序（filesort）低，所以优化器会倾向于使用全表扫描的方式执行查询。如果我们加了LIMIT子句，比如这样：
      SELECT * FROM person_info ORDER BY name, birthday, phone_number LIMIT 10;
      这样需要回表的记录特别少，优化器就会倾向于使用二级索引 + 回表的方式执行查询。
- 覆盖索引
  - 最好在查询列表里只包含索引列，比如这样：
    - SELECT name, birthday, phone_number FROM person_info WHERE name > 'Asa' AND name < 'Barlow'
- 索引下推
  - 首先根据索引来查找记录，然后再根据where条件来过滤记录；在支持ICP优化后，MySQL会在取出索引的同时，判断是否可以进行  
  where条件过滤再进行索引查询，也就是说提前执行where的部分过滤操作，在某些场景下，可以大大减少回表次数，从而提升整体性能。
  - 索引能用上的根本因素就是排好了序，联合索引先按name排序，name一样再按age排序，比如使用name="张三" and age > 18，  
这个就能使用联合索引的所有列，因为name都是张三，那么索引中的name是张三的age列也是有序的。但是如果使用like "张%"，这时  
索引中name可能有张一，张二，张三的顺序，name不一样导致了它们的age不一定有序，可能是20,10,30 ；就会导致age不能用在索引搜索，  
5.6之前就会是where过滤，5.6之后通过索引下推在索引name的同时过滤age
- 如何挑选索引
  - 只为用于搜索、排序或分组的列创建索引
  - 考虑列的基数  某个列包含值2, 5, 8, 2, 5, 8, 2, 5, 8，虽然有9条记录，但该列的基数却是3。
    - 在记录行数一定的情况下，列的基数越大，该列中的值越分散，列的基数越小，该列中的值越集中
    - 最好为那些列的基数大的列建立索引，为基数太小列的建立索引效果可能不好。
  - 索引列的类型尽量小
    - 数据类型越小，在查询时进行的比较操作越快
    - 数据类型越小，索引占用的存储空间就越少，在一个数据页内就可以放下更多的记录，从而减少磁盘I/O带来的性能损耗，也就意味着可以把更多的数据页缓存
      在内存中，从而加快读写效率。
  - 索引字符串值的前缀
    - 只对字符串的前几个字符进行索引
    - 只索引字符串值的前缀的策略是我们非常鼓励的，尤其是在字符串类型能存储的字符比较
      多的时候。
    - 使用索引列前缀的方式无法支持使用索引排序
  - 让索引列在比较表达式中单独出现
    - 1. WHERE my_col * 2 < 4
    - 2. WHERE my_col < 4/2
    - 第1个WHERE子句中my_col列并不是以单独列的形式出现的，而是以my_col * 2这样的表达式的形式出现的，存储引擎会依次遍历所有的记录，计算这个表达式的值是
      不是小于4，所以这种情况下是使用不到为my_col列建立的B+树索引的。而第2个WHERE子句中my_col列并是以单独列的形式出现的，这样的情况可以直接使用B+树索
      引。
  - 主键插入顺序
    - 如果我们插入的主键值忽大忽小的话，涉及到页分裂，损耗性能
#### 总结
- 上边只是我们在创建和使用B+树索引的过程中需要注意的一些点，后边我们还会陆续介绍更多的优化方法和注意事项，敬请期待。本集内容总结如下：
  - B+树索引在空间和时间上都有代价，所以没事儿别瞎建索引。
  - B+树索引适用于下边这些情况：
    - 全值匹配
    - 匹配左边的列
    - 匹配范围值
    - 精确匹配某一列并范围匹配另外一列
    - 用于排序
    - 用于分组
2. 在使用索引时需要注意下边这些事项：
   - 只为用于搜索、排序或分组的列创建索引
   - 为列的基数大的列创建索引（区分度高）
   - 索引列的类型尽量小（比较计算时快）
   - 可以只对字符串值的前缀建立索引
   - 只有索引列在比较表达式中单独出现才可以适用索引
   - 为了尽可能少的让聚簇索引发生页面分裂和记录移位的情况，建议让主键拥有AUTO_INCREMENT属性。
   - 定位并删除表中的重复和冗余索引
   - 尽量使用覆盖索引进行查询，避免回表带来的性能损耗。    
#### sql分析
- EXPLAIN
  - EXPLAIN语句来帮助我们查看某个查询语句的具体执行计划
    - select_type SELECT关键字对应的那个查询的类型（是单表查询、还是关联查询、还是包含子查询等）
    - type表明了这个访问方法是个啥
      - system：当表中只有一条记录并且该表使用的存储引擎的统计数据是精确的，比如MyISAM、Memory，那么对该表的访问方法就是system。
      - const：就是当我们根据主键或者唯一二级索引列与常数进行等值匹配时，对单表的访问方法就是const
      - eq_ref：在连接查询时，如果被驱动表是通过主键或者唯一二级索引列等值匹配的方式进行访问的（如果该主键或者唯一二级索引是联合索引的话，所有的索引列都必须进行等值比较），
      则对该被驱动表的 访问方法就是eq_ref
      - ref：当通过普通的二级索引列与常量进行等值匹配时来查询某个表，那么对该表的访问方法就可能是ref
      - ref_or_null：当对普通二级索引进行等值匹配查询，该索引列的值也可以是NULL值时，那么对该表的访问方法就可能是ref_or_null
      - ALL：全表扫描
    - possible_keys和key：在EXPLAIN语句输出的执行计划中，possible_keys列表示在某个查询语句中，对某个表执行单表查询时可能用到的索引有哪些，key列表示实际用到的索引有哪些
    - rows：如果查询优化器决定使用全表扫描的方式对某个表执行查询时，执行计划的rows列就代表预计需要扫描的行数，如果使用索引来执行查询时，执行计划的rows列就代表预计扫描的索  
    引记录行数。
    - filesort：很多情况下排序操作无法使用到索引，只能在内存中（记录较少的时候）或者磁盘中（记录较多 的时候）进行排序，设计MySQL的大叔把这种在内存中或者磁盘上进行排序的方式统称为  
    文件排序（英文名：filesort）。如果某个查询需要使用文件排序的方式执行查询，就会在执行计划
      的Extra列中显示Using filesort提示
- optimizer
  - 我们所说的基于成本的优化主要集中在optimize阶段
    - prepare阶段
    - optimize阶段：对于单表查询来说，我们主要关注optimize阶段的"rows_estimation"这个过程，这个过程深入分析了对单表查询的各种执行方案的成本
    - execute阶段：对于多表连接查询来说，我们更多需要关注"considered_execution_plans"这个过程，这个过程里会写明各种不同的连接方式所对应的成本
  - 优化器最终会选择成本最低的那种方案来作为最终的执行计划，
    也就是我们使用EXPLAIN语句所展现出的那种方案。
- EXPLAIN展现出来的方案就是optimizer展现出来的方案中成本最低的方案
