package redshift;


import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.ESqlStatementType;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.stmt.redshift.TRedshiftDropGroup;
import junit.framework.TestCase;


public class testDropGroup extends TestCase {

    public void test1() {
        TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvredshift);
        sqlparser.sqltext = "drop group guests;";
        assertTrue(sqlparser.parse() == 0);

        assertTrue(sqlparser.sqlstatements.get(0).sqlstatementtype == ESqlStatementType.sstredshiftDropGroup);
        TRedshiftDropGroup dropGroup = (TRedshiftDropGroup) sqlparser.sqlstatements.get(0);
        assertTrue(dropGroup.getGroupName().toString().equalsIgnoreCase("guests"));


    }
}