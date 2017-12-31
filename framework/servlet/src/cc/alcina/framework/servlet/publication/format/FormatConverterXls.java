package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_XLS;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.servlet.excel.ExcelExporter;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@RegistryLocation(registryPoint = FormatConverter.class, targetClass = FormatConversionTarget_XLS.class)
public class FormatConverterXls implements FormatConverter {
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		if (fcm.bytes == null) {
			ExcelExporter ee = new ExcelExporter();
			Document doc = ee.getTemplate();
			ee.addCollectionToBook(fcm.rows, doc, "results");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(baos, "UTF-8");
			XmlUtils.streamXML(doc, writer);
			fcm.bytes = baos.toByteArray();
		}
		return new ByteArrayInputStream(fcm.bytes);
	}

	public String getFileExtension() {
		return "xls";
	}

	public String getMimeType() {
		return "application/msexcel";
	}
}
