package cc.alcina.framework.servlet.servlet.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.cache.Domain;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.UserStory;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCacheQuery;
import cc.alcina.framework.entity.parser.structured.node.XmlDoc;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;
import cc.alcina.framework.entity.parser.structured.node.XmlNodeHtmlTableBuilder;
import cc.alcina.framework.entity.parser.structured.node.XmlNodeHtmlTableBuilder.XmlNodeHtmlTableRowBuilder;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.SessionHelper;
import cc.alcina.framework.servlet.Sx;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

public class UserStories {
	public void persist(UserStory incoming) {
		ClientInstance clientInstance = SessionHelper
				.getAuthenticatedSessionClientInstance(
						CommonRemoteServiceServlet
								.getContextThreadLocalRequest());
		Optional<? extends UserStory> o_story = getUserStory(clientInstance,
				incoming.getClientInstanceUid());
		UserStory story = null;
		if (o_story.isPresent()) {
			story = AlcinaMemCache.get().find(o_story.get());
			TransformManager.get().registerDomainObject(story);
		} else {
			story = TransformManager.get()
					.createDomainObject(getImplementation());
		}
		String delta = getDelta(incoming, story);
		ResourceUtilities.copyBeanProperties(incoming, story, null, false,
				Domain.domainBaseVersionablePropertyNames);
		story.setClientInstanceId(clientInstance.getId());
		long creationId = ServletLayerUtils
				.pushTransformsAndGetFirstCreationId(true);
		long storyId = creationId == 0 ? story.getId() : creationId;
		Ax.out("published user story - %s:\n%s", storyId, delta);
		build(storyId, delta);
		topicUserStoriesEvents().publish(storyNode);
	}

	public static final String TOPIC_USER_STORIES_EVENT_OCCURRED = UserStories.class
			.getName() + "." + "TOPIC_USER_STORIES_EVENT_OCCURRED";

	public static TopicSupport<ObjectNode> topicUserStoriesEvents() {
		return new TopicSupport<>(TOPIC_USER_STORIES_EVENT_OCCURRED);
	}

	private String getDelta(UserStory incoming, UserStory story) {
		String s1 = incoming.getStory();
		String s2 = story.getStory();
		List<String> lines1 = Arrays
				.asList(Ax.blankToEmpty(incoming.getStory()).split("\n"));
		List<String> lines2 = Arrays
				.asList(Ax.blankToEmpty(story.getStory()).split("\n"));
		return lines1.subList(lines2.size(), lines1.size()).stream()
				.collect(Collectors.joining("\n"));
	}

	private Class<? extends UserStory> getImplementation() {
		return CommonPersistenceProvider.get()
				.getCommonPersistenceExTransaction()
				.getImplementation(UserStory.class);
	}

	private Optional<? extends UserStory> getUserStory(
			ClientInstance clientInstance, String clientInstanceUid) {
		Predicate<UserStory> predicate = us -> clientInstanceUid != null
				? clientInstanceUid.equals(us.getClientInstanceUid())
				: us.getClientInstanceId() == clientInstance.getId();
		return new AlcinaMemCacheQuery().filter(predicate)
				.list(getImplementation()).stream().findFirst();
	}

	private String html;

	private ObjectNode storyNode;

	public String asHtml(long id) {
		if (!PermissionsManager.get().isAdmin() && !Sx.isTest()) {
			throw new RuntimeException("Not permitted");
		}
		build(id, null);
		return html;
	}

