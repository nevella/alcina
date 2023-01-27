package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class LeafModel {
	@Directed(
		tag = "div",
		bindings = @Binding(from = "html", type = Type.INNER_HTML))
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
	public static class TagClassModel extends Model implements HasTag {
		private String className;

		private String tag;

		public TagClassModel() {
		}

		public TagClassModel(String tag, String className) {
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

	@Directed(bindings = @Binding(from = "text", type = Type.INNER_TEXT))
	public static class TagTextModel extends Model implements HasTag {
		private final String text;

		private final String tag;

		public TagTextModel(String tag, String text) {
			this.tag = tag;
			this.text = text;
		}

		public String getTag() {
			return this.tag;
		}

		public String getText() {
			return this.text;
		}

		@Override
		public String provideTag() {
			return getTag();
		}
	}
}
