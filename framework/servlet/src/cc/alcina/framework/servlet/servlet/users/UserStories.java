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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableRowBuilder;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.IUserStory;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.servlet.Sx;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;

public class UserStories {
	public static final String TOPIC_USER_STORIES_EVENT_OCCURRED = UserStories.class
			.getName() + "." + "TOPIC_USER_STORIES_EVENT_OCCURRED";

	public static Topic<ObjectNode> topicUserStoriesEvents() {
		return Topic.global(TOPIC_USER_STORIES_EVENT_OCCURRED);
	}

	private String html;

	private ObjectNode storyNode;

	Logger logger = LoggerFactory.getLogger(getClass());

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
		IUserStory userStory = Domain.find(getImplementation(), id);
		ClientInstance clientInstance = null;
		long clientInstanceId = userStory.getClientInstanceId();
		if (clientInstanceId != 0) {
			clientInstance = AuthenticationPersistence.get()
					.getClientInstance(clientInstanceId);
		} else {
			try {
				clientInstance = AlcinaPersistentEntityImpl
						.getImplementation(ClientInstance.class).newInstance();
				clientInstance.setUserAgent(userStory.getUserAgent());
				clientInstance
						.setHelloDate(userStory.getDate() == null ? new Date()
								: userStory.getDate());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		DomDoc doc = DomDoc.basicHtmlDoc();
		String css = ResourceUtilities.readClassPathResourceAsString(
				UserStories.class, "user-stories.css");
		doc.xpath("//head").node().builder().tag("style").text(css).append();
		DomNode body = doc.xpath("//body").node();
		body.builder().tag("h2").text("User Story").append();
		String idNameString = clientInstance.provideUser() == null
				? userStory.getEmail()
				: clientInstance.provideUser().toIdNameString();
		String location = userStory.getLocation();
		if (Ax.notBlank(location)) {
			location = Ax.format("%s :: %s", location,
					GeolocationResolver.get().getLocation(location));
		}
		{
			DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
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
			DomNodeHtmlTableRowBuilder row = builder.row();
			row.cell("\u00a0");
			row.cell().text(
					"Note, below the 'near' field refers to text either of the element clicked or the closest subsequent text")
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
			DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
			builder.row().style("font-weight:bold").cell("Time").cell("Type")
					.cell("Details");
			String story = delta != null ? delta : userStory.getStory();
			ArrayNode details = storyNode.arrayNode();
			storyNode.set("details", details);
			List<ClientLogRecord> list = new ArrayList<>();
			for (String line : story.split("\\n")) {
				if (line.isEmpty()) {
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
				DomNodeHtmlTableRowBuilder row = builder.row();
				String timestamp = CommonUtils.formatDate(record.getTime(),
						DateStyle.TIMESTAMP_NO_DAY);
				row.cell().text(timestamp).nowrap().cell();
				String topic = Ax.friendly(record.getTopic());
				row.cell(topic);
				DomNode td = row.getNode().builder().tag("td").append();
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
					if (node.get("items") != null) {
						((ArrayNode) node.get("items")).forEach(
								n -> ((ObjectNode) n).remove("additional"));
					}
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

	public void persist(IUserStory incoming) {
		if (ResourceUtilities.is("disabled")) {
			return;
		}
		if (incoming.getStory().length() > 100000) {
			return;
		}
		ClientInstance clientInstance = AuthenticationManager.get()
				.getContextClientInstance().orElse(null);
		Optional<? extends IUserStory> o_story = getUserStory(clientInstance,
				incoming.getClientInstanceUid());
		IUserStory story = null;
		if (o_story.isPresent()) {
			story = (IUserStory) ((Entity) o_story.get());
		} else {
			story = Domain.create(getImplementation());
			postCreateStory(story, clientInstance);
		}
		String delta = getDelta(incoming, story);
		ResourceUtilities.copyBeanProperties(incoming, story, null, false,
				getUserStoryPropertiesNotPopulatedByClient());
		story.setClientInstanceId(clientInstance.getId());
		story.setIid(clientInstance.getIid());
		long creationId = TransformCommit
				.commitTransformsAndGetFirstCreationId(true);
		long storyId = creationId == 0 ? story.getId() : creationId;
		logger.info("published user story - {}", storyId);
		build(storyId, delta);
		topicUserStoriesEvents().publish(storyNode);
	}

	private String getDelta(IUserStory incoming, IUserStory story) {
		String s1 = incoming.getStory();
		String s2 = story.getStory();
		List<String> lines1 = Arrays
				.asList(Ax.blankToEmpty(incoming.getStory()).split("\n"));
		List<String> lines2 = Arrays
				.asList(Ax.blankToEmpty(story.getStory()).split("\n"));
		return lines1.subList(lines2.size(), lines1.size()).stream()
				.collect(Collectors.joining("\n"));
	}

	private Optional<? extends IUserStory> getUserStory(
			ClientInstance clientInstance, String clientInstanceUid) {
		return ThreadedPermissionsManager.cast()
				.callWithPushedSystemUserIfNeededNoThrow(() -> {
					Predicate<IUserStory> predicate = us -> clientInstanceUid != null
							? clientInstanceUid
									.equals(us.getClientInstanceUid())
							: us.getClientInstanceId() == clientInstance
									.getId();
					return Domain.query(getImplementation()).filter(predicate)
							.stream().findFirst();
				});
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

	protected <T extends Entity & IUserStory> Class<T> getImplementation() {
		return (Class<T>) AlcinaPersistentEntityImpl
				.getImplementation(IUserStory.class);
	}

	protected List<String> getUserStoryPropertiesNotPopulatedByClient() {
		List<String> properties = Domain.DOMAIN_BASE_VERSIONABLE_PROPERTY_NAMES
				.stream().collect(Collectors.toList());
		properties.add("date");
		properties.add("iid");
		return properties;
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

	protected void postCreateStory(IUserStory story,
			ClientInstance clientInstance) {
		story.setDate(new Date());
	}

	static class Message {
		String path;

		String text;

		public boolean textIsLocator;
	}
}
