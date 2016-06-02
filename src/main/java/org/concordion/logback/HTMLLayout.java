package org.concordion.logback;

import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;

import java.util.Map;

import org.slf4j.helpers.DataMarker;
import org.slf4j.helpers.ScreenshotMarker;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.helpers.Transform;
import ch.qos.logback.core.html.HTMLLayoutBase;
import ch.qos.logback.core.html.IThrowableRenderer;
import ch.qos.logback.core.pattern.Converter;

/**
 * 
 * HTMLLayout outputs events in an HTML table. <p> The content of the table
 * columns are specified using a conversion pattern. See
 * {@link ch.qos.logback.classic.PatternLayout} for documentation on the
 * available patterns. <p> For more information about this layout, please refer
 * to the online manual at
 * http://logback.qos.ch/manual/layouts.html#ClassicHTMLLayout
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */
public class HTMLLayout extends HTMLLayoutBase<ILoggingEvent> {
    /**
     * Default pattern string for log output.
     */
    static final String DEFAULT_CONVERSION_PATTERN = "%date{HH:mm:ss.SSS}%logger{30}%level%message";

    private IThrowableRenderer<ILoggingEvent> throwableRenderer;
    private StepRecorder stepRecorder = StepRecorder.STEP_MARKER;
	private int screenshotsTakenCount = 0;
	private int columnCount;

    /**
     * Constructs a PatternLayout using the DEFAULT_LAYOUT_PATTERN.
     * 
     * The default pattern just produces the application supplied message.
     */
    public HTMLLayout() {
        pattern = DEFAULT_CONVERSION_PATTERN;
        throwableRenderer = new HTMLThrowableRenderer();
        cssBuilder = new HTMLLayoutCssBuilder();
        columnCount = getColumnCount();
    }

    public void setStepRecorder(String value) {
		stepRecorder = StepRecorder.valueOf(value);
	}
    
    @Override
    public void setPattern(String conversionPattern) {
    	super.setPattern(conversionPattern);
    	columnCount = getColumnCount();
    };
    
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
        StringBuilder buf = new StringBuilder();
        startNewTableIfLimitReached(buf);

        if (containsMarker(event, HTMLLogMarkers.STEP) || event.getLevel() == stepRecorder.getLevel()) {
        	appendStepToBuffer(buf, event);
        	return buf.toString();
        }
        
        appendMessageToBuffer(buf, event);
        
        if (event.getMarker() instanceof ScreenshotMarker) {
			appendScreenshotToBuffer(buf, (ScreenshotMarker) event.getMarker());
        } 
        
        if (event.getMarker() instanceof DataMarker) {
			appendDataToBuffer(buf, (DataMarker) event.getMarker());
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
    	counter = 0;

		buf.append(LINE_SEPARATOR);
        buf.append("<tr>");
        buf.append(LINE_SEPARATOR);
        buf.append("<td class=\"step\" colspan=\"").append(columnCount + 1).append("\">");
        
        if (containsMarker(event, HTMLLogMarkers.HTML)) {
			buf.append(event.getMessage());
		} else {
			buf.append(Transform.escapeTags(event.getMessage()));
		}
        
        buf.append("</td>");
		buf.append(LINE_SEPARATOR);
		buf.append("</tr>");
	}
	
	private void appendMessageToBuffer(StringBuilder buf, ILoggingEvent event) {
        boolean odd = true;
        if (((counter++) & 1) == 0) {
            odd = false;
        }
        
        String level = event.getLevel().toString().toLowerCase();

        buf.append(LINE_SEPARATOR);
        buf.append("<tr class=\"");
        buf.append(level);
        if (odd) {
            buf.append(" odd\">");
        } else {
            buf.append(" even\">");
        }
        buf.append(LINE_SEPARATOR);
        buf.append("<td class=\"even\"></td>");
    
        Converter<ILoggingEvent> c = head;
        while (c != null) {
			appendEventToBuffer(buf, c, event);
            c = c.getNext();
        }
        
        buf.append("</tr>");
	}

	private void appendEventToBuffer(StringBuilder buf, Converter<ILoggingEvent> c, ILoggingEvent event) {
        buf.append("<td class=\"");
        buf.append(computeConverterName(c));
        buf.append("\">");
		if (containsMarker(event, HTMLLogMarkers.HTML)) {
			buf.append(c.convert(event));
		} else {
			buf.append(Transform.escapeTags(c.convert(event)));
		}
        buf.append("</td>");
        buf.append(LINE_SEPARATOR);
    }
	
	public void appendScreenshotToBuffer(StringBuilder buf, ScreenshotMarker screenshot) {
		buf.append(LINE_SEPARATOR);
		buf.append("<tr>");
		buf.append(LINE_SEPARATOR);
        buf.append("<td></td><td colspan=\"").append(columnCount).append("\">");
        
		try {
			buf.append("<img src=\"").append(screenshot.writeScreenshot(screenshotsTakenCount)).append("\"/>");
			screenshotsTakenCount++;

		} catch (Exception e) {
			buf.append(e.getMessage());
		}

		buf.append("</td>");
		buf.append(LINE_SEPARATOR);
		buf.append("</tr>");
	}

	public void appendDataToBuffer(StringBuilder buf, DataMarker data) {
		buf.append(LINE_SEPARATOR);
		buf.append("<tr>");
		buf.append(LINE_SEPARATOR);
		buf.append("<td></td><td colspan=\"").append(columnCount).append("\">");
		
		try {
			buf.append("<pre>");
			buf.append(LINE_SEPARATOR);

			if (data.escapeData()) {
				buf.append(Transform.escapeTags(data.getData()));
			} else {
				buf.append(data.getData());
			}
			
			buf.append(LINE_SEPARATOR);
			buf.append("</pre>");
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
        } else {
            return super.computeConverterName(c);
        }
    }

    @Override
    public String getPresentationHeader() {
        StringBuilder sbuf = new StringBuilder();
//        sbuf.append("<hr/>");
//        sbuf.append(LINE_SEPARATOR);
        sbuf.append("<p>Log session start time ");
        sbuf.append(new java.util.Date());
        sbuf.append("</p><p></p>");
        sbuf.append(LINE_SEPARATOR);
        sbuf.append(LINE_SEPARATOR);
        sbuf.append("<table cellspacing=\"0\">");
        sbuf.append(LINE_SEPARATOR);

        buildHeaderRowForTable(sbuf);

        return sbuf.toString();
    }

    private void buildHeaderRowForTable(StringBuilder sbuf) {
        Converter c = head;
        String name;
        sbuf.append("<tr class=\"header\"><td style=\"width:50px\"></td>");
        sbuf.append(LINE_SEPARATOR);
        while (c != null) {
            name = computeConverterName(c);
            if (name == null) {
                c = c.getNext();
                continue;
            }
            // sbuf.append("<td class=\"").append(name).append("\">");
            sbuf.append("<td>");
            sbuf.append(name.replaceAll("(.)([A-Z])", "$1&nbsp;$2"));
            sbuf.append("</td>");
            sbuf.append(LINE_SEPARATOR);
            c = c.getNext();
        }
        sbuf.append("</tr>");
        sbuf.append(LINE_SEPARATOR);
    }

	private boolean containsMarker(ILoggingEvent event, String name) {
		if (event.getMarker() == null) {
			return false;
		}
		
		return event.getMarker().contains(name);
	}
	
	private int getColumnCount() {
		return pattern.length() - pattern.replace("%", "").length();
	}
}
