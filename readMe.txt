使用数据库水平分片
1、数据库水平分片功能在newsee-database类库中实现，版本0.0.2-SNAPSHOT。需要在MAVEN POM文件中引入该依赖，并升级相关依赖：

<dependency>
    <groupId>com.newsee</groupId>
    <artifactId>newsee-database</artifactId>
    <version>0.0.2-SNAPSHOT</version>
</dependency>
<!--分页插件-->
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper</artifactId>
    <version>5.1.11</version>
</dependency>
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>1.2.13</version>
</dependency>

2、应用入口上，取消掉Druid数据源自动初始化

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class, DruidDataSourceAutoConfigure.class})

3、配置文件中修改数据源配置，增加读写分离、水平分库自定义策略、水平分库范围设置、广播表及其主键生成设置，具体见配置说明



开发要求
java

不能使用Map作为Controller参数，反序列化时会造成部分参数的类型不固定（如短整数反序列化为Integer，长整数解析为了Long）。需要使用POJO类反序列化请求参数，ID主键须使用Long类型。
新线程中调用DAO方法时，跟原来一样，需要从上下文中获取和设定数据源。方法略有变化：
String datasource = DataSourceContextHolder.getLocal().getDataSource();
DataSourceContextHolder.getLocal().setDatasource(datasource);
使用多组数据源（如newsee-report中同时访问charge、bill和owner数据库），并且数据源按照上面的规则进行了分库，则需要同时指定分片分组（rangeGroup）和分片信息。
如需要访问bill库时，需要使用DataSourceContextHolderForReport.getLocal().setDatasourceByGroupAndPrecinctId(DataSourceEnum.Bill.getValue(), precinctIdsList)。
如果不指定分片参数，只指定分组，则会在该分组上的所有分片库上执行SQL。

不需要指定ReadDataSource路由到从库。系统会根据SQL类型自动路由。R类型语句会自动路由到slave从库，CUD类型自动路由到master主库。从库数据一般有一定的复制延迟，如果数据实时性要求较高，可以使用WriteDataSource指定（R）读数据时使用master主库。
不要在新线程（asyncService、ThreadPoolTaskExecutor等）中远程调用已经分片的微服务（目前有charge和bill）。因为新线程的上下文中，没有了controll层的Header信息，会造成分库路由失败。路由失败时，会在所有分库执行SQL语句，查询、更新、删除类的语句还好，插入语句就会造成数据重复。如果必须使用新线程，可以在请求Header或者Parameter中指定路由需要的参数（precinctId、precinctIds、enterpriseId、enterpriseIds、orderNo、orderNos）。
SQL

Group By语句中，结果列中除了聚合计算列，其余的列必须全部放在Group By后。
尽可能的把order by和group by字段放在结果集中，避免sharding jdbc改写查询语句。
取子字符串函数不支持SUBSTR，需要用SUBSTRING
仅可能的不要使用GROUP_CONCAT，因为动态组装的数据可能会很大，JDBC会自动识别为BLOB类型，而Sharding-jdbc目前不支持BLOB。建议：1、使用distinct缩减长度，如GROUP_CONCAT(DISTINCT s.HouseId) as houseIdList；2、取子串SUBSTRING来限定长度
开启日志
开启方法：

1、newsee-logback.xml中添加如下2行：

<logger name="com.newsee.database.aop" level="DEBUG" />
<logger name="com.newsee.database.sharding" level="DEBUG" />

2、应用配置中添加如下内容：

# 日志中显示实际SQL和在哪个分片库执行
spring.shardingsphere.props.sql.show=true
日志样例

