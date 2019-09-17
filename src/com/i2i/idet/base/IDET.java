package com.i2i.idet.base;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.smartxls.WorkBook;

public class IDET {

	static final long LOG4J_CONFIG_CHECK_INTERVAL = TimeUnit.MILLISECONDS
			.convert(5, TimeUnit.MINUTES);

	static Logger logger = Logger.getLogger(IDET.class);
	static PreparedStatement ps;

	public static void main(String[] args) {

		CommandLineParser parser = new GnuParser();
		Options optionsCMD = new Options();
		optionsCMD.addOption(new Option("f", "input file info", true,
				" input id"));
		optionsCMD.addOption(new Option("s", "input file size", true,
				" input size"));
		CommandLine cmdLine = null;
		try {
			cmdLine = parser.parse(optionsCMD, args);
		} catch (ParseException e1) {
			logger.error(e1.getMessage());
			return;
		}

		DOMConfigurator.configureAndWatch("config/idet_log4j_config.xml",
				LOG4J_CONFIG_CHECK_INTERVAL);

		String[] options = { "1) eFirma_Firm_Template_v1.0.xlsx",
				"2) eFirma_Dealer_Template_v1.0.xlsx",
				"3) EMPTOR_Firm_Template_v1.0.xlsx",
				"4) EMPTOR_Dealer_Template_v1.0.xlsx",
				"5) EMPTOR_Employee_Template_v1.0.xlsx",
				"6) DKCYS_Employee_Template_v1.0.xlsx",
				"7) DKCYS_Dealer_Template_v1.0.xlsx",
				"8) COST_CENTER_INFO.xlsx", "9) MIG_SOL_KURUMSAL_DEALER.xlsx",
				"10) MIG_HISTORIC_POSITION.xlsx", "11) MIG_IP_POOL.xlsx",
				"12) MIG_TABLES_FOR_BACH.xlsx", "13) MIG_GENERIC.xlsx",
				"14) EOS_EMP_VIEW.xlsx", "15) ERP_FIRM_UPDATE.xlsx" };

		String selectedFile = "0";

		if (cmdLine != null && cmdLine.getOptionValue("f") != null) {
			selectedFile = cmdLine.getOptionValue("f");
		} else {
			for (String option : options)
				System.out.println(option);
			System.out.print("Hangi exceli yüklemek istediğinizi seçiniz : ");

			try {
				BufferedReader bufferRead = new BufferedReader(
						new InputStreamReader(System.in));
				selectedFile = bufferRead.readLine();
			} catch (IOException e) {
				logger.error(e.getMessage());
				return;
			}
		}

		try {
			String configName = null;
			switch (Integer.parseInt(selectedFile)) {
			case 1:
				configName = "configuration_MIG_EFIRMA_FIRM.txt";
				break;

			case 2:
				configName = "configuration_MIG_EFIRMA_DEALER.txt";
				break;

			case 3:
				configName = "configuration_MIG_EMPTOR_FIRM.txt";
				break;

			case 4:
				configName = "configuration_MIG_EMPTOR_DEALER.txt";
				break;

			case 5:
				configName = "configuration_MIG_EMPTOR_EMPLOYEE.txt";
				break;

			case 6:
				configName = "configuration_MIG_DKCYS_EMPLOYEE.txt";
				break;

			case 7:
				configName = "configuration_MIG_DKCYS_DEALER.txt";
				break;

			case 8:
				configName = "configuration_MIG_COSTCENTERINFO.txt";
				break;

			case 9:
				configName = "configuration_MIG_SOL_KURUMSAL_DEALER.txt";
				break;

			case 10:
				configName = "configuration_MIG_HISTORIC_POSITION.txt";
				break;

			case 11:
				configName = "configuration_MIG_IP_POOL.txt";
				break;

			case 12:
				configName = "configuration_MIG_TABLES_FOR_BACH.txt";
				break;

			case 13:
				configName = "configuration_MIG_GENERIC.txt";
				break;

			case 14:
				configName = "configuration_EOS_EMPLOYEE_VIEW.txt";
				break;

			case 15:
				configName = "configuration_MIG_ERP_FIRM_TMP.txt";
				break;

			default:
				logger.warn("Geçersiz değer!");
				break;
			}

			logger.debug("Bulk Process started!!!");
			Map<String, String> configMap = readInsertFile(configName);

			Class.forName(configMap.get("DbDriver"));
			Connection conn = DriverManager.getConnection(
					configMap.get("DbURL"), configMap.get("DbUserName"),
					configMap.get("DbPassword"));
			conn.setAutoCommit(false);
			ps = conn.prepareStatement(configMap.get("insertQuery"));

			String[] columnDataType = configMap.get("columnDataType")
					.split(",");
			DateFormat formatter = null;
			if (configMap.get("dateFormatter") != null)
				formatter = new SimpleDateFormat(
						configMap.get("dateFormatter"), Locale.UK);

			WorkBook m_book = new WorkBook();
			String fileName = configMap.get("migrationFile");
			if (cmdLine != null && cmdLine.getOptionValue("s") != null)
				fileName.replace(".xlsx", cmdLine.getOptionValue("s") + ".xlsx");
			m_book.readXLSX(fileName);
			m_book.setSheet(0);
			int lastCol = m_book.getLastCol();
			int lastRow = m_book.getLastRow();

			HashMap<Integer, Object> faultyRecord = new HashMap<Integer, Object>();
			Object firstColumn = null;

			for (int i = 1; i <= lastRow; i++) {
				for (int j = 0; j < lastCol + 1; j++) {
					if (j < columnDataType.length) {
						if (columnDataType[j].equals("String")) {
							if (!m_book.getText(i, j).toLowerCase()
									.equals("null")
									&& !m_book.getText(i, j).equals(""))
								ps.setString(j + 1, m_book.getText(i, j));
							else
								ps.setString(j + 1, null);
						} else if (columnDataType[j].equals("Date")) {
							if (m_book.getText(i, j) != null
									&& !m_book.getText(i, j).equals(""))
								try {
									if (configMap.get("dateReplaceMonth") == null
											|| !new Boolean(
													configMap
															.get("dateReplaceMonth")))
										ps.setTimestamp(
												j + 1,
												new Timestamp(
														formatter
																.parse(m_book
																		.getFormattedText(
																				i,
																				j)
																		.length() < 19 ? m_book
																		.getFormattedText(
																				i,
																				j)
																		.concat(" 00:00:00")
																		: m_book.getFormattedText(
																				i,
																				j))
																.getTime()));
									else
										ps.setDate(
												j + 1,
												new Date(
														formatter
																.parse(replaceMonthValue(m_book
																		.getFormattedText(
																				i,
																				j)))
																.getTime()));
								} catch (Exception e) {
									e.printStackTrace();
									ps.setDate(j + 1, null);
								}
							else
								ps.setDate(j + 1, null);
						} else if (columnDataType[j].equals("Number")) {
							if (m_book.getText(i, j) != null
									&& !m_book.getText(i, j).equals(""))
								ps.setLong(j + 1,
										Long.parseLong(m_book.getText(i, j)));
							else
								ps.setString(j + 1, null);
						}

						if (j == 0)
							firstColumn = m_book.getText(i, j);
					}
				}
				ps.addBatch();
				if (i % 100 == 0) {
					try {
						logger.debug("Okunan Satır Sayısı = " + i);
						ps.executeBatch();
					} catch (Exception e) {
						e.printStackTrace();
						faultyRecord.put(i, firstColumn);
					}
				}
			}
			// i % 1 de Mod > 1 olursa comment out edilmeli
			// try {
			// ps.executeBatch();
			// } catch (Exception e) {
			// }

			for (Map.Entry<Integer, Object> entry : faultyRecord.entrySet())
				logger.debug("Hatali kayit sira no = " + entry.getKey()
						+ "    Hatali kayit ilk sütun = " + entry.getValue());

			ps.executeBatch();
			conn.commit();
			ps.close();
			conn.close();

			logger.debug("Data have " + lastRow + " rows and " + (lastCol + 1)
					+ " colums.!!!");
			logger.debug(configMap.get("migrationFile"));
			logger.debug(configMap.get("insertQuery"));
		} catch (Exception e) {
			logger.debug("ERROR : " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	private static Map<String, String> readInsertFile(String configName)
			throws IOException {

		Map<String, String> configMap = new HashMap<String, String>();

		BufferedReader br = new BufferedReader(new FileReader("config/"
				+ configName));
		String line = "";

		while (line != null) {
			line = br.readLine();
			if (line != null && !line.trim().equals("")) {
				String[] configParams = line.split("=");
				configMap.put(configParams[0].trim(), configParams[1].trim());
			}
		}

		br.close();
		return configMap;
	}

	private static String replaceMonthValue(String date) {
		final Map<String, String> monthValues = new HashMap<String, String>();
		monthValues.put("Oca", "01");
		monthValues.put("Şub", "02");
		monthValues.put("Mar", "03");
		monthValues.put("Nis", "04");
		monthValues.put("May", "05");
		monthValues.put("Haz", "06");
		monthValues.put("Tem", "07");
		monthValues.put("Ağu", "08");
		monthValues.put("Eyl", "09");
		monthValues.put("Eki", "10");
		monthValues.put("Kas", "11");
		monthValues.put("Ara", "12");

		return date.substring(0, 20).replace(date.substring(3, 6),
				monthValues.get(date.substring(3, 6)));
	}
}
