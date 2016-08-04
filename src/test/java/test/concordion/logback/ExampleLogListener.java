package test.concordion.logback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class ExampleLogListener extends LoggingListener {
	ByteArrayOutputStream stream = new ByteArrayOutputStream();

	@Override
	protected void append(ILoggingEvent event) {
		try {
			stream.write(event.getFormattedMessage().getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String[] getFilterMarkers() {
		return null;
	}

	public String getStreamContent() {
		return stream.toString();
	}

	public void resetStream() {
		stream.reset();
	}
}
