package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.Annotations;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.RendererInput;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableTypeFactory;
import cc.alcina.framework.gwt.client.dirndl.widget.SimpleWidget;

/**
 * <p>
 * Renderers that emit no new inputs, only a widget
 *
 * <p>
 * A quick note re the HtmlXxx classes:
 *
 * <ul>
 * <li>Use <code>@Directed(renderer=LeafRenderer.Html.class)</code> to render a
 * String property as an element containing html (by default a
 * <code>&lt;span&gt;</code> element)
 * <li>Use a property of type <code>LeafRenderer.HtmlBlock</code>, with a
 * default <code>@Directed</code> annotation to have a property whose value
 * property as an element containing html (by default a <code>&lt;div&gt;</code>
 * element)
 * <li>Use a transform of type <code>LeafRenderer.HtmlModel.Transform</code>,
 * with a default <code>@Directed</code> annotation to have a list of String
 * instances be rendered as a list of non-escaped html elements
 *
 * </ul>
 *
 * @author nick@alcina.cc
 *
 */
public abstract class LeafRenderer extends DirectedRenderer {
	// for dirndl, mostly
	public static final Object OBJECT_INSTANCE = new Object();

	@Override
	protected void render(RendererInput input) {
		Node node = input.node;
		renderWidget(node);
		renderNode(node);
	}

	protected abstract void renderNode(Node node);

	protected void renderWidget(Node node) {
		String tag = getTag(node, "span");
		Preconditions.checkArgument(Ax.notBlank(tag));
		Widget widget = new SimpleWidget(tag);
		applyCssClass(node, widget);
		setWidget(node, widget);
	}

	protected void setWidget(Node node, Widget widget) {
		node.widget = widget;
	}

	@Registration({ DirectedRenderer.class, Boolean.class })
	public static class BooleanRenderer extends Text {
	}

	@Registration({ DirectedRenderer.class, Date.class })
	public static class DateRenderer extends Text {
		@Override
		protected String getModelText(Object model) {
			return Ax.dateTimeSlash((Date) model);
		}
	}

	@Registration({ DirectedRenderer.class, Entity.class })
	public static class EntityRenderer extends Text {
		@Override
		protected String getModelText(Object model) {
			return "";
		}
	}

	@Registration({ DirectedRenderer.class, Enum.class })
	public static class EnumRenderer extends HasDisplayNameRenderer {
	}

	public static class HasDisplayNameRenderer extends Text {
		@Override
		protected String getModelText(Object model) {
			if (model instanceof HasDisplayName) {
				return ((HasDisplayName) model).displayName();
			} else {
				return super.getModelText(model);
			}
		}
	}

	public static class Html extends LeafRenderer {
		protected String getText(Node node) {
			return node.model == null ? "" : node.model.toString();
		}

		@Override
		protected void renderNode(Node node) {
			node.widget.getElement().setInnerHTML(getText(node));
		}
	}

	@Directed(tag = "div", bindings = @Binding(from = "html", type = Type.INNER_HTML))
	public static class HtmlBlock extends Model {
		private String html;

		public HtmlBlock() {
		}

		public HtmlBlock(String html) {
			setHtml(html);
		}

		public HtmlBlock(String template, Object... args) {
			this(Ax.format(template, args));
		}

		public String getHtml() {
			return this.html;
		}

		public void setHtml(String html) {
			this.html = html;
		}
	}

	@Directed(bindings = @Binding(from = "html", type = Type.INNER_HTML))
	public static class HtmlModel extends Model {
		private String html;

		public HtmlModel() {
		}

		public HtmlModel(String html) {
			setHtml(html);
		}

		public String getHtml() {
			return this.html;
		}

		public void setHtml(String html) {
			this.html = html;
		}

		@Reflected
		public static class Transform
				implements ModelTransform<String, HtmlModel> {
			@Override
			public HtmlModel apply(String t) {
				return new HtmlModel(t);
			}
		}
	}

	@Directed(bindings = @Binding(from = "string", type = Type.INNER_HTML))
	public static class HtmlTagModel extends Model implements HasTag {
		private String string;

