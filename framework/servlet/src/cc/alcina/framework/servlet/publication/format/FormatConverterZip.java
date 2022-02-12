package cc.alcina.framework.servlet.publication.format;

import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_ZIP;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@RegistryLocation(registryPoint = FormatConverter.class, targetClass = FormatConversionTarget_ZIP.class)
@Registration({ FormatConverter.class, FormatConversionTarget_ZIP.class })
public class FormatConverterZip implements FormatConverter {
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		return fcm.provideByteStream();
	}

	public String getFileExtension() {
		return "zip";
	}

	public String getMimeType() {
		return "application/zip";
	}
}
