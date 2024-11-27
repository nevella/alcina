package cc.alcina.framework.servlet.story.doc;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.story.Waypoint.TextInterpolator;

@Registration(TextInterpolator.class)
public class TextInterpolatorImpl implements TextInterpolator {
	static class InterpolationResult {
		String content;

		boolean htmlEscaped;

		public InterpolationResult(String content, boolean htmlEscaped) {
			this.content = content;
			this.htmlEscaped = htmlEscaped;
		}

		String escapedContent() {
			return htmlEscaped ? content
					: StringEscapeUtils.escapeHtml4(content);
		}
	}

	public String interpolate(Class<?> clazz, String value) {
		InterpolationResult result = null;
		if (value.startsWith("res:")) {
			String resourcePath = value.substring(4);
			String contents = Io.read().relativeTo(clazz).resource(resourcePath)
					.asString();
			if (resourcePath.endsWith(".md")) {
				result = new InterpolationResult(markdownToHtml(contents),
						true);
			} else if (resourcePath.endsWith(".html")) {
				result = new InterpolationResult(contents, true);
			} else {
				result = new InterpolationResult(contents, false);
			}
		} else {
			result = new InterpolationResult(value, false);
		}
		return result.escapedContent();
	}

	String markdownToHtml(String markdown) {
		List<Extension> extensions = Arrays.asList(TablesExtension.create());
		Parser parser = Parser.builder().extensions(extensions).build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions)
				.build();
		String html = renderer.render(document);
		return html;
	}
}
