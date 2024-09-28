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

	private List<StyleResource> styleResources = new ArrayList<>();

	private Model renderable;

	private ContextResolver contextResolver;

	private boolean wrapStyleInCdata = false;

	public DirndlRenderer addStyleFile(Class<?> styleRelativeClass,
			String styleRelativeFilename) {
		styleResources.add(
				new StyleResource(styleRelativeClass, styleRelativeFilename));
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
		Preconditions.checkState(styleResources.isEmpty());
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
		styleResources.forEach(res -> {
			String style = res.getCss();
			if (Ax.notBlank(style)) {
				doc.html().appendStyleNode(style, wrapStyleInCdata);
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

	public DirndlRenderer withWrapStyleInCdata(boolean wrapStyleInCdata) {
		this.wrapStyleInCdata = wrapStyleInCdata;
		return this;
	}

	public DirndlRenderer withResolver(ContextResolver contextResolver) {
		this.contextResolver = contextResolver;
		return this;
	}

	static class StyleResource {
		Class<?> styleRelativeClass;

		String styleRelativeFilename;

		String css;

		public StyleResource(Class<?> styleRelativeClass,
				String styleRelativeFilename) {
			this.styleRelativeClass = styleRelativeClass;
			this.styleRelativeFilename = styleRelativeFilename;
		}

		public String getCss() {
			if (styleRelativeFilename != null) {
				return Io.read().relativeTo(styleRelativeClass)
						.resource(styleRelativeFilename).asString();
			} else {
				return css;
			}
		}

		StyleResource(String css) {
			this.css = css;
		}

		public static StyleResource cssResource(String css) {
			return new StyleResource(css);
		}
	}

	public String asMarkup() {
		String markup = asDocument().prettyToString();
		if (wrapStyleInCdata) {
			markup = markup.replace("<![CDATA[", "");
			markup = markup.replace("]]", "");
		}
		return markup;
	}

	public DirndlRenderer addStyleResource(String customCss) {
		styleResources.add(StyleResource.cssResource(customCss));
		return this;
	}
}
