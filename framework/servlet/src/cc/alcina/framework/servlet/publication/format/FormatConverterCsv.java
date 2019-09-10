package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_CSV;
import cc.alcina.framework.entity.util.CsvCols;
import cc.alcina.framework.servlet.grid.ExcelExporter;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@RegistryLocation(registryPoint = FormatConverter.class, targetClass = FormatConversionTarget_CSV.class)
public class FormatConverterCsv implements FormatConverter {
	@Override
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		if (fcm.bytes == null) {
			ExcelExporter ee = new ExcelExporter();
			Document doc = ee.getTemplate();
			ee.addCollectionToBook(fcm.rows, doc, "results");
			CsvCols csvCols = new CsvCols((List) ee.getCellList());
			fcm.bytes = csvCols.toCsv().getBytes(StandardCharsets.UTF_8);
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
