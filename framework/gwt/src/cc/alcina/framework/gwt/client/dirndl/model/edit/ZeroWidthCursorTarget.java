package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;

@Directed(tag = "span", className = "cursor-target")
public class ZeroWidthCursorTarget extends FragmentNode {
	public static final String ZWS_CONTENT = "\u200B";

	public static boolean is(String text) {
		return Objects.equals(text, ZWS_CONTENT);
	}

	public ZeroWidthCursorTarget() {
		// throw new UnsupportedOperationException();
	}

	public static boolean isOneOrMore(String text) {
		return text.matches("\u200B+");
	}

	@Override
	public void onFragmentRegistration() {
		nodes().append(new TextNode(ZWS_CONTENT));
	}

	@Property.Not
	public TextNode getSoleTextNode() {
		if (provideChildNodes().size() != 1) {
			return null;
		}
		FragmentNode child = children().findFirst().get();
		return child instanceof TextNode ? (TextNode) child : null;
	}

	boolean unwrapIfContainsNonZwsText() {
		TextNode soleTextNode = getSoleTextNode();
		if (soleTextNode == null
				|| !Objects.equals(soleTextNode.liveValue(), ZWS_CONTENT)) {
			List<TextNode> descendantTexts = (List) byType(TextNode.class)
					.collect(Collectors.toList());
			descendantTexts.forEach(text -> {
				String nodeValue = text.liveValue();
				String replaceValue = nodeValue.replace(ZWS_CONTENT, "");
				if (!Objects.equals(nodeValue, replaceValue) &&
				// localdom doesn't like 0-length text nodes
						replaceValue.length() > 0) {
					// this may move the selection cursor! so requires more
					// bubbling/event chaining, non-deferred
					text.setValue(replaceValue);
					// this is the non-bubbling, quick hack - FIXME FN
					Document.get().getSelection().validate();
					text.domNode().asLocation().getLocationContext()
							.invalidate();
				}
			});
			nodes().strip();
			return true;
		} else {
			return false;
		}
	}

	/* TODO - boundary-req */
	void removeIfNotRequired() {
		if (unwrapIfContainsNonZwsText()) {
			return;
		}
		boolean required = HasContentEditable
				.isUneditable(nodes().previousSibling())
				&& HasContentEditable.isUneditable(nodes().nextSibling());
		if (!required) {
			nodes().removeFromParent();
			// this is the non-bubbling, quick hack - FIXME FN
			Document.get().getSelection().validate();
		}
	}
}