		private String tag;

		public HtmlTagModel() {
		}

		public HtmlTagModel(String string, String tag) {
			this.string = string;
			this.tag = tag;
		}

		@Directed
		public String getString() {
			return this.string;
		}

		public String getTag() {
			return this.tag;
		}

		@Override
		public String provideTag() {
			return getTag();
		}

		public void setString(String string) {
			this.string = string;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}
	}

	@Directed(tag = "img", bindings = @Binding(from = "src", type = Type.PROPERTY))
	public static class Image extends Model {
		private String src;

		public Image() {
		}

		public Image(String src) {
			this.src = src;
		}

		public String getSrc() {
			return this.src;
		}

		public void setSrc(String src) {
			this.src = src;
		}
	}

	@Registration({ DirectedRenderer.class, Number.class })
	public static class NumberNodeRenderer extends Text {
	}

	public static class SafeHtml extends LeafRenderer {
		@Override
		protected String getTag(Node node, String defaultTag) {
			return super.getTag(node, "span");
		}

		@Override
		protected void renderNode(Node node) {
			Element element = node.widget.getElement();
			element.setInnerSafeHtml(
					(com.google.gwt.safehtml.shared.SafeHtml) node.model);
			if (element.hasTagName("a")) {
				element.setAttribute("href", "#");
			}
		}
	}

	@Directed
	public static class StringListModel extends Model {
		private List<String> strings;

		public StringListModel() {
		}

		public StringListModel(List<String> strings) {
			this.strings = strings;
		}

		@Directed
		public List<String> getString() {
			return this.strings;
		}

		public void setList(List<String> strings) {
			this.strings = strings;
		}
	}

	@Directed(tag = "div")
	public static class StringModel extends Model {
		private String string;

		public StringModel() {
		}

		public StringModel(String string) {
			this.string = string;
		}

		@Directed
		public String getString() {
			return this.string;
		}

		public void setString(String string) {
			this.string = string;
		}
	}

	@Registration({ DirectedRenderer.class, String.class })
	public static class StringNodeRenderer extends Text {
	}

	@Directed(bindings = @Binding(from = "string", type = Type.INNER_TEXT))
	public static class StringTagModel extends Model implements HasTag {
		private String string;

		private String tag;

		public StringTagModel() {
		}

		public StringTagModel(String string, String tag) {
			this.string = string;
			this.tag = tag;
		}

		public String getString() {
			return this.string;
		}

		public String getTag() {
			return this.tag;
		}

		@Override
		public String provideTag() {
			return getTag();
		}

		public void setString(String string) {
			this.string = string;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}
	}

	public static class TableHeaders extends StringListModel {
		public TableHeaders() {
			super();
		}

		public TableHeaders(Class<? extends Bindable> clazz,
				DirectedLayout.Node node) {
			BoundWidgetTypeFactory factory = Registry
					.impl(TableTypeFactory.class);
			List<String> strings = Reflections.at(clazz).properties().stream()
					.map(p -> Annotations.resolve(p, Directed.Property.class,
							node.getResolver()))
					.filter(Objects::nonNull).map(Directed.Property::name)
					.collect(Collectors.toList());
			setList(strings);
		}

		public TableHeaders(List<String> strings) {
			super(strings);
		}
	}

	public static class Text extends LeafRenderer {
		protected String getModelText(Object model) {
			return model.toString();
		}

		protected String getText(Node node) {
			return node.model == null ? "<null text>"
					: getModelText(node.model);
		}

		@Override
		protected void renderNode(Node node) {
			node.widget.getElement().setInnerText(getText(node));
		}
	}
	/*
	 * Normally entities, if directly rendered, are the models for actions - so
	 * just some simple text...
	 */

	@Directed(tag = "div", bindings = @Binding(from = "text", type = Type.INNER_TEXT))
	public static class TextString extends Model {
		private String text;

		public TextString() {
		}

		public TextString(String text) {
			setText(text);
		}

		public String getText() {
			return this.text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}