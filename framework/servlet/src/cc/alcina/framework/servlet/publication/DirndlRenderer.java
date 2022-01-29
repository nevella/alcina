package cc.alcina.framework.servlet.publication;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class DirndlRenderer {
	public static DirndlRenderer instance() {
		return new DirndlRenderer();
	}

	private Class styleRelativeClass;

	private String styleRelativeFilename;

	private Model renderable;

	public DirndlRenderer withRenderable(Model renderable) {
		this.renderable = renderable;
		return this;
	}

	public DirndlRenderer withStyleFile(Class<?> styleRelativeClass,
			String styleRelativeFilename) {
		this.styleRelativeClass = styleRelativeClass;
		this.styleRelativeFilename = styleRelativeFilename;
		return this;
	}

	public DomDoc render() {
		Widget widget = new DirectedLayout().render(new ContextResolver(), null,
				renderable);
		String outerHtml = widget.getElement().getOuterHtml();
		DomDoc doc = DomDoc.basicHtmlDoc();
		DomNode div = doc.html().body().builder().tag("div").append();
		div.setInnerXml(outerHtml);
		if (styleRelativeClass != null) {
			String style = ResourceUtilities.read(styleRelativeClass,
					styleRelativeFilename);
			if (Ax.notBlank(style)) {
				doc.html().appendStyleNode(style);
			}
		}
		return doc;
	}
}
