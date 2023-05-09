package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_XLS;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.servlet.grid.ExcelExporter;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

public class FormatConverterXls
		implements FormatConverter<FormatConversionTarget_XLS> {
	@Override
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

	@Override
	public String getFileExtension() {
		return "xls";
	}

	@Override
	public String getMimeType() {
		return "application/msexcel";
	}
}
