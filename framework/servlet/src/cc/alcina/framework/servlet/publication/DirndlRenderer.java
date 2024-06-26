package cc.alcina.framework.servlet.publication;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class DirndlRenderer {
	public static DirndlRenderer instance() {
		return new DirndlRenderer();
	}

	private List<StylePath> stylePaths = new ArrayList<>();

	private Model renderable;

	private ContextResolver contextResolver;

	public DirndlRenderer addStyleFile(Class<?> styleRelativeClass,
			String styleRelativeFilename) {
		stylePaths
				.add(new StylePath(styleRelativeClass, styleRelativeFilename));
		return this;
	}

	/**
	 * Render, return an html doc containing the rendered element and any style
	 * nodes
	 */
	public DomDocument asDocument() {
		return render(this::renderDocument);
	}

	/**
	 * Render, just return the rendered element
	 */
	public DomNode asNode() {
		Preconditions.checkState(stylePaths.isEmpty());
		DomDocument document = asDocument();
		return document.html().body().children.firstElement().children
				.firstElement();
	}

	private <T> T render(Supplier<T> supplier) {
		try {
			LooseContext.push();
			Document.contextProvider.createFrame(RemoteType.NONE);
			return supplier.get();
		} finally {
			LooseContext.pop();
		}
	}

	private DomDocument renderDocument() {
		Element element = renderElement();
		String outerHtml = element.getOuterHtml();
		outerHtml = EntityCleaner.get().htmlToUnicodeEntities(outerHtml);
		outerHtml = XmlUtils.balanceForXhtml(outerHtml);
		DomDocument doc = DomDocument.basicHtmlDoc();
		DomNode div = doc.html().body().builder().tag("div").append();
		div.setInnerXml(outerHtml);
		stylePaths.forEach(p -> {
			String style = Io.read().relativeTo(p.styleRelativeClass)
					.resource(p.styleRelativeFilename).asString();
			if (Ax.notBlank(style)) {
				doc.html().appendStyleNode(style);
			}
		});
		return doc;
	}

	private Element renderElement() {
		DirectedLayout layout = new DirectedLayout();
		Element element = layout.render(contextResolver, renderable)
				.getRendered().asElement();
		// unbind listeners
		layout.remove();
		return element;
	}

	public DirndlRenderer withRenderable(Model renderable) {
		this.renderable = renderable;
		return this;
	}

	public DirndlRenderer withResolver(ContextResolver contextResolver) {
		this.contextResolver = contextResolver;
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
