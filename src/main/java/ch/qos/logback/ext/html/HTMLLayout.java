package ch.qos.logback.ext.html;

// TODO: Expand/Collapse button for Data; Copy Text Button for Data; Link = Eye on example.

import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.concordion.ext.loggingFormatter.LogbackAdaptor;
import org.concordion.slf4j.markers.BaseDataMarker;
import org.concordion.slf4j.markers.DataMarker;
import org.concordion.slf4j.markers.HtmlMessageMarker;
import org.concordion.slf4j.markers.ReportLoggerMarkers;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.DateConverter;
import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.helpers.Transform;
import ch.qos.logback.core.html.HTMLLayoutBase;
import ch.qos.logback.core.html.IThrowableRenderer;
import ch.qos.logback.core.pattern.Converter;

/**
 * 
 * HTMLLayout outputs events in an HTML table.
 * <p>
 * The content of the table
 * columns are specified using a conversion pattern. See
 * {@link ch.qos.logback.classic.PatternLayout} for documentation on the
 * available patterns.
 * <p>
 * For more information about this layout, please refer
 * to the online manual at
 * http://logback.qos.ch/manual/layouts.html#ClassicHTMLLayout
 * 
 * @author Andrew Sumner
 */
public class HTMLLayout extends HTMLLayoutBase<ILoggingEvent> {
    /**
     * Default pattern string for log output.
     */
    static final String DEFAULT_CONVERSION_PATTERN = "%date{HH:mm:ss.SSS}%logger{30}%level%message";

    private IThrowableRenderer<ILoggingEvent> throwableRenderer;
    private StepRecorder stepRecorder = StepRecorder.STEP_MARKER;
	private Format format = Format.COLUMN;
	private int columnCount;
	private PatternLayout stringLayout = null;
	private String stylesheet = "";
	
    /**
     * Constructs a PatternLayout using the DEFAULT_LAYOUT_PATTERN.
     * 
     * The default pattern just produces the application supplied message.
     */
    public HTMLLayout() {
        pattern = DEFAULT_CONVERSION_PATTERN;
        throwableRenderer = new HTMLThrowableRenderer();
		cssBuilder = null;
        columnCount = getColumnCount();
    }

    public void setStepRecorder(String value) {
		stepRecorder = StepRecorder.valueOf(value);
	}

	public String getStepRecorder() {
		return stepRecorder.name();
	}

	public void setFormat(String value) {
		format = Format.valueOf(value);
	}
	
	public String getFormat() {
		return format.name();
	}

	public void setStylesheet(String value) {
		stylesheet = value;
	}

	public String getStylesheet() {
		return stylesheet;
	}

	public boolean hasStylesheet() {
		if (stylesheet == null || stylesheet.isEmpty()) {
			LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
			stylesheet = context.getProperty(LogbackAdaptor.LAYOUT_STYLESHEET);
		}

		return stylesheet != null && !stylesheet.isEmpty();
	}

    @Override
    public void setPattern(String conversionPattern) {
    	super.setPattern(conversionPattern);
    	columnCount = getColumnCount();
	}
    
    @Override
    public void start() {
        int errorCount = 0;
        if (throwableRenderer == null) {
            addError("ThrowableRender cannot be null.");
            errorCount++;
        }
        if (errorCount == 0) {
            super.start();
        }
    }

    protected Map<String, String> getDefaultConverterMap() {
        return PatternLayout.defaultConverterMap;
    }

    public String doLayout(ILoggingEvent event) {
		if (containsMarker(event, ReportLoggerMarkers.PROGRESS_MARKER)) {
			return "";
		}

        StringBuilder buf = new StringBuilder();
        startNewTableIfLimitReached(buf);

		if (containsMarker(event, ReportLoggerMarkers.STEP_MARKER.getName()) || event.getLevel() == stepRecorder.getLevel()) {
			appendStepToBuffer(buf, event);
        	return buf.toString();
        }
        
		appendMessageToBuffer(buf, event);
        
		if (containsMarker(event, ReportLoggerMarkers.DATA_MARKER_NAME)) {
			appendDataToBuffer(buf, (BaseDataMarker<?>) getMarker(event.getMarker(), ReportLoggerMarkers.DATA_MARKER_NAME));
        }

        if (event.getThrowableProxy() != null) {
        	if (throwableRenderer instanceof HTMLThrowableRenderer) {
        		((HTMLThrowableRenderer) throwableRenderer).setColumnCount(columnCount);
        	}
        		
            throwableRenderer.render(buf, event);
        }
        
        return buf.toString();
    }

