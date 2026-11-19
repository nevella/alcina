package cc.alcina.framework.gwt.client.dirndl.overlay;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed(className = "info-dialog")
public abstract class InfoDialog extends Model {
	private final String areaTitle;

	private final String content;

	protected InfoDialog(String title, String contentHtml) {
		this.areaTitle = title;
		this.content = contentHtml;
	}

	@Directed
	public String getAreaTitle() {
		return this.areaTitle;
	}

	@Directed(renderer = LeafRenderer.Html.class)
	public String getContent() {
		return this.content;
	}

	public static class Warning extends InfoDialog {
		public Warning(String title, String contentHtml) {
			super(title, contentHtml);
		}
	}
}