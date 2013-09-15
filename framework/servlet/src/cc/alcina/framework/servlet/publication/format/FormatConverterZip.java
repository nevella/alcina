package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_ZIP;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@RegistryLocation(registryPoint = FormatConverter.class, targetClass = FormatConversionTarget_ZIP.class)
public class FormatConverterZip implements FormatConverter {
	public InputStream convert(PublicationContext ctx,FormatConversionModel fcm) throws Exception {
		return new ByteArrayInputStream(fcm.bytes);
	}

	public String getMimeType() {
		return "application/zip";
	}

	public String getFileExtension() {
		return "zip";
	}
}
