package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_JSON;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.servlet.grid.ExcelExporter;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@RegistryLocation(registryPoint = FormatConverter.class, targetClass = FormatConversionTarget_JSON.class)
public class FormatConverterJson implements FormatConverter {
	@Override
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		if (fcm.bytes == null) {
			ExcelExporter exporter = new ExcelExporter();
			Document doc = exporter.getTemplate();
			exporter.addCollectionToBook(fcm.rows, doc, "results");
			List<List> cellList = exporter.getCellList();
			String json = JacksonUtils.toNestedJsonList(cellList);
			fcm.bytes = json.getBytes(StandardCharsets.UTF_8);
		}
		return new ByteArrayInputStream(fcm.bytes);
	}

	@Override
	public String getFileExtension() {
		return "json";
	}

	@Override
	public String getMimeType() {
		return "application/json";
	}
}
