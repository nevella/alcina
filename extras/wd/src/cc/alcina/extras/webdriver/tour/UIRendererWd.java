package cc.alcina.extras.webdriver.tour;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.gwt.client.tour.Tour;
import cc.alcina.framework.gwt.client.tour.Tour.PopupInfo;
import cc.alcina.framework.gwt.client.tour.Tour.Step;
import cc.alcina.framework.gwt.client.tour.TourManager;
import cc.alcina.framework.gwt.client.tour.TourManager.UIRenderer;

public class UIRendererWd extends UIRenderer {
	public static UIRendererWd get() {
		return (UIRendererWd) UIRenderer.get();
	}

	public WdExec exec;

	private int idCounter;

	protected List<RenderedPopup> popups = new ArrayList<>();

	public String dashedEnum(Enum e) {
		return CommonUtils.friendlyConstant(e, "-");
	}

	@Override
	protected void afterStepListenerAction() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void clearPopups() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void exitTour(String message) {
		// TODO Auto-generated method stub
	}

	@Override
	protected boolean hasElement(List<String> selectors) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean performAction(Step step) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void publishNext() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void render(Step step) {
		clearPopups();
		int idx = 0;
		for (Tour.PopupInfo popupInfo : step.providePopups()) {
			RenderedPopup popup = new RenderedPopup(popupInfo);
			popup.render();
			popups.add(popup);
		}
		int debug = 3;
	}

	@Override
	protected boolean showStepPopups() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void startTour(TourManager tourManager) {
		this.tourManager = tourManager;
		String js = ResourceUtilities
				.readRelativeResource("res/UIRendererWd.js");
		String css = ResourceUtilities
				.readRelativeResource("res/UIRendererWd.css");
		String cmd = Ax.format(
				"var __UIRendererWd_css=\"%s\";\n" + "%s\n"
						+ "window.__UIRendererWd = new UIRendererWd();\n"
						+ "__UIRendererWd.start()",
				StringEscapeUtils.escapeJavaScript(css), js);
		exec.executeScript(cmd);
	}

	Object wdJsInvoke(String template, Object... args) {
		Object[] escapedArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			escapedArgs[i] = StringEscapeUtils
					.escapeJavaScript(args[i].toString());
		}
		String wdCommand = Ax.format(template, escapedArgs);
		String command = Ax.format("__UIRendererWd.%s;", wdCommand);
		try {
			return exec.executeScript(command);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	class RenderedPopup {
		String id;

		private PopupInfo popupInfo;

		public RenderedPopup(PopupInfo popupInfo) {
			this.popupInfo = popupInfo;
			id = Ax.format("__tmwd_rendered_popup_%s", ++idCounter);
		}

		void remove() {
			wdJsInvoke("remove('%s')", id);
		}

		void render() {
			DomDoc doc = DomDoc.from("<div/>");
			DomNode root = doc.getDocumentElementNode();
			root.setClassName("tmwd-popup");
			String direction = dashedEnum(
					popupInfo.getRelativeTo().getDirection());
			root.style().addClassName(direction);
			root.builder().tag("caption-area").text(popupInfo.getCaption())
					.append();
			root.builder().tag("description").text(popupInfo.getDescription())
					.append();
			root.setAttr("id", id);
			wdJsInvoke("renderRelative('%s','%s')",
					JacksonUtils.serializeNoTypes(popupInfo),
					root.fullToString());
		}
	}
}
