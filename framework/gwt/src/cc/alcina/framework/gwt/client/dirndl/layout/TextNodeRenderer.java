package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.Annotations;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableTypeFactory;

public class TextNodeRenderer extends LeafNodeRenderer {
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.TYPE)
	@ClientVisible
	public static @interface FieldNamesAsTags {
	}

	@Override
	public Widget render(Node node) {
		Widget rendered = super.render(node);
		rendered.getElement().setInnerText(getText(node));
		return rendered;
	}

	protected String getModelText(Object model) {
		return model.toString();
	}

	@Override
	protected String getTag(Node node) {
		if (node.parent != null && node.parent.has(FieldNamesAsTags.class)
				&& node.property.getName() != null) {
			return node.property.getName();
		}
		return Ax.blankTo(super.getTag(node), "span");
	}

	protected String getText(Node node) {
		return node.model == null ? "<null text>" : getModelText(node.model);
	}
	/*
	 * Normally entities, if directly rendered, are the models for actions - so
	 * just some simple text...
	 */

	@Registration({ DirectedNodeRenderer.class, Entity.class })
	public static class EntityNodeRenderer extends TextNodeRenderer {
		@Override
		protected String getModelText(Object model) {
			return "";
		}
	}

	@Registration({ DirectedNodeRenderer.class, Date.class })
	public static class DateNodeRenderer extends TextNodeRenderer {
		@Override
		protected String getModelText(Object model) {
			return Ax.dateTimeSlash((Date) model);
		}
	}

	@Registration({ DirectedNodeRenderer.class, Enum.class })
	public static class EnumNodeRenderer extends HasDisplayNameRenderer {
	}

	public static class HasDisplayNameRenderer extends TextNodeRenderer {
		@Override
		protected String getModelText(Object model) {
			if (model instanceof HasDisplayName) {
				return ((HasDisplayName) model).displayName();
			} else {
				return super.getModelText(model);
			}
		}
	}

	@Registration({ DirectedNodeRenderer.class, String.class })
	public static class StringNodeRenderer extends TextNodeRenderer {
	}

	@Registration({ DirectedNodeRenderer.class, Number.class })
	public static class NumberNodeRenderer extends TextNodeRenderer {
	}

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

	public static class TableHeaders extends TextNodeRenderer.StringListModel {
		public TableHeaders() {
			super();
		}

		public TableHeaders(List<String> strings) {
			super(strings);
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
}