	public void appendStepToBuffer(StringBuilder buf, ILoggingEvent event) {
		buf.append(LINE_SEPARATOR);
		buf.append("<tr class=\"record step\">");
        buf.append(LINE_SEPARATOR);
		buf.append("<th colspan=\"").append(columnCount + 1).append("\">");
        
		if (event.getMarker() instanceof DataMarker) {
			buf.append(event.getFormattedMessage());
		} else {
			buf.append(Transform.escapeTags(event.getFormattedMessage()));
		}
        
        buf.append("</th>");
		buf.append(LINE_SEPARATOR);
		buf.append("</tr>");
	}
	
	private String getFormattedMessage(String format, Object[] arguments) {
		return MessageFormatter.arrayFormat(format, arguments).getMessage();
	}
	
	private void appendMessageToBuffer(StringBuilder buf, ILoggingEvent event) {
		boolean escapeTags = true;
		Field field = null;
		String originalMessage = null;
		
        buf.append(LINE_SEPARATOR);
        buf.append("<tr class=\"record\">");
        buf.append(LINE_SEPARATOR);
		buf.append("<td></td>");
		
		if (containsMarker(event, HtmlMessageMarker.MARKER_NAME)) {
			// Replace plain log message with HTML formatted version 
			escapeTags = false;
			
			HtmlMessageMarker marker = (HtmlMessageMarker) getMarker(event.getMarker(), HtmlMessageMarker.MARKER_NAME);
			
			try {
				originalMessage = event.getFormattedMessage();
				
				field = event.getClass().getDeclaredField("formattedMessage");
				field.setAccessible(true);
				field.set(event, getFormattedMessage(marker.getFormat(), event.getArgumentArray()));
			} catch (Throwable e) {
				// Silently ignore
			}
		}
		
		Converter<ILoggingEvent> c = head;
		if (format == Format.COLUMN) {
			while (c != null) {
				appendEventToBuffer(buf, c, event, escapeTags);
				c = c.getNext();
			}
		} else {
			buf.append("<td>");
			
			if (stringLayout == null) {
				stringLayout = new PatternLayout();
				stringLayout.setPattern(this.getPattern());
				stringLayout.setContext(this.getContext());
				stringLayout.start();
			}
			
			String text = stringLayout.doLayout(event);

			if (escapeTags) {
				buf.append(Transform.escapeTags(text));
			} else {
				buf.append(text);
			}
			buf.append("</td>");
        }
        
		if (field != null) {
			try {
				field.set(event, originalMessage);
			} catch (Throwable e) {
				// Silently ignore
			}
		}
		
        buf.append("</tr>");
	}

	private void appendEventToBuffer(StringBuilder buf, Converter<ILoggingEvent> c, ILoggingEvent event, boolean escapeTags) {
		String name = computeConverterName(c);
		
        buf.append("<td class=\"");
        buf.append(name);
        if (name.equalsIgnoreCase("Level")) {
        	buf.append(" ").append(event.getLevel().toString().toLowerCase());	
        }
        buf.append("\">");
		if (escapeTags) {
			buf.append(TransformText.escapeText(Transform.escapeTags(c.convert(event))));
		} else {
			buf.append(c.convert(event));
		}
        buf.append("</td>");
        buf.append(LINE_SEPARATOR);
    }
	
	public void appendDataToBuffer(StringBuilder buf, BaseDataMarker<?> data) {
		if (!data.hasData()) {
			return;
		}

		buf.append(LINE_SEPARATOR);
		buf.append("<tr class=\"companion\">");
		buf.append(LINE_SEPARATOR);
		buf.append("<td class=\"indent\"></td><td colspan=\"").append(columnCount).append("\" class=\"output\">");
		
		try {
			buf.append(LINE_SEPARATOR);

			buf.append(data.getFormattedData());
			
			buf.append(LINE_SEPARATOR);
		} catch (Exception e) {
			buf.append(e.getMessage());
		}
		
		buf.append("</td>");
		buf.append(LINE_SEPARATOR);
		buf.append("</tr>");
	}

	public IThrowableRenderer<?> getThrowableRenderer() {
        return throwableRenderer;
    }

    public void setThrowableRenderer(IThrowableRenderer<ILoggingEvent> throwableRenderer) {
        this.throwableRenderer = throwableRenderer;
    }

