package testSQLFiles;

import gudusoft.gsqlparser.EDbVendor;
import junit.framework.TestCase;

import static testSQLFiles.parseTest.parsefiles;

public class testHanaFiles extends TestCase {
    public  void testHana(){
        parsefiles(EDbVendor.dbvhana,common.gspCommon.BASE_SQL_DIR+"hana");
        parsefiles(EDbVendor.dbvhana,common.gspCommon.BASE_SQL_DIR+"java/hana/");
    }

}
