package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_TEXT;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

public class FormatConverterText
		implements FormatConverter<FormatConversionTarget_TEXT> {
	@Override
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		return new ByteArrayInputStream(fcm.bytes);
	}

	@Override
	public String getFileExtension() {
		return "txt";
	}

	@Override
	public String getMimeType() {
		return "text/plain";
	}
}
