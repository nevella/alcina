package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.Objects;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.logic.domain.HasStringValue;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.AttributeMappingType;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.Mapping;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.TextContainer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;

public abstract interface Inline {
	static final String _JDPT = "_jdpt";

	public static abstract class Base extends FragmentDocumentNode
			implements Inline, HasPutText {
	}

	// maps a text node. Note the directed tag
	@Mapping(
		directed = @Directed(
			tag = DomNodeType.NodeNames.TEXT,
			renderer = LeafRenderer.TextNode.class))
	public static class Text extends Base
			implements TextContainer, FragmentNode.TextLeaf {
		public static Predicate<FragmentNode> isWhitespaceOrEmpty() {
			return fn -> {
				if (fn instanceof Text) {
					return TextUtils
							.isWhitespaceOrEmpty(((Text) fn).getValue());
				} else {
					return false;
				}
			};
		}

		private String value = "";

		public Text() {
		}

		public Text(String value) {
			// the value will be eventually set to a non-null, but on init it's
			// null - elide the issue
			setValue(Ax.blankToEmpty(value));
		}

		@Override
		public void copyFromExternal(FragmentNode external) {
			value = ((Text) external).value;
		}

		@Binding(type = Type.INNER_TEXT)
		public String getValue() {
			if (provideIsBound()) {
				this.value = provideNode().getRendered().getNode()
						.getNodeValue();
			}
			return this.value;
		}

		@Override
		public void onBind(Bind event) {
			if (!event.isBound()) {
				this.value = provideNode().getRendered().getNode()
						.getNodeValue();
			}
			super.onBind(event);
		}

		public void setValue(String value) {
			Objects.requireNonNull(value);
			set("value", this.value, value, () -> this.value = value);
		}

		@Override
		public String toString() {
			return Ax.format("#TEXT :: %s", getValue());
		}
	}

	// maps to a processing instruction. Note the directed tag
	@Mapping(
		directed = @Directed(
			tag = "pgbrk",
			renderer = LeafRenderer.ProcessingInstructionNode.class))
	public static class Pagebreak extends Base implements HasStringValue {
		private String stringValue;

		public Pagebreak() {
		}

		public Pagebreak(String stringValue) {
			this.stringValue = stringValue;
		}

		@Binding(type = Type.INNER_TEXT)
		public String getStringValue() {
			return stringValue;
		}

		public void setStringValue(String stringValue) {
			set("stringValue", this.stringValue, stringValue,
					() -> this.stringValue = stringValue);
		}

		public boolean provideIsFirstPage() {
			return Objects.equals(stringValue, "Page 1");
		}
	}

	/*
	 * Container should not be removed, even if text content is whitespace
	 */
	public interface NonTextContent {
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "img"))
	public static class Image extends Base implements NonTextContent {
		private String src;

		private String alt;

		private int width;

		private int height;

		public Image() {
		}

		public Image(String src, String alt, int width, int height) {
			this.src = src;
			this.alt = alt;
			this.width = width;
			this.height = height;
		}

		@Binding(type = Type.PROPERTY)
		public int getWidth() {
			return width;
		}

		@Binding(type = Type.PROPERTY)
		public void setWidth(int width) {
			set("width", this.width, width, () -> this.width = width);
		}

		@Binding(type = Type.PROPERTY)
		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			set("height", this.height, height, () -> this.height = height);
		}

		@Binding(type = Type.PROPERTY)
		public String getAlt() {
			return alt;
		}

		public void setAlt(String alt) {
			set("alt", this.alt, alt, () -> this.alt = alt);
		}

		@Binding(type = Type.PROPERTY)
		public String getSrc() {
			return src;
		}

		public void setSrc(String src) {
			set("src", this.src, src, () -> this.src = src);
		}
	}

	/**
	 * Passthrough links (the citation parser will ignore + parse around them)
	 */
	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(
			tag = "a",
			bindings = @Binding(
				type = Type.PROPERTY,
				to = _JDPT,
				literal = "true")))
	public static class PassthroughLink extends Base {
		private String href;

		public PassthroughLink() {
		}

		public PassthroughLink(
				LayoutTokens.PassthroughLink.HasLinkAttributes attr) {
			href = attr.getHref();
		}

		@Binding(type = Type.PROPERTY)
		public String getHref() {
			return href;
		}

		public void setHref(String href) {
			this.href = href;
		}

		public static boolean is(DomNode domNode) {
			return domNode.tagIs("a") && domNode.has(_JDPT);
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "inline"))
	@AttributeMappingType.Ref(AttributeMappingType.SINGLE)
	public static class Styled extends Base {
		private String className;

		private String styleAttribute;

		@Binding(type = Type.PROPERTY, to = "style")
		public String getStyleAttribute() {
			return styleAttribute;
		}

		public void setStyleAttribute(String styleAttribute) {
			this.styleAttribute = styleAttribute;
		}

		@Override
		public void copyFromExternal(FragmentNode external) {
			className = ((Styled) external).className;
			styleAttribute = ((Styled) external).styleAttribute;
		}

		@Binding(type = Type.CLASS_PROPERTY)
		public String getClassName() {
			return this.className;
		}

		public void setClassName(String className) {
			this.className = className;
		}
	}
}
