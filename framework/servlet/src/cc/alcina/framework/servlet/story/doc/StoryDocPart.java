package cc.alcina.framework.servlet.story.doc;

import java.util.Date;

import cc.alcina.framework.gwt.client.story.Story.Point;
import cc.alcina.framework.gwt.client.story.TellerContext;

public class StoryDocPart implements TellerContext.Part {
	public String path;

	public RendererConfiguration rendererConfiguration;

	public static class RendererConfiguration {
		public Class<? extends StoryDocRenderer> renderer = DocumentRenderer.class;

		public String storyTitle;

		public String device = "Desktop";

		public String build;

		public Date date = new Date();

		public Class<? extends Point> pointFilter;
	}
}