package testSQLFiles;

import gudusoft.gsqlparser.EDbVendor;
import junit.framework.TestCase;

import static testSQLFiles.parseTest.parsefiles;

public class testSnowflakeFiles extends TestCase {
    public  void testSnowflake(){
        parseTest.parsefiles(EDbVendor.dbvsnowflake,common.gspCommon.BASE_SQL_DIR+"snowflake");
        parsefiles(EDbVendor.dbvsnowflake,common.gspCommon.BASE_SQL_DIR+"java/snowflake/");
    }
}
