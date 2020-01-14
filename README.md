# DataSyncTool（关系型数据同步工具）

一款Java开发的数据同步软件，支持mysql,sqlserver,oracle之间的数据同步，理论上支持任何jdbc可访问的关系数据库。本软件是由实际项目驱动而开发，之后抽取成为单独项目并开源。
支持定时同步与固定频率同步，全量同步和增量同步，以及分页查询同步。

**主要使用技术**：spring boot,spring jdbc,cron定时表达式
**数据同步策略**：全量同步,增量同步

### 全量同步
每次同步时，先清空目标表所有记录，再从源表查出所有记录并插入到目标表。
### 增量同步
每次同步时，获取出上次同步的分界点，再从源表的分界点开始增量同步到目标表。

### 使用说明
修改spring-config.xml配置以下步骤

#### 1.配置数据源

##### （1）【注】以下配置：MySQL版本为5.7 ~ 8.0以下的配置：

```xml
<bean id="target-ds" parent="datasource-parent">
	<property name="name" value="target-ds" />
	<property name="driverClassName" value="com.mysql.jdbc.Driver" />
	<property name="url" value="jdbc:mysql://127.0.0.1:3306/cc?useUnicode=true&amp;characterEncoding=utf8" />
	<property name="username" value="root" />
	<property name="password" value="password" />
	<property name="dbType" value="mysql" />
</bean>
<bean id="origin-mysql" parent="datasource-parent">
	<property name="name" value="origin-mysql" />
	<property name="driverClassName" value="com.mysql.jdbc.Driver" />
	<property name="url" value="jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&amp;characterEncoding=utf8" />
	<property name="username" value="root" />
	<property name="password" value="password" />
	<property name="dbType" value="mysql" />
</bean>
```
##### （2）【注】以下配置：MySQL版本为8.0以上的配置

```xml
<!-- 目标数据源配置 -->
	<bean id="target-ds" parent="datasource-parent">
		<property name="name" value="target-ds" />
		<property name="driverClassName" value="com.mysql.cj.jdbc.Driver" />
		<property name="url" value="jdbc:mysql://127.0.0.1:3306/xxgk?useUnicode=true&amp;characterEncoding=utf-8&amp;useSSL=false&amp;serverTimezone=GMT%2B8" />
		<property name="username" value="root" />
		<property name="password" value="root" />
		<property name="dbType" value="mysql" />
	</bean>
```

#### 2.配置同步Job

##### 全量同步配置
```xml
<bean id="datasync1" class="com.zle.datasync.job.DataSyncJob"
	p:originds-ref="origin-mysql" p:targetds-ref="target-ds" p:strategy="all"
	p:selectSql="select id,name,`status`,remark from demo"
	p:insertSql="insert into t_demo(id,name,`status`,remark) values(:id,:name,:status,:remark)"
	p:deleteSql="delete from t_demo"/>
```
**参数说明**
> strategy：all 表示全量同步。
> originds：源数据源
> targetds：目标数据源
> selectSql：查询源数据表的sql语句
> insertSql：插入到目标表的sql，切记字段要与selectSql中的一一对应
> deleteSql：执行全量同步时，需要先清空目标表记录，这里配置清空目标表的sql

##### 增量同步配置
```xml
<bean id="datasync2" class="com.zle.datasync.job.DataSyncJob"
	p:originds-ref="origin-mysql" p:targetds-ref="target-ds"
	p:strategy="delta" p:pageSize="1" p:startTime="2014-01-01 00:00:00"
	p:selectSql="select id,name,`status`,remark,birthday,height,create_time from demo where create_time>:startTime limit :startRow,:pageSize" 
	p:insertSql="insert into t_demo(id,name,`status`,remark,birthday,height,create_time) values(:id,:name,:status,:remark,:birthday,:height,:create_time)">
</bean>
```
**参数说明**
> strategy：delta 表示增量同步。
> originds：源数据源
> targetds：目标数据源
> pageSize：分页查询大小
> startTime：增量同步要求源表必须有一个时间字段，用于计算分界点。此参数用于配置第一次同步时，从哪个时间点开始同步数据，之后系统会记录最新的时间点，并保存到syncinf.properties文件中。系统优先从syncinf.properties文件中取，若取不到再使用本参数。因此若配置了此参数，系统只在第一次同步时使用（因为此时syncinf.properties文件中是空的），之后会保存到文件中，优先使用文件中的参数。新配置的同步任务，请在此处配置startTime。
> selectSql：查询源数据表的sql语句
> insertSql：插入到目标表的sql，切记字段要与selectSql中的一一对应


**sql动态参数说明**
> 	有必要着重解释一下selectSql中的几个动态参数，因为增量同步是针对大数据量设计的，所以在执行查询时，不能一次将所有数据查询出来，这样做有内存溢出的风险。因此，分页查询是必须要使用的。本软件巧妙的将sql语句配置出来，就是为了解决不同数据库分页查询语句不一致。若不配置出来，由系统去计算分页语句，那么计算过程太复杂，支持的数据库也是有限的（前面说过“理论上支持任何jdbc可访问的关系数据库”就是这个道理）。将sql语句配置出来后，开发者还可以按照需求定制条件，过滤掉无需同步的数据，岂不妙哉。
> 	既然将sql语句配置出来，且要支持分页查询，那么必然需要将分页信息动态传参给sql语句。以下4个参数，系统会判断如果存在那么动态传递进去，由jdbcTemplate负责替换并执行。
> 	startTime：开始时间分界点
> 	startRow：开始行，动态计算
> 	endRow：结束行，动态计算
> 	pageSize：分页大小，会替换为bean中的pageSize值
> 	虽然说这4个参数都不是必须的，但如果不用startTime标识分界点，那么和全量同步又有什么分别呢？然后也没有限制一定要使用分页查询，因为有的系统数据增长不快，可以一次将新增的所有数据查询并同步到目标表。

#### 3.配置定时器
```xml
<task:scheduled-tasks scheduler="taskScheduler">
	<task:scheduled ref="datasync1" method="execute" cron="1/10 * * * * ?"/>
	<!-- <task:scheduled ref="datasync2" method="execute" cron="1/10 * * * * ?"/> -->
</task:scheduled-tasks>
```

> 本示例使用cron表达式配置定时器，也可以配置固定频率同步，请参考spring的定时配置文档

#### 4.运行程序
```shell
java com.zle.datasync.Application
```

#### 5.具体操作

###### 5.1 配置目标数据源

###### 5.2 配置源数据源

###### 5.3 设置全量同步同步/增量同步配置；

###### 5.4 以增量同步为例

```xml
<!-- 增量同步配置 -->
<bean id="datasync" class="com.jointsky.datasync.job.DataSyncJob"
	p:originds-ref="origin-sqlserver" p:targetds-ref="target-ds"
	p:strategy="delta" p:pageSize="1" p:startTime="2020-01-01 00:00:00"
	p:selectSql="SELECT ps_code, mp_code, monitor_time, gk_name, remark, data_status FROM 			domestic_garbage_burn_electricity_gk_data" 
	p:insertSql="insert into domestic_garbage_burn_electricity_gk_data(ps_code, mp_code, 			monitor_time, gk_name, remark, data_status) values(:ps_code, :mp_code, 					:monitor_time, :gk_name, :remark, :data_status)">
</bean>
```

###### 【注】![](E:\PersonalWork\SysconDataSourcesTools\datasync\images\增量同步.jpg)

参考了大神的项目 感谢<https://gitee.com/zhanngle/datasync>