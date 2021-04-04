package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class HtmlNodeRenderer extends LeafNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return Ax.blankTo(super.getTag(node), "div");
	}

	@Override
	public Widget render(Node node) {
		Widget rendered = super.render(node);
		rendered.getElement().setInnerHTML(getText(node));
		return rendered;
	}

	protected String getText(Node node) {
		return node.model == null ? "" : node.model.toString();
	}

	@Directed(tag = "div", bindings = @Binding(from = "html", type = Type.INNER_HTML))
	public static class HtmlString extends Model {
		private String html;

		public HtmlString() {
		}

		public HtmlString(String html) {
			setHtml(html);
		}

		public String getHtml() {
			return this.html;
		}

		public void setHtml(String html) {
			this.html = html;
		}
	}
}
