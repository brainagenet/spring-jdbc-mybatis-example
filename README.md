# spring-jdbc-mybatis-example

Database 연동을 하는 Java의 표준 Sepc은 JDBC입니다. 대부분의 Project에서는 Database 연동을 위한 다양한 Framework를 사용하고 있습니다. 대표적인 것인 Sql Mapper인 MyBatis와 ORM인 JPA, Hibernate, EclipseLink, Oracle TopLink 등이 있습니다.

여기서는 MyBatis를 사용하여 대용량 질의 결과를 다른 테이블에 Insert하거나 Update 하는 방법을 살펴보려고 합니다.

## Batch Update란?
 
Batch Update란 대량의 데이터에 대한 Insert/Update/Delete에 대한 반영을 건-By-건으로 하는 것이 아니라 Database의 영역에 저장해 놓았다가 한 번에 Database에 반영하는 방법입니다. 일반적인 건-by-건으로 처리는 것에 비해 많은 속도 향상을 기대할 수 있습니다.

## implement by JDBC API

아래의 코드는 JDBC API를 사용하여 Batch Update를 수행하는 것입니다.
 
 
 ```java
         Connection conn = null;
         PreparedStatement ps = null;
         PreparedStatement ups = null;
         ResultSet rs = null;
 
         try {
             conn = dataSource.getConnection();
             conn.setAutoCommit(false);
 
             ps = conn.prepareStatement("SELECT BOOK_ID FROM TEST_BOOK");
             rs = ps.executeQuery();
 
             ups = conn.prepareStatement("UPDATE TEST_BOOK SET ORIGIN_PRICE = 0 WHERE BOOK_ID = ?");
             int updateCount = 0;
             while (rs.next()) {
                 ups.clearParameters();
                 ups.setString(1, rs.getString("BOOK_ID"));
                 ups.addBatch();
                 updateCount++;
 
                 if (updateCount == 100) {
                     ups.executeBatch();
                     updateCount = 0;
                 }
             }
             ups.executeBatch();
 
             conn.commit();
         } catch (SQLException ex) {
             conn.rollback();
             System.err.println(ex);
         } finally {
             if (rs != null) {
                 rs.close();
                 rs = null;
             }
 
             if (ups != null) {
                 ups.close();
                 ups = null;
             }
 
             if (ps != null) {
                 ps.close();
                 ps = null;
             }
 
             if (conn != null) {
                 conn.close();
                 conn = null;
             }
         }
 ```
 
위의 코드는 100건 단위로 Batch Update를 수행하고 있습니다. 100,000건이라면 100000/100 = 1000 번의 Batch Update가 수행된다고 생각하면 됩니다.
 
## implement by MyBatis
 
그럼 이것을 MyBatis에서는 어떻게 구현을 할까요? MyBatis에서도 Batch Update 기능을 지원하고 있습니다만 보다 좋은 성능을 위해 여기에서는 Dynamic SQL 지원을 위한 <forEach />를 사용한 구현 방법을 알아보도록 하겠습니다.

> 추후 MyBatis 자체의 Batch Update 방법과 Spring JDBC에서 지원하는 부분도 업데이트 할 예정입니다.

### insert 문에 forEach 적용하기

각 Database 별로 다르지만 보통은 아래와 같은 질의문을 사용할 것입니다.

```sql
INSERt INTO TABLE_NAME
VALUES (#{val1}, #{val1}, #{val1}, #{val1}), 
       (#{val1}, #{val1}, #{val1}, #{val1});
```

이것이 일반적으로 사용되는 대량 INSERT를 위한 질의문입니다.

그런데 Oracle은 위와 같은 질의문은 지원하지 않는다고 에러가 발생합니다. Oracle은 아래와 같이 질의문을 사용합니다.

```sql
INSERT ALL 
  INTO TABLE_NAME VALUES (#{val1}, #{val1}, #{val1}, #{val1})
  INTO TABLE_NAME VALUES (#{val1}, #{val1}, #{val1}, #{val1})
SELECT * FROM DUAL
```

잘 보면 INSERT 문의 INTO 절을 모두 사용하고 있습니다. 이렇다면 아마도 동시에 다른 테이블에 넣는 것도 가능하지 않을까 생각이 듭니다.


```sql
INSERT ALL 
  INTO TABLE_NAME1 VALUES (#{val1}, #{val2}, #{val3}, #{val4})
  INTO TABLE_NAME2 VALUES (#{val1}, #{val3}, #{val4}, #{val6}, #{val2})
SELECT * FROM DUAL
```

