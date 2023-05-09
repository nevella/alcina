package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_XLSX;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

public class FormatConverterXlsx
		implements FormatConverter<FormatConversionTarget_XLSX> {
	@Override
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		return new ByteArrayInputStream(fcm.bytes);
	}

	@Override
	public String getFileExtension() {
		return "xlsx";
	}

	@Override
	public String getMimeType() {
		return "application/msexcel";
	}
}