    @Override
	protected String computeConverterName(@SuppressWarnings("rawtypes") Converter c) {
        if (c instanceof MDCConverter) {
            MDCConverter mc = (MDCConverter) c;
            String key = mc.getFirstOption();
            if (key != null) {
                return key;
            } else {
                return "MDC";
            }
		} else if (c instanceof DateConverter) {
			// Check if format contains only time related date pattern
			// * http://logback.qos.ch/manual/layouts.html
			// * http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
			String option = ((DateConverter) c).getFirstOption();
			if (option != null) {
				option = option.replaceAll("[ HmsS:.,kKzZXa]", "");
				if (option.isEmpty()) {
					return "Time";
				}
			}
		}

		return super.computeConverterName(c);
    }

    @Override
	public String getFileHeader() {
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
		sbuf.append(" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		sbuf.append(LINE_SEPARATOR);
		sbuf.append("<html>");
		sbuf.append(LINE_SEPARATOR);
		sbuf.append("  <head>");
		sbuf.append(LINE_SEPARATOR);
		sbuf.append("    <title>").append(title).append("</title>").append(LINE_SEPARATOR);
		if (hasStylesheet()) {
			sbuf.append("    <link rel=\"stylesheet\" type=\"text/css\" href=\"").append(stylesheet).append("\"/>").append(LINE_SEPARATOR);
		}
		
		sbuf.append(readFile("htmllog.css"));
		sbuf.append(readFile("htmllog.js"));
		// cssBuilder.addCss(sbuf);

		sbuf.append(LINE_SEPARATOR);
		sbuf.append("  </head>");
		sbuf.append(LINE_SEPARATOR);
		sbuf.append("<body>");
		sbuf.append(LINE_SEPARATOR);

		// Required for screenshot popup
		sbuf.append("<img id=\"ScreenshotPopup\" class=\"screenshot\" />");

		return sbuf.toString();
	}

	@Override
    public String getPresentationHeader() {
        StringBuilder sbuf = new StringBuilder();
//        sbuf.append("<hr/>");
//        sbuf.append(LINE_SEPARATOR);
        sbuf.append("<h1>Log session start time ");
        sbuf.append(new java.util.Date());
        sbuf.append("</h1><p></p>");
        sbuf.append(LINE_SEPARATOR);
        sbuf.append(LINE_SEPARATOR);
        sbuf.append("<table>");
        sbuf.append(LINE_SEPARATOR);

        buildHeaderRowForTable(sbuf);

        return sbuf.toString();
    }

    private void buildHeaderRowForTable(StringBuilder sbuf) {
		Converter<?> c = head;
        String name;
        sbuf.append("<thead>");
        sbuf.append(LINE_SEPARATOR);
        
		sbuf.append("<tr><th class=\"Row\">Row</th>");
        sbuf.append(LINE_SEPARATOR);
        
        if (format == Format.COLUMN) {
	        while (c != null) {
	            name = computeConverterName(c);
	            if (name == null) {
	                c = c.getNext();
	                continue;
	            }
	            
	            sbuf.append("<th class=\"").append(name).append("\">");
	            sbuf.append(name.replaceAll("(.)([A-Z])", "$1&nbsp;$2"));
	            sbuf.append("</th>");
	            sbuf.append(LINE_SEPARATOR);
	            c = c.getNext();
	        }
        } else {
			sbuf.append("<th>Message</th>");
        }
        
        sbuf.append("</tr>");
        sbuf.append(LINE_SEPARATOR);
        sbuf.append("</thead>");
        sbuf.append(LINE_SEPARATOR);
        sbuf.append("<tbody>");
        sbuf.append(LINE_SEPARATOR);
    }

	private boolean containsMarker(ILoggingEvent event, Marker marker) {
		if (event.getMarker() == null) {
			return false;
		}

		return event.getMarker().contains(marker);
	}

	private boolean containsMarker(ILoggingEvent event, String name) {
		if (event.getMarker() == null) {
			return false;
		}
		
		return event.getMarker().contains(name);
	}

	private Marker getMarker(Marker reference, String name) {
		if (reference == null) {
			return null;
		}

		if (reference.getName().equals(name)) {
			return reference;
		}
		
		Iterator<Marker> references = reference.iterator();
		while (references.hasNext()) {
			Marker found = getMarker(references.next(), name);
			
			if (found != null) {
				return found;
			}
		}
		
		return null;
	}
	
	private int getColumnCount() {
		if (format == Format.COLUMN) {
			return pattern.length() - pattern.replace("%", "").length();
		} else {
			return 1;
		}
	}

	public static String readFile(String filename) {
		InputStream input = null;

		try {
			input = HTMLLayout.class.getResourceAsStream(filename);
			if (input != null) {
				return IOUtils.toString(input, StandardCharsets.UTF_8.name());
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		}

		return null;
	}
}
