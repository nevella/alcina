package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Date;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.gwittir.validator.ShortIso8601DateValidator;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer.HasDisplayNameRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

// LeafModel itself is just a naming container
public abstract class LeafModel {
	@Directed(tag = "div")
	public static class HtmlBlock extends Model implements HasValue<String> {
		public String value;

		public HtmlBlock() {
		}

		public HtmlBlock(String html) {
			setValue(html);
		}

		public HtmlBlock(String template, Object... args) {
			this(Ax.format(template, args));
		}

		@Override
		@Binding(type = Type.INNER_HTML)
		public String getValue() {
			return value;
		}

		@Override
		public void setValue(String value) {
			set("value", this.value, value, () -> this.value = value);
		}

		public static class To implements ModelTransform<String, HtmlBlock> {
			@Override
			public HtmlBlock apply(String t) {
				return new HtmlBlock(t);
			}
		}
	}

	/*
	 * A null tag is permitted (see HasTag)
	 */
	@Directed
	public static class TagMarkup extends Model.Fields implements HasTag {
		@Binding(type = Type.INNER_HTML)
		public String markup;

		String tag;

		public TagMarkup() {
		}

		public TagMarkup(String tag, String markup) {
			Preconditions.checkArgument(
					tag == null || tag.matches("[a-zA-Z\\-0-9_]+"));
			this.tag = tag;
			this.markup = markup;
		}

		@Override
		public String provideTag() {
			return tag;
		}
	}

	@Directed(bindings = @Binding(from = "src", type = Type.PROPERTY))
	public static class Img extends Model {
		private String src;

		public Img() {
		}

		public Img(String src) {
			this.src = src;
		}

		public String getSrc() {
			return this.src;
		}

		public void setSrc(String src) {
			String old_src = this.src;
			this.src = src;
			propertyChangeSupport().firePropertyChange("src", old_src, src);
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

	@Directed(
		bindings = @Binding(from = "className", type = Type.CLASS_PROPERTY))
	public static class TagClass extends Model implements HasTag {
		private String className;

		private String tag;

		public TagClass() {
		}

		public TagClass(String tag, String className) {
			this.tag = tag;
			this.className = className;
		}

		public String getClassName() {
			return this.className;
		}

		public String getTag() {
			return this.tag;
		}

		@Override
		public String provideTag() {
			return getTag();
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}
	}

	/**
	 * An utterly simple dialog
	 */
	@Directed
	public static class PreText extends Model.All {
		Heading heading;

		Inner inner;

		class Inner extends Model.All {
			@Binding(type = Type.INNER_TEXT)
			final String text;

			Inner(String text) {
				this.text = text;
			}
		}

		public PreText(String caption, String text) {
			heading = new Heading(caption);
			this.inner = new Inner(text);
		}
	}

	@Directed
	public static class TagText extends Model.Fields implements HasTag {
		@Binding(type = Type.INNER_TEXT)
		public final String text;

		public final String tag;

		@Binding(type = Type.PROPERTY)
		public String title;

		public TagText(String tag, String text) {
			this(tag, text, null);
		}

		public TagText(String tag, String text, String title) {
			this.tag = tag;
			this.text = text;
			this.title = title;
		}

		@Override
		public String provideTag() {
			return tag;
		}
	}

	@Directed
	public static class TextTitle extends Model.Fields {
		@Binding(type = Type.INNER_TEXT)
		public final String text;

		@Binding(type = Type.PROPERTY)
		public String title;

		public TextTitle(String text) {
			this(text, text);
		}

		public TextTitle(String text, String title) {
			this.text = text;
			this.title = title;
		}

		public static class To implements ModelTransform<Object, TextTitle> {
			@Override
			public TextTitle apply(Object t) {
				String text = CommonUtils.nullSafeToString(t);
				return new TextTitle(text, text);
			}
		}
	}

	/*
	 * Simple fixed-width *without* sass/classnames - for use when the code
	 * can't easily manipulate the sass (sequence browser detail, e.g.)
	 */
	@Directed(tag = "div")
	@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
	public static class DivLabel extends Model.Fields {
		@Directed(
			tag = "span",
			bindings = @Binding(
				to = "style",
				type = Type.PROPERTY,
				literal = "display: inline-block; width: 15em"))
		public String label;

		@Directed(
			tag = "span",
			bindings = @Binding(
				to = "style",
				type = Type.PROPERTY,
				literal = "display: inline-block; width: 15em"))
		@Directed
		public Object model;

		public DivLabel(String label, Object model) {
			this.label = label;
			this.model = model;
		}
	}

	@Directed
	public static class Button extends Model.Fields {
		@Binding(type = Type.PROPERTY)
		public String title;

		@Binding(type = Type.CLASS_PROPERTY)
		public String className;

		@Binding(type = Type.PROPERTY)
		public String type = "button";

		@Binding(type = Type.INNER_TEXT)
		public String text;

		public Button() {
		}

		public static class To<T> implements ModelTransform<T, Button> {
			@Override
			public Button apply(T t) {
				Button button = new Button();
				button.text = new HasDisplayNameRenderer().getModelText(t);
				return button;
			}
		}
	}

	public static class EditableDateModel extends Bindable.Fields.All {
		public EditableDateModel() {
		}

		public EditableDateModel(Date date) {
			this.date = date;
		}

		@Display
		@Validator(ShortIso8601DateValidator.class)
		public Date date;
	}
}
