package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_CSV;
import cc.alcina.framework.entity.util.Csv;
import cc.alcina.framework.servlet.grid.ExcelExporter;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

public class FormatConverterCsv
		implements FormatConverter<FormatConversionTarget_CSV> {
	@Override
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		if (fcm.bytes == null) {
			ExcelExporter ee = new ExcelExporter();
			Document doc = ee.getTemplate();
			ee.addCollectionToBook(fcm.rows, doc, "results");
			Csv csv = new Csv((List) ee.getCellList());
			fcm.bytes = csv.toCsvString().getBytes(StandardCharsets.UTF_8);
		}
		return new ByteArrayInputStream(fcm.bytes);
	}

	@Override
	public String getFileExtension() {
		return "csv";
	}

	@Override
	public String getMimeType() {
		return "text/csv";
	}
}
