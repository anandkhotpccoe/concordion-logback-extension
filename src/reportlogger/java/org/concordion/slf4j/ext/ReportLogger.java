/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.concordion.slf4j.ext;

import org.concordion.slf4j.markers.ReportLoggerMarkers;
import org.slf4j.Logger;
import org.slf4j.ext.LoggerWrapper;

/**
 * A utility that provides standard mechanisms for logging certain kinds of
 * activities.
 * 
 * @author Andrew Sumner
 */
public class ReportLogger extends LoggerWrapper {
	private static FluentLogger bufferedLogger = null;
	
	/**
	 * Given an underlying logger, construct an XLogger
	 * 
	 * @param logger
	 *            underlying logger
	 */
	public ReportLogger(Logger logger) {
		super(logger, LoggerWrapper.class.getName());
	}

	/**
	 * Logs progress of test suite to console - not added to log file.
	 * 
	 * @param format
	 *            the format string
	 * @param arguments
	 *            a list of arguments
	 */
	public void progress(String format, Object... arguments) {
		logger.info(ReportLoggerMarkers.PROGRESS_MARKER, format, arguments);
	}

	/**
	 * Logs a step.
	 * 
	 * @param format
	 *            the format string
	 * @param arguments
	 *            a list of arguments
	 */
	public void step(String format, Object... arguments) {
		logger.info(ReportLoggerMarkers.STEP_MARKER, format, arguments);
	}

	/**
	 * Access custom reporting methods such as data, html, and screenshots.
	 * 
	 * @return A FluentLogger
	 */
	public FluentLogger with() {
		writeBufferedMessage();
		
		return new FluentLogger(logger, instanceofLAL);
	}
	
	/**
	 * Buffer the desired log entry until you push it using {@link ReportLogger#writeBufferedMessage()}
	 * or another log entry is written forcing this one to write.
	 * 
	 * <p>
	 * TODO: Implement a listener so this applies to all log entries - not just those ones using ReportLogger.
	 * </p>
	 * 
	 * @return A FluentLogger
	 */
	public FluentLogger withBuffered() {
		writeBufferedMessage();
		
		bufferedLogger = new FluentLogger(logger, instanceofLAL, true);
		return bufferedLogger;
	}
	
	/**
	 * @return True if a buffered message is waiting to be written.
	 */
	public boolean hasBufferedMessage() {
		return bufferedLogger != null;
	}
	
	/**
	 * Write the buffered entry to the logs.
	 */
	public void writeBufferedMessage() {
		if (hasBufferedMessage()) {
			bufferedLogger.writeBufferedEntry();
			bufferedLogger = null;
		}
	}
	
	/**
	 * Clear the buffered entry, it will not get written to the logs.
	 */
	public void clearBufferedMessage() {
		bufferedLogger = null;
	}
	
	/**
	 * @return The buffered log entry so it can be updated.
	 */
	public FluentLogger getBufferedMessage() {
		return bufferedLogger;
	}
	
	/**
	 * Logs a tool tip.
	 * 
	 * @param format
	 *            the format string
	 * @param arguments
	 *            a list of arguments
	 */
	public void tooltip(String format, Object... arguments) {
		debug(ReportLoggerMarkers.TOOLTIP_MARKER, format, arguments);
	}
}
