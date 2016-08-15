package org.concordion.ext.loggingFormatter;

import java.io.File;

/**
 * Class to handle setting/removing MDC on per test case basis. This helps us log each test case into it's own log file. 
 * @see <a href="http://logback.qos.ch/manual/appenders.html#SiftingAppender">Sifting Appender</a>
 * @see <a href="http://logback.qos.ch/manual/mdc.html">MDC</a>
 */
public interface ILoggingAdaptor
{
	/**
	 * Update logger so that logging statements are directed a file
	 *
	 * @param testPath Path of the current test
	 * @param stylesheet Name of a style sheet to append to the log file 
	 */
	public void startSpecificationLogFile(String testPath, String stylesheet);
	
	/**
	 * Update logger so that logging statements for a specific example are directed to specified file
	 * 
	 * @param testPath Path of the current test
	 * @param exampleName Name of the current example 
	 */
	public void startExampleLogFile(String testPath, String exampleName);

	/**
	 * Stop directing logging statements to test specific log file
	 */
	public void stopLogFile();
	
	/**
	 * Checks to see if a log file has been created for this test
	 * 
	 * @return true or false
	 */
	public boolean logFileExists();
	
	/**
	 * Return the file for the currently active log.
	 * 
	 * @return the log file, if one exists otherwise null
	 */
	public File getLogFile();
}