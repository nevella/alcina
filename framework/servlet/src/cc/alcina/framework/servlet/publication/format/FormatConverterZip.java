package cc.alcina.framework.servlet.publication.format;

import java.io.InputStream;

import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_ZIP;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

public class FormatConverterZip
		implements FormatConverter<FormatConversionTarget_ZIP> {
	@Override
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		return fcm.provideByteStream();
	}

	@Override
	public String getFileExtension() {
		return "zip";
	}

	@Override
	public String getMimeType() {
		return "application/zip";
	}
}