【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - x-forwarded-for, Value - 192.168.1.27, 192.168.1.95
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - appclienttype, Value - pc
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - origin, Value - http://192.168.1.95
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - precinctid, Value - 414237
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - user-agent, Value - Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - content-type, Value - application/json;charset=UTF-8
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - accept, Value - application/json, text/plain, */*
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - appid, Value - 07d8737811434732
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - funcid, Value - newsee-charge-root-funcid-paymentlist
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - token, Value - eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImp0aSI6IjEwNDA1NiIsImlhdCI6MTU5MDExNzc2OH0.4REF8NfvfoD1s-5DhYFwuTQAuuj1IwzhQxFhk5b5yIw
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - referer, Value - http://192.168.1.95/
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - accept-encoding, Value - gzip, deflate
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - accept-language, Value - zh-CN,zh;q=0.9
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - companylevelorganizationid, Value - 197093
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - x-forwarded-proto, Value - http
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - x-forwarded-port, Value - 80
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - organizationid, Value - 197093
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - companyid, Value - 0
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - grouplevelorganizationid, Value - 197093
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - x-forwarded-host, Value - 192.168.1.95
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - usertelephone, Value - 18340874240
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - x-forwarded-prefix, Value - /charge
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - enterpriseid, Value - 974
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - username, Value - admin
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - content-length, Value - 36941
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - host, Value - 192.168.1.96:8778
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Requst Header Name - connection, Value - Keep-Alive
【newsee_charge】2020-05-22 11:54:00.734 [http-nio-8778-exec-7] DEBUG com.newsee.database.aop.DataSourceAopInController - Got targetShardingDb[charge11] by precinct[414237] from request header/parameter.
【newsee_charge】2020-05-22 11:54:00.767 [http-nio-8778-exec-7] DEBUG c.n.c.d.C.listPageStandrd_COUNT - ==> Preparing: SELECT count(0) FROM Charge_CustomerChargeDetail d LEFT JOIN charge_customerchargecalctask t ON d.TaskId = t.Id WHERE d.EnterpriseId = ? AND d.HouseId = ? AND d.isDelete = ?
【newsee_charge】2020-05-22 11:54:00.767 [http-nio-8778-exec-7] DEBUG c.n.c.d.C.listPageStandrd_COUNT - ==> Parameters: 974(Long), 414257(Long), 0(String)
【newsee_charge】2020-05-22 11:54:00.767 [http-nio-8778-exec-7] DEBUG c.n.database.sharding.NSDbHintShardingAlgorithm - Matched Datasource[charge11] by DataSourceContextHolder.
【newsee_charge】2020-05-22 11:54:00.767 [http-nio-8778-exec-7] INFO ShardingSphere-SQL - Rule Type: sharding
【newsee_charge】2020-05-22 11:54:00.767 [http-nio-8778-exec-7] INFO ShardingSphere-SQL - Logic SQL: SELECT count(0) FROM Charge_CustomerChargeDetail d LEFT JOIN charge_customerchargecalctask t ON d.TaskId = t.Id WHERE d.EnterpriseId = ? AND d.HouseId = ? AND d.isDelete = ?
【newsee_charge】2020-05-22 11:54:00.767 [http-nio-8778-exec-7] INFO ShardingSphere-SQL - SQLStatement: SelectSQLStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@301d34cc, tablesContext=TablesContext(tables=[Table(name=Charge_CustomerChargeDetail, alias=Optional.of(d)), Table(name=charge_customerchargecalctask, alias=Optional.of(t))], schema=Optional.absent())), projectionsContext=ProjectionsContext(startIndex=7, stopIndex=14, distinctRow=false, projections=[AggregationProjection(type=COUNT, innerExpression=(0), alias=Optional.absent(), derivedAggregationProjections=[], index=-1)], columnLabels=[COUNT(0)]), groupByContext=org.apache.shardingsphere.sql.parser.relation.segment.select.groupby.GroupByContext@19a65f26, orderByContext=org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.OrderByContext@1b9323b3, paginationContext=org.apache.shardingsphere.sql.parser.relation.segment.select.pagination.PaginationContext@2f2e2eee, containsSubquery=false)
【newsee_charge】2020-05-22 11:54:00.767 [http-nio-8778-exec-7] INFO ShardingSphere-SQL - Actual SQL: charge11master ::: SELECT count(0) FROM Charge_CustomerChargeDetail d LEFT JOIN charge_customerchargecalctask t ON d.TaskId = t.Id WHERE d.EnterpriseId = ? AND d.HouseId = ? AND d.isDelete = ? ::: [974, 414257, 0]
【newsee_charge】2020-05-22 11:54:00.770 [http-nio-8778-exec-7] DEBUG c.n.c.d.C.listPageStandrd_COUNT - <== Total: 1
【newsee_charge】2020-05-22 11:54:00.771 [http-nio-8778-exec-7] DEBUG c.n.c.d.C.listPageStandrd - ==> Preparing: select d.Id,d.EnterpriseId,d.OrganizationId,d.ContractId,d.DiscountDate,d.Sequence,CONCAT(DATE_FORMAT(d.CalcStartDate,"%Y%m%d"),"~",DATE_FORMAT(d.CalcEndDate,"%Y%m%d")) as chargeCycle,d.TaskId,d.PreinctId,d.PreinctName,d.HouseId,d.HouseName,d.OwnerId,d.OwnerName,d.PaidOwnerType,d.ChargeItemId,d.ChargeItemName,d.ChargeId,d.ChargeName,d.Price,d.ChargeItemPrice,d.Amount,d.CalcStartDate,d.CalcEndDate,d.ShouldChargeDate,d.ActualChargeSum,d.ChargeSum,d.PaidChargeSum,d.Arrears,d.Arrears as Arrear,d.Discount,d.DiscountType,d.DiscountUserId,d.DiscountUserName,d.DelaySum,d.DelayDiscount,d.DelayDays,d.DiscountReason,d.Tax,(d.ChargeSum-d.Tax) as NotTaxAmount,d.IsCheck,d.NotCheckReason,d.IsPaid,d.PaidDate,d.IsClosing,d.IsBadDebt,d.BadDebtDate,d.ChargeSource,d.CreateUserId,d.CreateUserName,d.CreateTime,d.UpdateUserId,d.UpdateUserName,d.UpdateTime,d.SysTime,d.Description,d.PayStatus,d.remark,t.TaskName as refTaskName,d.IsDelete,d.powerType,(d.taxRate*100) as taxRate ,d.accountFromType,d.billChangeType,d.initOwnerId,d.isKongGuan,d.isKongZhi,d.AccountBook,d.fromSysAccountId,d.RefChargeDetailId from Charge_CustomerChargeDetail d LEFT JOIN charge_customerchargecalctask t ON d.TaskId = t.Id WHERE d.EnterpriseId = ? and d.HouseId = ? and d.isDelete = ? order by d.IsCheck desc,d.HouseName,d.CalcStartDate LIMIT ?
【newsee_charge】2020-05-22 11:54:00.771 [http-nio-8778-exec-7] DEBUG c.n.c.d.C.listPageStandrd - ==> Parameters: 974(Long), 414257(Long), 0(String), 50(Integer)
【newsee_charge】2020-05-22 11:54:00.771 [http-nio-8778-exec-7] DEBUG c.n.database.sharding.NSDbHintShardingAlgorithm - Matched Datasource[charge11] by DataSourceContextHolder.
【newsee_charge】2020-05-22 11:54:00.771 [http-nio-8778-exec-7] INFO ShardingSphere-SQL - Rule Type: sharding
【newsee_charge】2020-05-22 11:54:00.771 [http-nio-8778-exec-7] INFO ShardingSphere-SQL - Logic SQL: select
d.Id,d.EnterpriseId,d.OrganizationId,d.ContractId,d.DiscountDate,d.Sequence,CONCAT(DATE_FORMAT(d.CalcStartDate,"%Y%m%d"),"~",DATE_FORMAT(d.CalcEndDate,"%Y%m%d"))
as
chargeCycle,d.TaskId,d.PreinctId,d.PreinctName,d.HouseId,d.HouseName,d.OwnerId,d.OwnerName,d.PaidOwnerType,d.ChargeItemId,d.ChargeItemName,d.ChargeId,d.ChargeName,d.Price,d.ChargeItemPrice,d.Amount,d.CalcStartDate,d.CalcEndDate,d.ShouldChargeDate,d.ActualChargeSum,d.ChargeSum,d.PaidChargeSum,d.Arrears,d.Arrears
as
Arrear,d.Discount,d.DiscountType,d.DiscountUserId,d.DiscountUserName,d.DelaySum,d.DelayDiscount,d.DelayDays,d.DiscountReason,d.Tax,(d.ChargeSum-d.Tax) as NotTaxAmount,d.IsCheck,d.NotCheckReason,d.IsPaid,d.PaidDate,d.IsClosing,d.IsBadDebt,d.BadDebtDate,d.ChargeSource,d.CreateUserId,d.CreateUserName,d.CreateTime,d.UpdateUserId,d.UpdateUserName,d.UpdateTime,d.SysTime,d.Description,d.PayStatus,d.remark,t.TaskName
as refTaskName,d.IsDelete,d.powerType,(d.taxRate*100) as taxRate
,d.accountFromType,d.billChangeType,d.initOwnerId,d.isKongGuan,d.isKongZhi,d.AccountBook,d.fromSysAccountId,d.RefChargeDetailId
from
Charge_CustomerChargeDetail d
LEFT JOIN charge_customerchargecalctask t ON d.TaskId = t.Id
WHERE d.EnterpriseId = ?


and d.HouseId = ?






















and d.isDelete = ?
order by d.IsCheck desc,d.HouseName,d.CalcStartDate LIMIT ?
【newsee_charge】2020-05-22 11:54:00.771 [http-nio-8778-exec-7] INFO ShardingSphere-SQL - SQLStatement: SelectSQLStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@fad41d3, tablesContext=TablesContext(tables=[Table(name=Charge_CustomerChargeDetail, alias=Optional.of(d)), Table(name=charge_customerchargecalctask, alias=Optional.of(t))], schema=Optional.absent())), projectionsContext=ProjectionsContext(startIndex=15, stopIndex=1148, distinctRow=false, projections=[ColumnProjection(owner=d, name=Id, alias=Optional.absent()), ColumnProjection(owner=d, name=EnterpriseId, alias=Optional.absent()), ColumnProjection(owner=d, name=OrganizationId, alias=Optional.absent()), ColumnProjection(owner=d, name=ContractId, alias=Optional.absent()), ColumnProjection(owner=d, name=DiscountDate, alias=Optional.absent()), ColumnProjection(owner=d, name=Sequence, alias=Optional.absent()), ExpressionProjection(expression=CONCAT(DATE_FORMAT(d.CalcStartDate,"%Y%m%d"),"~",DATE_FORMAT(d.CalcEndDate,"%Y%m%d")), alias=Optional.of(chargeCycle)), ColumnProjection(owner=d, name=TaskId, alias=Optional.absent()), ColumnProjection(owner=d, name=PreinctId, alias=Optional.absent()), ColumnProjection(owner=d, name=PreinctName, alias=Optional.absent()), ColumnProjection(owner=d, name=HouseId, alias=Optional.absent()), ColumnProjection(owner=d, name=HouseName, alias=Optional.absent()), ColumnProjection(owner=d, name=OwnerId, alias=Optional.absent()), ColumnProjection(owner=d, name=OwnerName, alias=Optional.absent()), ColumnProjection(owner=d, name=PaidOwnerType, alias=Optional.absent()), ColumnProjection(owner=d, name=ChargeItemId, alias=Optional.absent()), ColumnProjection(owner=d, name=ChargeItemName, alias=Optional.absent()), ColumnProjection(owner=d, name=ChargeId, alias=Optional.absent()), ColumnProjection(owner=d, name=ChargeName, alias=Optional.absent()), ColumnProjection(owner=d, name=Price, alias=Optional.absent()), ColumnProjection(owner=d, name=ChargeItemPrice, alias=Optional.absent()), ColumnProjection(owner=d, name=Amount, alias=Optional.absent()), ColumnProjection(owner=d, name=CalcStartDate, alias=Optional.absent()), ColumnProjection(owner=d, name=CalcEndDate, alias=Optional.absent()), ColumnProjection(owner=d, name=ShouldChargeDate, alias=Optional.absent()), ColumnProjection(owner=d, name=ActualChargeSum, alias=Optional.absent()), ColumnProjection(owner=d, name=ChargeSum, alias=Optional.absent()), ColumnProjection(owner=d, name=PaidChargeSum, alias=Optional.absent()), ColumnProjection(owner=d, name=Arrears, alias=Optional.absent()), ColumnProjection(owner=d, name=Arrears, alias=Optional.of(Arrear)), ColumnProjection(owner=d, name=Discount, alias=Optional.absent()), ColumnProjection(owner=d, name=DiscountType, alias=Optional.absent()), ColumnProjection(owner=d, name=DiscountUserId, alias=Optional.absent()), ColumnProjection(owner=d, name=DiscountUserName, alias=Optional.absent()), ColumnProjection(owner=d, name=DelaySum, alias=Optional.absent()), ColumnProjection(owner=d, name=DelayDiscount, alias=Optional.absent()), ColumnProjection(owner=d, name=DelayDays, alias=Optional.absent()), ColumnProjection(owner=d, name=DiscountReason, alias=Optional.absent()), ColumnProjection(owner=d, name=Tax, alias=Optional.absent()), ExpressionProjection(expression=d.ChargeSum-d.Tax)asNotTaxAmoun, alias=Optional.of(NotTaxAmount)), ColumnProjection(owner=d, name=IsCheck, alias=Optional.absent()), ColumnProjection(owner=d, name=NotCheckReason, alias=Optional.absent()), ColumnProjection(owner=d, name=IsPaid, alias=Optional.absent()), ColumnProjection(owner=d, name=PaidDate, alias=Optional.absent()), ColumnProjection(owner=d, name=IsClosing, alias=Optional.absent()), ColumnProjection(owner=d, name=IsBadDebt, alias=Optional.absent()), ColumnProjection(owner=d, name=BadDebtDate, alias=Optional.absent()), ColumnProjection(owner=d, name=ChargeSource, alias=Optional.absent()), ColumnProjection(owner=d, name=CreateUserId, alias=Optional.absent()), ColumnProjection(owner=d, name=CreateUserName, alias=Optional.absent()), ColumnProjection(owner=d, name=CreateTime, alias=Optional.absent()), ColumnProjection(owner=d, name=UpdateUserId, alias=Optional.absent()), ColumnProjection(owner=d, name=UpdateUserName, alias=Optional.absent()), ColumnProjection(owner=d, name=UpdateTime, alias=Optional.absent()), ColumnProjection(owner=d, name=SysTime, alias=Optional.absent()), ColumnProjection(owner=d, name=Description, alias=Optional.absent()), ColumnProjection(owner=d, name=PayStatus, alias=Optional.absent()), ColumnProjection(owner=d, name=remark, alias=Optional.absent()), ColumnProjection(owner=t, name=TaskName, alias=Optional.of(refTaskName)), ColumnProjection(owner=d, name=IsDelete, alias=Optional.absent()), ColumnProjection(owner=d, name=powerType, alias=Optional.absent()), ExpressionProjection(expression=d.taxRate*100)astaxRat, alias=Optional.of(taxRate)), ColumnProjection(owner=d, name=accountFromType, alias=Optional.absent()), ColumnProjection(owner=d, name=billChangeType, alias=Optional.absent()), ColumnProjection(owner=d, name=initOwnerId, alias=Optional.absent()), ColumnProjection(owner=d, name=isKongGuan, alias=Optional.absent()), ColumnProjection(owner=d, name=isKongZhi, alias=Optional.absent()), ColumnProjection(owner=d, name=AccountBook, alias=Optional.absent()), ColumnProjection(owner=d, name=fromSysAccountId, alias=Optional.absent()), ColumnProjection(owner=d, name=RefChargeDetailId, alias=Optional.absent())], columnLabels=[Id, EnterpriseId, OrganizationId, ContractId, DiscountDate, Sequence, chargeCycle, TaskId, PreinctId, PreinctName, HouseId, HouseName, OwnerId, OwnerName, PaidOwnerType, ChargeItemId, ChargeItemName, ChargeId, ChargeName, Price, ChargeItemPrice, Amount, CalcStartDate, CalcEndDate, ShouldChargeDate, ActualChargeSum, ChargeSum, PaidChargeSum, Arrears, Arrear, Discount, DiscountType, DiscountUserId, DiscountUserName, DelaySum, DelayDiscount, DelayDays, DiscountReason, Tax, NotTaxAmount, IsCheck, NotCheckReason, IsPaid, PaidDate, IsClosing, IsBadDebt, BadDebtDate, ChargeSource, CreateUserId, CreateUserName, CreateTime, UpdateUserId, UpdateUserName, UpdateTime, SysTime, Description, PayStatus, remark, refTaskName, IsDelete, powerType, taxRate, accountFromType, billChangeType, initOwnerId, isKongGuan, isKongZhi, AccountBook, fromSysAccountId, RefChargeDetailId]), groupByContext=org.apache.shardingsphere.sql.parser.relation.segment.select.groupby.GroupByContext@1a652284, orderByContext=org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.OrderByContext@44622678, paginationContext=org.apache.shardingsphere.sql.parser.relation.segment.select.pagination.PaginationContext@1e305c25, containsSubquery=false)
【newsee_charge】2020-05-22 11:54:00.771 [http-nio-8778-exec-7] INFO ShardingSphere-SQL - Actual SQL: charge11master ::: select
d.Id,d.EnterpriseId,d.OrganizationId,d.ContractId,d.DiscountDate,d.Sequence,CONCAT(DATE_FORMAT(d.CalcStartDate,"%Y%m%d"),"~",DATE_FORMAT(d.CalcEndDate,"%Y%m%d"))
as
chargeCycle,d.TaskId,d.PreinctId,d.PreinctName,d.HouseId,d.HouseName,d.OwnerId,d.OwnerName,d.PaidOwnerType,d.ChargeItemId,d.ChargeItemName,d.ChargeId,d.ChargeName,d.Price,d.ChargeItemPrice,d.Amount,d.CalcStartDate,d.CalcEndDate,d.ShouldChargeDate,d.ActualChargeSum,d.ChargeSum,d.PaidChargeSum,d.Arrears,d.Arrears
as
Arrear,d.Discount,d.DiscountType,d.DiscountUserId,d.DiscountUserName,d.DelaySum,d.DelayDiscount,d.DelayDays,d.DiscountReason,d.Tax,(d.ChargeSum-d.Tax) as NotTaxAmount,d.IsCheck,d.NotCheckReason,d.IsPaid,d.PaidDate,d.IsClosing,d.IsBadDebt,d.BadDebtDate,d.ChargeSource,d.CreateUserId,d.CreateUserName,d.CreateTime,d.UpdateUserId,d.UpdateUserName,d.UpdateTime,d.SysTime,d.Description,d.PayStatus,d.remark,t.TaskName
as refTaskName,d.IsDelete,d.powerType,(d.taxRate*100) as taxRate
,d.accountFromType,d.billChangeType,d.initOwnerId,d.isKongGuan,d.isKongZhi,d.AccountBook,d.fromSysAccountId,d.RefChargeDetailId
from
Charge_CustomerChargeDetail d
LEFT JOIN charge_customerchargecalctask t ON d.TaskId = t.Id
WHERE d.EnterpriseId = ?


and d.HouseId = ?






















and d.isDelete = ?
order by d.IsCheck desc,d.HouseName,d.CalcStartDate LIMIT ? ::: [974, 414257, 0, 50]
【newsee_charge】2020-05-22 11:54:00.784 [http-nio-8778-exec-7] DEBUG c.n.c.d.C.listPageStandrd - <== Total: 1
