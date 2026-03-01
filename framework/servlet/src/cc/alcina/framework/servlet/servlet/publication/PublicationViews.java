package cc.alcina.framework.servlet.servlet.publication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.servlet.publication.ContentRenderer.ContentRendererResults;
import cc.alcina.framework.servlet.publication.Publisher.PublicationPersister;

public class PublicationViews {
	private String html;

	public String asHtml(long id) {
		if (!Permissions.get().isAdmin() && !EntityLayerUtils.isTest()) {
			throw new RuntimeException("Not permitted");
		}
		build(id, null);
		return html;
	}

	public void build(long id, String delta) {
		Publication publication = PersistentImpl.find(Publication.class, id);
		DomDocument doc = DomDocument.basicHtmlDoc();
		String css = Io.read().resource("publication-view.css").asString();
		doc.xpath("//head").node().builder().tag("style").text(css).append();
		DomNode body = doc.xpath("//body").node();
		body.builder().tag("h2").text("Publication view").append();
		{
			DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
			builder.row().cell("Id").cell(id);
			builder.row().cell("User")
					.cell(publication.getUser().toIdNameString());
			builder.row().cell("Date").cell(publication.getPublicationDate());
			builder.row().cell("Type").cell(publication.getPublicationType());
			DeliveryModel deliveryModel = publication.provideDeliveryModel();
			builder.row().cell("Subject").cell(deliveryModel.getEmailSubject());
			builder.row().cell("To").cell(deliveryModel.getEmailAddress());
			builder.append();
		}
		body.builder().tag("hr").append();
		DomNode content = body.builder().tag("iframe").append().setAttr("id",
				"content-frame");
		content.style().addClassName("content");
		PublicationPersister publicationContentPersister = Registry
				.impl(PublicationPersister.class);
		DomNode script = body.builder().tag("script").append();
		ContentRendererResults crr = publicationContentPersister
				.getContentRendererResults(publication);
		String nodeHtml = crr.htmlContent;
		nodeHtml = nodeHtml.replaceFirst("(?si)<html>(.+)</html>.*", "$1");
		nodeHtml = StringEscapeUtils.escapeJavaScript(nodeHtml);
		FormatBuilder fb = new FormatBuilder();
		fb.line("var content=\"%s\";", nodeHtml);
		fb.line("document.getElementById('content-frame').contentDocument.documentElement.innerHTML= content;");
		html = XmlUtils.streamNCleanForBrowserHtmlFragment(
				doc.getDocumentElementNode().w3cNode());
		html = html.replace("<script>", "<script>" + fb.toString());
	}

	protected Message parseMessage(ClientLogRecord record) {
		Message out = new Message();
		String text = record.getMessage();
		{
			String regex = ".+(/.+?/.+) :: .+value :: (.*)";
			if (text.matches(regex)) {
				out.path = text.replaceFirst(regex, "$1");
				out.text = text.replaceFirst(regex, "$2");
				return out;
			}
		}
		{
			String regex = ".+(/.+?/.+) :: \\[(.+)\\].*";
			if (text.matches(regex)) {
				out.path = text.replaceFirst(regex, "$1");
				out.text = text.replaceFirst(regex, "$2");
				out.text = untilFirstCamel(out.text);
				out.textIsLocator = true;
				return out;
			}
		}
		out.text = text;
		return out;
	}

	private String untilFirstCamel(String text) {
		List<String> out = new ArrayList<>();
		List<String> words = Arrays.asList(text.split(" "));
		Pattern pattern = Pattern.compile("(\\w[a-z]+)[A-Z].*");
		for (String word : words) {
			Matcher matcher = pattern.matcher(word);
			if (matcher.matches()) {
				out.add(matcher.group(1));
				break;
			} else {
				out.add(word);
			}
		}
		return out.stream().collect(Collectors.joining(" "));
	}

	static class Message {
		String path;

		String text;

		public boolean textIsLocator;
	}
}
