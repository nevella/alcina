package cc.alcina.framework.servlet.publication;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.dom.DomDocument;
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

	private List<StylePath> stylePaths = new ArrayList<>();

	private Model renderable;

	public DirndlRenderer addStyleFile(Class<?> styleRelativeClass,
			String styleRelativeFilename) {
		stylePaths
				.add(new StylePath(styleRelativeClass, styleRelativeFilename));
		return this;
	}

	public DomDocument render() {
		Widget widget = new DirectedLayout().render(new ContextResolver(), 
				renderable);
		String outerHtml = widget.getElement().getOuterHtml();
		DomDocument doc = DomDocument.basicHtmlDoc();
		DomNode div = doc.html().body().builder().tag("div").append();
		div.setInnerXml(outerHtml);
		stylePaths.forEach(p -> {
			String style = ResourceUtilities.read(p.styleRelativeClass,
					p.styleRelativeFilename);
			if (Ax.notBlank(style)) {
				doc.html().appendStyleNode(style);
			}
		});
		return doc;
	}

	public DirndlRenderer withRenderable(Model renderable) {
		this.renderable = renderable;
		return this;
	}

	static class StylePath {
		Class<?> styleRelativeClass;

		String styleRelativeFilename;

		public StylePath(Class<?> styleRelativeClass,
				String styleRelativeFilename) {
			this.styleRelativeClass = styleRelativeClass;
			this.styleRelativeFilename = styleRelativeFilename;
		}
	}
}