대용량 질의 후, 여러 테이블에 데이타를 나누어서 Insert를 해야할 경우 성능 향상을 위해 사용가능하다고 생각이 됩니다.

### MyBatis의 SQL XML에 적용하기

위의 내용을 SQL XML에 적용하여 보겠습니다. 

```xml
<?xml version="1.0" encoding="UTF-8"?>

<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="net.brainage.example.mapper.TestBookBackupMapper">

    <insert id="insert" parameterType="net.brainage.example.model.TestBook">
        INSERT INTO TEST_BOOK_BACKUP (BACKUP_ID, BOOK_ID, BOOK_NAME, ORIGIN_PRICE)
        VALUES (TEST_BOOK_BACKUP_SEQ.NEXTVAL, #{id}, #{name}, #{originPrice})
    </insert>

    <insert id="insertBatch" parameterType="list">
        INSERT ALL
        <foreach collection="list" item="book">
            INTO TEST_BOOK_BACKUP (BACKUP_ID, BOOK_ID, BOOK_NAME, ORIGIN_PRICE)
                VALUES ( TEST_BOOK_BACKUP_SEQ.NEXTVAL, #{book.id}, #{book.name}, #{book.originPrice} )
        </foreach>
        SELECT * FROM DUAL
    </insert>
    
    <insert id="insertBatch4MultiTable" parameterType="list">
        INSERT ALL
        <foreach collection="list" item="book">
            INTO TABLE_NAME1 VALUES (#{val1}, #{val2}, #{val3}, #{val4})
            INTO TABLE_NAME2 VALUES (#{val1}, #{val3}, #{val4}, #{val6}, #{val2})
        </foreach>
        SELECT * FROM DUAL
    </insert>

</mapper>
```

### MyBatis Mapper Interface 정의하기

위와 같이 SQL을 정의하였습니다. namespace에 있는 Mapper Interface 정의를 살표보겠습니다.

```java
package net.brainage.example.mapper;

import net.brainage.example.model.TestBook;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TestBookBackupMapper {

    int insert(TestBook paramObject);

    int insertBatch(@Param("list") List<TestBook> list);
    
    int insertBatch4MultiTable(@Param("list") List<TestBook> list);

}
```

Mapper Interface에 있는 Method Name과 SQL XML의 id와 Mapping 이 되어야 한다는 것은 다 아실 것입니다.

### Unit Test 하기

이제 정상적으로 구동이 되는지 확인하도록 하겠습니다.

> Test Code는 src/test/net/brainage/example/mapper/ITestBookMapperTest.java를 보시면 됩니다.

```java
    @Test
    public void test_for_backup_book() {

        // Programatic Transaction 처리 코드를 추후 PMD로 체크하기 위해 Wrapping 하였습니다.
        final TransactionImpl transaction = new TransactionImpl(transactionManager);

        try {
            // Transaction을 시작합니다.
            transaction.start();
            
            // 100 건 단위로 처리하기 위한 저장소를 생성합니다.
            final List<TestBook> books = new ArrayList<TestBook>();
            
            // 대용량 질의를 수행합니다. 질의 결과를 직접 처리하기 위해 MyBatis ResultHandler를 사용합니다.
            sqlMapMapper.select("net.brainage.example.mapper.TestBookMapper.getAll", new ResultHandler() {
                @Override
                public void handleResult(ResultContext resultContext) {
                    // 조회 결과 객체를 얻는다.
                    TestBook book = (TestBook) resultContext.getResultObject();
                    
                    // 조회 결과 객체를 100건 단위 처리를 위한 저장소에 담는다.
                    books.add(book);

                    // 저장소의 크기가 100인지 확인
                    if (books.size() == 100) {
                        // 저장소의 크기가 100이면 Insert를 수행합니다.
                        testBookBackupMapper.insertBatch(books);
                        
                        // 저장소를 비웁니다.
                        books.clear();
                    }
                }
            });
            
            // 100건 단위이므로 마지막에 100건이 되지 않고 위의 처리가 끝날 수도 있습니다.
            // 따라서 저장소의 크기가 0보다 크면 나머지에 대해 처리를 해 줍니다.
            if (books.size() > 0 ) {
                testBookBackupMapper.insertBatch(books);
                books.clear();
            }
            
            // Transaction을 commit 합니다.
            transaction.commit();
        } finally {
            // Transaction Status를 확인하여 complete가 되지 않았을 경우 자동으로 rollback이 실행됩니다.
            // Transaction을 종료합니다.
            transaction.end();
        }
    }
```



