## git拉取问题
clone
git clone -b master git@github.com:zhangwenm/mynacos.git
connection reset
git config --global http.sslVerify "false"
Failed to connect to github.com port 443:connection timed out
git config --global --unset http.proxy
git config --global --unset https.proxy
## idea terminal执行mvn clean install -Dmaven.test.skip=true 报错
原因：idea命令行环境不支持，
解决办法：需要在bash执行 或者替换成 mvn clean install package '-Dmaven.test.skip=true'

## idea maven 编译  Malformed \uxxxx encoding
原因：可能电脑非正常关机导致jar index异常
解决办法：更改maven本地仓库，重新下载jar


##Maven 依赖冲突
1 定位冲突类
2 maven helper查看冲突jar
3 排除冲突依赖


mac idea terminal 执行maven命令找不到java
配置maven bin下mvn文件


## git相关
创建斌不过切换到新分支（在dev基础上创建新分支dev1）
git checkout -b dev1 dev
删除分支dev1
git branch -d dev1

## idea  Command line is too long. Shorten the command line via JAR manifest or via a classpath file and rerun.
修改项目下 .idea\workspace.xml，找到标签 <component name="PropertiesComponent"> ， 在标签里加一行  <property name="dynamic.classpath" value="true" />
## ssh
### 生成秘钥
1. ssh-keygen -t rsa -C "14794612149@163.com"
#在Enter file in which to save the key  行中，输入： id_rsa_XXXX，生成自定义名称的秘钥文件
生成文件存放路径
C:\Users\14794\.ssh

2. 将公钥加入到 gitlab/github 的SSH key中，这里只展示如何加入到github中
    1. github SSH and GPG keys： ssh keys 点击new
    2. 定义title，将id_sa_xxxx文件中的秘钥粘贴入，完成
3. C:\Users\14794\.ssh文件下创建config文件定义多账户
````
   # github account
Host github.com
HostName github.com
User iJarmin
IdentityFile C:\\Users\\iJarmin\\.ssh\\id_rsa_github
IdentitiesOnly yes
PreferredAuthentications publickey

# gitlab account
Host gitlab.com
HostName gitlab.com
User iJarmin_lab
IdentityFile C:\\Users\\iJarmin\\.ssh\\id_rsa_gitlab
IdentitiesOnly yes
PreferredAuthentications publickey
-----------------------------------
windows下配置git多账户
https://blog.51cto.com/u_15077536/4340688
````


git remote add upstream https://github.com/zhangwenm/mynacos.git

git remote add upstream https://github.com/alibaba/nacos.git



MAVEN_HOME = C:\appstore\apache-maven-3.8.1
path = %MAVEN_HOME%\bin


## 重复双引号消除问题
String skuId = skuId.replaceAll("\"","");

json字符串截取key value
String msg = message.getText();
//获取sku_id位置ceshi
int start=msg.indexOf("\"skuId\"");
//获取sku_id后第一个逗号位置
int last= msg.indexOf(",",start);
//截取sku_id key value
String res=msg.substring(start,last);
String[] arr = res.split(":");

## 运算
%取模
/除

## mapstruct
- 定义集合转换的方法时先定义单个实体转换的方法
- lombok和mapstruct冲突
  <plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
  <source>11</source>
  <target>11</target>
  <annotationProcessorPaths>
  <path>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct-processor</artifactId>
  <version>1.5.2.Final</version>
  </path>
  <path>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <version>1.18.18</version>
  </path>
  <!-- This is needed when using Lombok 1.18.16 and above -->
  <path>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok-mapstruct-binding</artifactId>
  <version>0.2.0</version>
  </path>
  </annotationProcessorPaths>
  </configuration>
  </plugin>












gms_product_productBase：十亿
gms_product_specAtt：亿级
gms_custom_expandSetting：千万
gms_custom_propSetting：千万级
gms_product_qualification：千