	public void build(long id, String delta) {
		JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
		storyNode = nodeFactory.objectNode();
		UserStory userStory = AlcinaMemCache.get().find(getImplementation(),
				id);
		ClientInstance clientInstance = null;
		long clientInstanceId = userStory.getClientInstanceId();
		if (clientInstanceId != 0) {
			clientInstance = CommonPersistenceProvider.get()
					.getCommonPersistence()
					.getClientInstance(String.valueOf(clientInstanceId));
		} else {
			try {
				clientInstance = CommonPersistenceProvider.get()
						.getCommonPersistenceExTransaction()
						.getImplementation(ClientInstance.class).newInstance();
				clientInstance.setUserAgent(userStory.getUserAgent());
				clientInstance.setHelloDate(userStory.getDate() == null
						? new Date() : userStory.getDate());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		XmlDoc doc = XmlDoc.basicHtmlDoc();
		String css = ResourceUtilities.readClassPathResourceAsString(
				UserStories.class, "user-stories.css");
		doc.xpath("//head").node().builder().tag("style").text(css).append();
		XmlNode body = doc.xpath("//body").node();
		body.builder().tag("h2").text("User Story").append();
		String idNameString = clientInstance.getUser() == null
				? userStory.getEmail()
				: clientInstance.getUser().toIdNameString();
		String location = userStory.getLocation();
		if (Ax.notBlank(location)) {
			location = Ax.format("%s :: %s", location, GeolocationResolver.get().getLocation(location));
		}
		{
			XmlNodeHtmlTableBuilder builder = body.html().tableBuilder();
			builder.row().cell("User").cell(idNameString);
			builder.row().cell("Client instance")
					.cell(clientInstanceId == 0 ? "---" : clientInstanceId);
			builder.row().cell("Date").cell(clientInstance.getHelloDate());
			builder.row().cell("Referrer").cell(userStory.getHttpReferrer());
			builder.row().cell("Location").cell(location);
			builder.row().cell("User agent")
					.cell(clientInstance.getUserAgent());
			builder.row().cell("Triggering paywall event")
					.cell(userStory.getTrigger());
			XmlNodeHtmlTableRowBuilder row = builder.row();
			row.cell("\u00a0");
			row.cell()
					.text("Note, below the 'near' field refers to text either of the element clicked or the closest subsequent text")
					.style("font-size: 85%; font-style: italic; font-color: #555;")
					.cell();
			builder.append();
		}
		{
			storyNode.set("user", storyNode.textNode(idNameString));
			storyNode.set("clientInstance",
					storyNode.numberNode(clientInstanceId));
			storyNode.set("date", storyNode
					.textNode(clientInstance.getHelloDate().toString()));
			storyNode.set("date", storyNode
					.textNode(clientInstance.getHelloDate().toString()));
			storyNode.set("userAgent",
					storyNode.textNode(clientInstance.getUserAgent()));
			storyNode.set("triggeringPaywallEvent",
					storyNode.textNode(userStory.getTrigger()));
		}
		body.builder().tag("hr").append();
		{
			XmlNodeHtmlTableBuilder builder = body.html().tableBuilder();
			builder.row().style("font-weight:bold").cell("Time").cell("Type")
					.cell("Details");
			String story = delta != null ? delta : userStory.getStory();
			ArrayNode details = storyNode.arrayNode();
			storyNode.set("details", details);
			List<ClientLogRecord> list = new ArrayList<>();
			for (String line : story.split("\\n")) {
				if(line.isEmpty()) {
					continue;
				}
				Object deser = AlcinaBeanSerializer.deserializeHolder(line);
				if (deser instanceof List) {
					list.addAll((List) deser);
				} else {
					ClientLogRecords records = (ClientLogRecords) deser;
					list.addAll(records.getLogRecords());
				}
			}
			int ctr = list.size();
			for (ClientLogRecord record : list) {
				ctr--;
				String messageTxt = Ax.nullSafe(record.getMessage());
				switch (Ax.nullSafe(record.getTopic()).toLowerCase()) {
				case "message":
					if (messageTxt.matches("Started logging.+")) {
						break;
					}
				case "restart":
					continue;
				case "history":
					if (messageTxt.equals("window closing") && ctr >= 10) {
						continue;
					}
					break;
				}
				XmlNodeHtmlTableRowBuilder row = builder.row();
				String timestamp = CommonUtils.formatDate(record.getTime(),
						DateStyle.TIMESTAMP_NO_DAY);
				row.cell().text(timestamp).nowrap().cell();
				String topic = Ax.friendly(record.getTopic());
				row.cell(topic);
				XmlNode td = row.getNode().builder().tag("td").append();
				Message message = parseMessage(record);
				if (message.path != null) {
					td.builder().tag("div")
							.text(Ax.format("Path: %s", message.path)).append();
					td.builder().tag("div")
							.text(Ax.format("%s: %s",
									message.textIsLocator ? "Near" : "Text",
									message.text))
							.append();
				} else {
					td.builder().tag("div").text(message.text).append();
				}
				ObjectNode detail = details.objectNode();
				detail.set("timestamp", storyNode.textNode(timestamp));
				detail.set("topic", storyNode.textNode(topic));
				if (message.path != null) {
					detail.set("path", storyNode.textNode(message.path));
					detail.set(message.textIsLocator ? "near" : "text",
							storyNode.textNode(message.text));
				} else {
					detail.set("value", storyNode.textNode(message.text));
				}
				details.add(detail);
			}
			builder.append();
			if (userStory.getCart() != null) {
				body.builder().tag("hr").append();
				body.builder().tag("h3").text("Cart...").append();
				ObjectMapper mapper = new ObjectMapper();
				try {
					ObjectNode node = (ObjectNode) mapper
							.readTree(userStory.getCart());
					((ArrayNode) node.get("items")).forEach(
							n -> ((ObjectNode) n).remove("additional"));
					String pretty = mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(node);
					body.builder().tag("pre").text(pretty).append();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
		html = doc.prettyToString();
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
