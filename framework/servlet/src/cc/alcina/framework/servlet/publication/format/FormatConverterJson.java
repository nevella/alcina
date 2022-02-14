package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_JSON;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.servlet.grid.ExcelExporter;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@Registration({ FormatConverter.class, FormatConversionTarget_JSON.class })
public class FormatConverterJson implements FormatConverter {
	@Override
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel fcm) throws Exception {
		Optional<TypedConverterJson> typedConverter = Registry.optional(
				TypedConverterJson.class, ctx.publicationContent.getClass());
		if (typedConverter.isPresent()) {
			String json = typedConverter.get().toJson(ctx.publicationContent);
			fcm.bytes = json.getBytes(StandardCharsets.UTF_8);
		}
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

	// FIXME - registry.2 - remove
	public static interface TypedConverterJson<T extends PublicationContent> {
		public String toJson(T publicationContent);
	}
}
