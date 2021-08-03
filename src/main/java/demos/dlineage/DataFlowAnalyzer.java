
package demos.dlineage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.TCustomSqlStatement;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.TSourceToken;
import gudusoft.gsqlparser.dlineage.dataflow.listener.DataFlowHandleAdapter;
import gudusoft.gsqlparser.dlineage.dataflow.model.ErrorInfo;
import gudusoft.gsqlparser.dlineage.dataflow.model.json.DataFlow;
import gudusoft.gsqlparser.dlineage.dataflow.model.xml.dataflow;
import gudusoft.gsqlparser.dlineage.util.RemoveDataflowFunction;
import gudusoft.gsqlparser.dlineage.util.XML2Model;
import gudusoft.gsqlparser.sqlenv.TGreenplumSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TMssqlSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TMysqlSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TNetezzaSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TOracleSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TPostgreSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TRedshiftSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TSQLEnv;
import gudusoft.gsqlparser.sqlenv.TSnowflakeSQLDataSource;
import gudusoft.gsqlparser.sqlenv.TTeradataSQLDataSource;
import gudusoft.gsqlparser.util.SQLUtil;
import gudusoft.gsqlparser.util.json.JSON;

public class DataFlowAnalyzer {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println(
					"Usage: java DataFlowAnalyzer [/f <path_to_sql_file>] [/d <path_to_directory_includes_sql_files>] [/stat] [/s] [/i] [/ic] [/lof] [/j] [/text] [/json] [/traceView] [/t <database type>] [/o <output file path>] [/version] [/h <host> /P <port> /u <username> /p <password> /db <database> [/metadata]]");
			System.out.println("/f: Option, specify the sql file path to analyze fdd relation.");
			System.out.println("/d: Option, specify the sql directory path to analyze fdd relation.");
			System.out.println("/j: Option, analyze the join relation.");
			System.out.println("/s: Option, simple output, ignore the intermediate results.");
			System.out.println("/i: Option, ignore all result sets.");
			System.out.println("/if: Option, ignore functions.");
			System.out.println("/ic: Option, ignore output coordinates.");
			System.out.println("/lof: Option, link orphan column to first table.");
			System.out.println("/traceView: Option, analyze the source tables of views.");
			System.out.println("/text: Option, print the plain text format output.");
			System.out.println("/json: Option, print the json format output.");
			System.out.println("/stat: Option, output the analysis statistic information.");
			System.out.println(
					"/t: Option, set the database type. Support sparksql,oracle, mysql, mssql, db2, netezza, teradata, informix, sybase, postgresql, hive, greenplum and redshift, the default type is oracle");
			System.out.println("/o: Option, write the output stream to the specified file.");
			System.out.println("/log: Option, generate a dataflow.log file to log information.");
			System.out.println("/h: Option, specify the host of jdbc connection");
			System.out.println("/P: Option, specify the port of jdbc connection, note it's capital P.");
			System.out.println("/u: Option, specify the username of jdbc connection.");
			System.out.println("/p: Option, specify the password of jdbc connection, note it's lowercase P.");
			System.out.println("/db: Option, specify the database of jdbc connection.");
			System.out.println("/schema: Option, specify the schema which is used for extracting metadata.");
			System.out.println("/metadata: Option, output the database metadata information to the file metadata.json.");
			return;
		}

		File sqlFiles = null;

		List<String> argList = Arrays.asList(args);

		if (argList.indexOf("/version") != -1) {
			System.out.println("Version: " + gudusoft.gsqlparser.dlineage.DataFlowAnalyzer.getVersion());
			System.out.println("Release Date: " + gudusoft.gsqlparser.dlineage.DataFlowAnalyzer.getReleaseDate());
			return;
		}

		if (argList.indexOf("/f") != -1 && argList.size() > argList.indexOf("/f") + 1) {
			sqlFiles = new File(args[argList.indexOf("/f") + 1]);
			if (!sqlFiles.exists() || !sqlFiles.isFile()) {
				System.out.println(sqlFiles + " is not a valid file.");
				return;
			}
		} else if (argList.indexOf("/d") != -1 && argList.size() > argList.indexOf("/d") + 1) {
			sqlFiles = new File(args[argList.indexOf("/d") + 1]);
			if (!sqlFiles.exists() || !sqlFiles.isDirectory()) {
				System.out.println(sqlFiles + " is not a valid directory.");
				return;
			}
		} else {
			System.out.println("Please specify a sql file path or directory path to analyze dlineage.");
			return;
		}

		EDbVendor vendor = EDbVendor.dbvoracle;

		int index = argList.indexOf("/t");

		if (index != -1 && args.length > index + 1) {
			vendor = TGSqlParser.getDBVendorByName(args[index + 1]);
		}

		String outputFile = null;

		index = argList.indexOf("/o");

		if (index != -1 && args.length > index + 1) {
			outputFile = args[index + 1];
		}

		boolean simple = argList.indexOf("/s") != -1;
		boolean ignoreResultSets = argList.indexOf("/i") != -1;
		boolean showJoin = argList.indexOf("/j") != -1;
		boolean textFormat = false;
		boolean jsonFormat = false;
		boolean linkOrphanColumnToFirstTable = argList.indexOf("/lof") != -1;
		boolean ignoreCoordinate = argList.indexOf("/ic") != -1;

		if (simple) {
			textFormat = argList.indexOf("/text") != -1;
		}

		boolean traceView = argList.indexOf("/traceView") != -1;
		if (traceView) {
			simple = true;
		}

		jsonFormat = argList.indexOf("/json") != -1;

		boolean stat = argList.indexOf("/stat") != -1;
		
		boolean ignoreFunction = argList.indexOf("/if") != -1;

		TSQLEnv sqlenv = null;

		// Get database metadata from sql jdbc
		if (argList.indexOf("/h") != -1 && argList.indexOf("/P") != -1 && argList.indexOf("/u") != -1
				&& argList.indexOf("/p") != -1) {
			try {
				String host = args[argList.indexOf("/h") + 1];
				String port = args[argList.indexOf("/P") + 1];
				String user = args[argList.indexOf("/u") + 1];
				String passowrd = args[argList.indexOf("/p") + 1];
				String database = null;
				String schema = null;
				if (argList.indexOf("/db") != -1) {
					database = args[argList.indexOf("/db") + 1];
				}
				if (argList.indexOf("/schema") != -1) {
					schema = args[argList.indexOf("/schema") + 1];
				}
				TSQLDataSource datasource = createSQLDataSource(vendor, host, port, user, passowrd, database, schema);
				if (datasource != null) {
					if (argList.indexOf("/metadata") != -1) {
						try {
							String metadata = datasource.exportJSON();
							SQLUtil.writeToFile(new File("./metadata.json"), metadata);
						} catch (Exception e) {
							System.err.println("Get datasource metadata failed. " + e.getMessage());
							e.printStackTrace();
						}
					}
					sqlenv = TSQLEnv.valueOf(datasource);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (!stat) {
			FileOutputStream writer = null;
			if (outputFile != null) {
				try {
					writer = new FileOutputStream(outputFile);
					System.setOut(new PrintStream(writer));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

			gudusoft.gsqlparser.dlineage.DataFlowAnalyzer dlineage = new gudusoft.gsqlparser.dlineage.DataFlowAnalyzer(
					sqlFiles, vendor, simple);

			if (sqlenv != null) {
				dlineage.setSqlEnv(sqlenv);
			}

			dlineage.setShowJoin(showJoin);
			dlineage.setIgnoreRecordSet(ignoreResultSets);
			dlineage.setLinkOrphanColumnToFirstTable(linkOrphanColumnToFirstTable);
			dlineage.setIgnoreCoordinate(ignoreCoordinate);

			if (simple && !jsonFormat) {
				dlineage.setTextFormat(textFormat);
			}

			String result = dlineage.generateDataFlow();
			
			if (jsonFormat) {
				dataflow dataflow = dlineage.getDataFlow();
				if(ignoreFunction){
					dataflow = new RemoveDataflowFunction().removeFunction(dataflow);
				}
				DataFlow model = gudusoft.gsqlparser.dlineage.DataFlowAnalyzer
						.getSqlflowJSONModel(dataflow);
				model.setDbvendor(vendor.name());
				result = JSON.toJSONString(model);
			} else if (traceView) {
				result = dlineage.traceView();
			} else if(ignoreFunction && result.trim().startsWith("<?xml")){
				dataflow dataflow = dlineage.getDataFlow();
				dataflow = new RemoveDataflowFunction().removeFunction(dataflow);
				result =  XML2Model.saveXML(dataflow);
			}

			if (result != null) {
				System.out.println(result);

				if (writer != null && result.length() < 1024 * 1024) {
					System.err.println(result);
				}
			}

			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			boolean log = argList.indexOf("/log") != -1;
			PrintStream pw = null;
			ByteArrayOutputStream sw = null;
			PrintStream systemSteam = System.err;

			try {
				sw = new ByteArrayOutputStream();
				pw = new PrintStream(sw);
				System.setErr(pw);
			} catch (Exception e) {
				e.printStackTrace();
			}

			List<ErrorInfo> errors = dlineage.getErrorMessages();
			if (!errors.isEmpty()) {
				System.err.println("Error log:\n");
				for (int i = 0; i < errors.size(); i++) {
					System.err.println(errors.get(i).getErrorMessage());
				}
			}

			if (sw != null) {
				String errorMessage = sw.toString().trim();
				if (errorMessage.length() > 0) {
					if (log) {
						try {
							pw = new PrintStream(new File(".", "dataflow.log"));
							pw.print(errorMessage);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}

					System.setErr(systemSteam);
					System.err.println(errorMessage);
				}
			}
		} else {
			final File[] files = SQLUtil.listFiles(sqlFiles, new FileFilter() {
				public boolean accept(File file) {
					return file.isDirectory() || file.getName().toLowerCase().endsWith(".sql");
				}
			});

			final int[] totalLines = new int[1];
			final int[] totalStatementCount = new int[1];
			final int[] successStatementCount = new int[1];
			final long[] analyzeTotalTime = new long[1];

			if (files.length > 0) {
				if (files[0].length() < 10240) {
					final gudusoft.gsqlparser.dlineage.DataFlowAnalyzer warmUp = new gudusoft.gsqlparser.dlineage.DataFlowAnalyzer(
							files[0], vendor, simple);
					warmUp.generateDataFlow();
				} else {
					final gudusoft.gsqlparser.dlineage.DataFlowAnalyzer warmUp = new gudusoft.gsqlparser.dlineage.DataFlowAnalyzer(
							"select a from b", vendor, simple);
					warmUp.generateDataFlow();
				}
			}

			int ignoreFileCount = 0;
			for (int i = 0; i < files.length; i++) {
				final File file = files[i];
				if (file.length() > 10000) {
					ignoreFileCount++;
					continue;
				}
				try {
					final gudusoft.gsqlparser.dlineage.DataFlowAnalyzer dlineage = new gudusoft.gsqlparser.dlineage.DataFlowAnalyzer(
							file, vendor, simple);
					if (sqlenv != null) {
						dlineage.setSqlEnv(sqlenv);
					}
					dlineage.setLinkOrphanColumnToFirstTable(false);
					dlineage.setHandleListener(new DataFlowHandleAdapter() {
						long analyzeStartTime;
						int statmentCount = 0;

						public void startAnalyze(File file, long fileCountOrSqlLength, boolean isFileCount) {
							analyzeStartTime = System.currentTimeMillis();
						}

						@Override
						public void endAnalyzeStatment(TCustomSqlStatement stmt) {
							statmentCount++;
						}

						@Override
						public void endAnalyzeDataFlow(TGSqlParser sqlparser) {
							successStatementCount[0] += statmentCount;
							totalStatementCount[0] += sqlparser.getSqlstatements().size();
							TSourceToken endToken = sqlparser.sourcetokenlist.get(sqlparser.sourcetokenlist.size() - 1);
							totalLines[0] += (endToken.lineNo + endToken.astext.split("\n").length - 1);
						}

						@Override
						public void endAnalyze() {
							StringBuilder builder = new StringBuilder();
							if (!dlineage.getErrorMessages().isEmpty()) {
								if (dlineage.getDataFlow() != null && dlineage.getDataFlow().getTables() != null
										&& !dlineage.getDataFlow().getTables().isEmpty()) {
									builder.append("status=").append("2");
								} else {
									builder.append("status=").append("3");
								}
							} else {
								builder.append("status=").append("1");
							}
							long spendTime = System.currentTimeMillis() - analyzeStartTime;
							builder.append(", time=").append(spendTime).append("ms");
							builder.append(", file=").append(file.getAbsolutePath());
							builder.append(", length=").append(file.length());
							System.out.println(builder.toString());

							analyzeTotalTime[0] += spendTime;
						}
					});
					dlineage.generateDataFlow();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			System.out.println();
			System.out.println("Summary:");
			System.out.println("Total sql files: " + files.length);
			System.out.println("Total sql statements: " + totalStatementCount[0]);
			System.out.println("Total sql lines: " + totalLines[0]);
			int dealFileCount = files.length - ignoreFileCount;
			if (dealFileCount > 0) {
				System.out.println(
						"Average statements per file: " + Math.round(totalStatementCount[0] / (double) dealFileCount));
				System.out.println("Average lines per file :" + Math.round(totalLines[0] / (double) dealFileCount));
				System.out.println("Average lines per sql statement: "
						+ Math.round(totalLines[0] / (double) totalStatementCount[0]));

				BigDecimal success = new BigDecimal(successStatementCount[0] / (double) totalStatementCount[0]);
				success.setScale(2, BigDecimal.ROUND_HALF_UP);

				BigDecimal failed = new BigDecimal(1 - successStatementCount[0] / (double) totalStatementCount[0]);
				failed.setScale(2, BigDecimal.ROUND_HALF_UP);

				NumberFormat percent = NumberFormat.getPercentInstance();
				percent.setMaximumFractionDigits(2);

				System.out.println(
						"Statement passed: " + successStatementCount[0] + ", " + percent.format(success.doubleValue()));
				System.out.println("Statement failed: " + (totalStatementCount[0] - successStatementCount[0]) + ", "
						+ percent.format(failed.doubleValue()));
			}
			System.out.println("Total time escaped: " + analyzeTotalTime[0] + "ms");
			System.out.println(
					"Average time per file: " + +Math.round(analyzeTotalTime[0] / (double) dealFileCount) + "ms");
			System.out.println("Average time per sql statement: "
					+ Math.round(analyzeTotalTime[0] / (double) successStatementCount[0]) + "ms");
			// if (ignoreFileCount > 0)
			{
				System.out.println("Files ignored in trial version: " + ignoreFileCount);
			}
		}
	}

	private static TSQLDataSource createSQLDataSource(EDbVendor vendor, String host, String port, String user,
			String passowrd, String database, String schema) {
		try {
			if (vendor == EDbVendor.dbvoracle) {
				TOracleSQLDataSource datasource = new TOracleSQLDataSource(host, port, user, passowrd, database);
				if (schema != null) {
					datasource.setExtractedSchemas(schema);
				}
				return datasource;
			}
			if (vendor == EDbVendor.dbvmssql) {
				TMssqlSQLDataSource datasource = new TMssqlSQLDataSource(host, port, user, passowrd);
				if (database != null) {
					if (schema != null) {
						datasource.setExtractedDbsSchemas(database + "/" + schema);
					} else {
						datasource.setExtractedDbsSchemas(database);
					}
				}
				return datasource;
			}
			if (vendor == EDbVendor.dbvpostgresql) {
				TPostgreSQLDataSource datasource = new TPostgreSQLDataSource(host, port, user, passowrd, database);
				if (schema != null) {
					datasource.setExtractedDbsSchemas(database + "/" + schema);
				} else {
					datasource.setExtractedDbsSchemas(database);
				}
				return datasource;
			}
			if (vendor == EDbVendor.dbvgreenplum) {
				TGreenplumSQLDataSource datasource = new TGreenplumSQLDataSource(host, port, user, passowrd, database);
				if (schema != null) {
					datasource.setExtractedDbsSchemas(database + "/" + schema);
				} else {
					datasource.setExtractedDbsSchemas(database);
				}
				return datasource;
			}
			if (vendor == EDbVendor.dbvredshift) {
				TRedshiftSQLDataSource datasource = new TRedshiftSQLDataSource(host, port, user, passowrd, database);
				if (schema != null) {
					datasource.setExtractedDbsSchemas(database + "/" + schema);
				} else {
					datasource.setExtractedDbsSchemas(database);
				}
				return datasource;
			}
			if (vendor == EDbVendor.dbvmysql) {
				TMysqlSQLDataSource datasource = new TMysqlSQLDataSource(host, port, user, passowrd);
				if (database != null) {
					if (schema != null) {
						datasource.setExtractedDbsSchemas(database + "/" + schema);
					} else {
						datasource.setExtractedDbsSchemas(database);
					}
				}
				return datasource;
			}
			if (vendor == EDbVendor.dbvnetezza) {
				TNetezzaSQLDataSource datasource = new TNetezzaSQLDataSource(host, port, user, passowrd, database);
				if (database != null) {
					if (schema != null) {
						datasource.setExtractedDbsSchemas(database + "/" + schema);
					} else {
						datasource.setExtractedDbsSchemas(database);
					}
				}
				return datasource;
			}
			if (vendor == EDbVendor.dbvsnowflake) {
				TSnowflakeSQLDataSource datasource = new TSnowflakeSQLDataSource(host, port, user, passowrd);
				if (database != null) {
					if (schema != null) {
						datasource.setExtractedDbsSchemas(database + "/" + schema);
					} else {
						datasource.setExtractedDbsSchemas(database);
					}
				}
				return datasource;
			}
			if (vendor == EDbVendor.dbvteradata) {
				TTeradataSQLDataSource datasource = new TTeradataSQLDataSource(host, port, user, passowrd, database);
				if (database != null) {
					datasource.setExtractedDatabases(database);
				}
				return datasource;
			}
		} catch (Exception e) {
			System.err.println("Connect datasource failed. " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
