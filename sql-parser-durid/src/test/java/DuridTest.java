import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @ClassName DuridTest
 * @Description
 * @Author wzj
 * @Date 2021/8/3 11:10
 **/
@RunWith(SpringRunner.class)
public class DuridTest {

    @Test
    public void testSqlParser() {
        String sql = "select name from user where id=1 and sex='女' and age>14";
        String dbType = JdbcConstants.MYSQL;
        List<SQLStatement> statementList = SQLUtils.parseStatements(sql, dbType);
        System.out.println(statementList);
    }

    @Test
    public void testUpdate(){
        String sql = "update user set sex='女' where id=1 and age>14";
        String dbType = JdbcConstants.MYSQL;
        List<SQLStatement> statementList = SQLUtils.parseStatements(sql, dbType);
        System.out.println(statementList);
    }
}
