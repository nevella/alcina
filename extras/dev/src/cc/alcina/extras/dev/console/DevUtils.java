package cc.alcina.extras.dev.console;

import java.util.Base64;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableRowBuilder;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Cell;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.Csv;

public class DevUtils {
	public static String fileToBase64(String path) {
		try {
			byte[] bytes = Io.read().path(path).asBytes();
			String encoded = Base64.getEncoder().encodeToString(bytes);
			DevConsole.get().setClipboardContents(encoded);
			return encoded;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static String fileToDataUrl(String path) {
		return fileToDataUrl(path, "image/png");
	}

	public static String fileToDataUrl(String path, String mimeType) {
		try {
			byte[] bytes = Io.read().path(path).asBytes();
			String encoded = Base64.getEncoder().encodeToString(bytes);
			String url = Ax.format("data:%s;base64,%s", mimeType, encoded);
			DevConsole.get().setClipboardContents(url);
			return url;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static String stringToBase64(String string) {
		String encoded = Base64.getEncoder().encodeToString(string.getBytes());
		DevConsole.get().setClipboardContents(encoded);
		return string;
	}

	public static String tsvToHtmlTable(String tsv) {
		Csv csv = Csv.parseTsv(tsv);
		DomDocument doc = DomDocument.basicHtmlDoc();
		DomNodeHtmlTableBuilder tableBuilder = doc.html().body().html()
				.tableBuilder();
		DomNodeHtmlTableRowBuilder headerBuilder = tableBuilder.row();
		csv.headers().forEach(headerBuilder::cell);
		csv.forEach(row -> {
			DomNodeHtmlTableRowBuilder rowBuilder = tableBuilder.row();
			row.values().forEach(rowBuilder::cell);
		});
		String tableMarkup = tableBuilder.domNode().prettyToString();
		DevConsole.get().setClipboardContents(tableMarkup);
		return tableMarkup;
	}
}
