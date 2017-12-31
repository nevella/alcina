package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_XLSX;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@RegistryLocation(registryPoint = FormatConverter.class, targetClass = FormatConversionTarget_XLSX.class)
public class FormatConverterXlsx implements FormatConverter {
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		return new ByteArrayInputStream(fcm.bytes);
	}

	public String getFileExtension() {
		return "xlsx";
	}

	public String getMimeType() {
		return "application/msexcel";
	}
}
