package testSQLFiles;

import gudusoft.gsqlparser.EDbVendor;
import junit.framework.TestCase;

import static testSQLFiles.parseTest.parsefiles;

public class testSQLServerFiles extends TestCase {
    public  void testSQLServer(){
        parsefiles(EDbVendor.dbvmssql,common.gspCommon.BASE_SQL_DIR+"mssql");
        parsefiles(EDbVendor.dbvmssql,common.gspCommon.BASE_SQL_DIR+"java/mssql");
    }
}
