package cc.alcina.framework.servlet.servlet.publication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.parser.structured.node.XmlDoc;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;
import cc.alcina.framework.entity.parser.structured.node.XmlNodeHtmlTableBuilder;
import cc.alcina.framework.servlet.Sx;
import cc.alcina.framework.servlet.publication.ContentRenderer.ContentRendererResults;
import cc.alcina.framework.servlet.publication.Publisher.PublicationContentPersister;

public class PublicationViews {
	private String html;

	public String asHtml(long id) {
		if (!PermissionsManager.get().isAdmin() && !Sx.isTest()) {
			throw new RuntimeException("Not permitted");
		}
		build(id, null);
		return html;
	}

	public void build(long id, String delta) {
		Publication publication = CommonPersistenceProvider.get()
				.getCommonPersistence().getPublication(id);
		XmlDoc doc = XmlDoc.basicHtmlDoc();
		String css = ResourceUtilities.readClassPathResourceAsString(
				PublicationViews.class, "publication-view.css");
		doc.xpath("//head").node().builder().tag("style").text(css).append();
		XmlNode body = doc.xpath("//body").node();
		body.builder().tag("h2").text("Publication view").append();
		{
			XmlNodeHtmlTableBuilder builder = body.html().tableBuilder();
			builder.row().cell("Id").cell(id);
			builder.row().cell("User")
					.cell(publication.getUser().toIdNameString());
			builder.row().cell("Date").cell(publication.getPublicationDate());
			builder.row().cell("Type").cell(publication.getPublicationType());
			DeliveryModel deliveryModel = publication.getDeliveryModel();
			builder.row().cell("Subject").cell(deliveryModel.getEmailSubject());
			builder.row().cell("To").cell(deliveryModel.getEmailAddress());
			builder.append();
		}
		body.builder().tag("hr").append();
		XmlNode content = body.builder().tag("iframe").append().setAttr("id",
				"content-frame");
		content.html().addClassName("content");
		PublicationContentPersister publicationContentPersister = Registry
				.impl(PublicationContentPersister.class);
		XmlNode script = body.builder().tag("script").append();
		ContentRendererResults crr = publicationContentPersister
				.getContentRendererResults(id);
		String nodeHtml = StringEscapeUtils.escapeJavaScript(crr.htmlContent);
		FormatBuilder fb = new FormatBuilder();
		fb.line("var content=\"%s\";", nodeHtml);
		fb.line("document.getElementById('content-frame').contentDocument.documentElement.innerHTML= content;");
		script.setText(fb.toString());
		// {
		// XmlNodeHtmlTableBuilder builder = body.html().tableBuilder();
		// builder.row().style("font-weight:bold").cell("Time").cell("Type")
		// .cell("Details");
		// String story = delta != null ? delta : userStory.getStory();
		// ArrayNode details = storyNode.arrayNode();
		// storyNode.set("details", details);
		// List<ClientLogRecord> list = new ArrayList<>();
		// for (String line : story.split("\\n")) {
		// Object deser = AlcinaBeanSerializer.deserializeHolder(line);
		// if (deser instanceof List) {
		// list.addAll((List) deser);
		// } else {
		// ClientLogRecords records = (ClientLogRecords) deser;
		// list.addAll(records.getLogRecords());
		// }
		// }
		// int ctr = list.size();
		// for (ClientLogRecord record : list) {
		// ctr--;
		// String messageTxt = Ax.nullSafe(record.getMessage());
		// switch (Ax.nullSafe(record.getTopic()).toLowerCase()) {
		// case "message":
		// if (messageTxt.matches("Started logging.+")) {
		// break;
		// }
		// case "restart":
		// continue;
		// case "history":
		// if (messageTxt.equals("window closing") && ctr >= 10) {
		// continue;
		// }
		// break;
		// }
		// XmlNodeHtmlTableRowBuilder row = builder.row();
		// String timestamp = CommonUtils.formatDate(record.getTime(),
		// DateStyle.TIMESTAMP_NO_DAY);
		// row.cell().text(timestamp).nowrap().cell();
		// String topic = Ax.friendly(record.getTopic());
		// row.cell(topic);
		// XmlNode td = row.getNode().builder().tag("td").append();
		// Message message = parseMessage(record);
		// if (message.path != null) {
		// td.builder().tag("div")
		// .text(Ax.format("Path: %s", message.path)).append();
		// td.builder().tag("div")
		// .text(Ax.format("%s: %s",
		// message.textIsLocator ? "Near" : "Text",
		// message.text))
		// .append();
		// } else {
		// td.builder().tag("div").text(message.text).append();
		// }
		// ObjectNode detail = details.objectNode();
		// detail.set("timestamp", storyNode.textNode(timestamp));
		// detail.set("topic", storyNode.textNode(topic));
		// if (message.path != null) {
		// detail.set("path", storyNode.textNode(message.path));
		// detail.set(message.textIsLocator ? "near" : "text",
		// storyNode.textNode(message.text));
		// } else {
		// detail.set("value", storyNode.textNode(message.text));
		// }
		// details.add(detail);
		// }
		// builder.append();
		// if (userStory.getCart() != null) {
		// body.builder().tag("hr").append();
		// body.builder().tag("h3").text("Cart...").append();
		// ObjectMapper mapper = new ObjectMapper();
		// try {
		// ObjectNode node = (ObjectNode) mapper
		// .readTree(userStory.getCart());
		// ((ArrayNode) node.get("items")).forEach(
		// n -> ((ObjectNode) n).remove("additional"));
		// String pretty = mapper.writerWithDefaultPrettyPrinter()
		// .writeValueAsString(node);
		// body.builder().tag("pre").text(pretty).append();
		// } catch (Exception e) {
		// throw new WrappedRuntimeException(e);
		// }
		// }
		// }
		html = XmlUtils.streamNCleanForBrowserHtmlFragment(
				doc.getDocumentElementNode().domNode());
	}

	static class Message {
		String path;

		String text;

		public boolean textIsLocator;
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
}
