package cc.alcina.extras.webdriver.tour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.openqa.selenium.WebElement;

import cc.alcina.extras.webdriver.WDUtils.TimedOutException;
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

	public long timeout() {
		return System.currentTimeMillis() + 2000;
	}

	@Override
	protected void afterStepListenerAction() {
	}

	@Override
	protected void clearPopups() {
		popups.forEach(RenderedPopup::remove);
		popups.clear();
	}

	@Override
	protected void exitTour(String message) {
	}

	protected WebElement getElement(List<String> selectors) {
		long timeout = timeout();
		while (System.currentTimeMillis() < timeout) {
			WebElement result = wdJsInvoke(false, "getForSelectors(%s)",
					JacksonUtils.serializeNoTypes(selectors));
			if (result != null) {
				return result;
			}
		}
		throw new TimedOutException(selectors.size() == 1 ? selectors.get(0)
				: selectors.toString());
	}

	@Override
	protected boolean hasElement(List<String> selectors) {
		return getElement(selectors) != null;
	}

	@Override
	protected boolean performAction(Step step) {
		Tour.Condition targetCondition = step.provideTarget();
		if (targetCondition == null) {
			return true;
		}
		final WebElement target = getElement(targetCondition.getSelectors());
		if (target == null) {
			return false;
		}
		exec.externalElement(target);
		switch (step.getAction()) {
		case NONE:
			break;
		case CLICK:
			exec.click();
			break;
		case SET_TEXT:
			exec.clearAndSetText(step.getActionValue());
			break;
		}
		exec.externalElement(null);
		return true;
	}

	@Override
	protected void publishNext() {
	}

	@Override
	protected void render(Step step) {
		clearPopups();
		for (Tour.PopupInfo popupInfo : step.providePopups()) {
			RenderedPopup popup = new RenderedPopup(popupInfo);
			popups.add(popup);
		}
	}

	@Override
	protected boolean showStepPopups() {
		popups.forEach(popup -> {
			popup.waitForSelector();
			popup.render();
		});
		tourManager.stepRendered.publish(tourManager.getStep());
		return true;
	}

	@Override
	protected void startTour(TourManager tourManager) {
		popups.clear();
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

	TourManagerWd managerWd() {
		return (TourManagerWd) tourManager;
	}

	<T> T wdJsInvoke(boolean escape, String template, Object... args) {
		Object[] escapedArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			escapedArgs[i] = StringEscapeUtils
					.escapeJavaScript(args[i].toString());
		}
		String wdCommand = Ax.format(template, escape ? escapedArgs : args);
		String command = Ax.format("return __UIRendererWd.%s;", wdCommand);
		try {
			return (T) exec.executeScript(command);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	<T> T wdJsInvoke(String template, Object... args) {
		return wdJsInvoke(true, template, args);
	}

	class RenderedPopup {
		String id;

		private PopupInfo popupInfo;

		public RenderedPopup(PopupInfo popupInfo) {
			this.popupInfo = popupInfo;
			id = Ax.format("__tmwd_rendered_popup_%s", ++idCounter);
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s", popupInfo.getCaption(),
					popupInfo.getRelativeTo().getElement());
		}

		public void waitForSelector() {
			getElement(Collections
					.singletonList(popupInfo.getRelativeTo().getElement()));
		}

		void remove() {
			try {
				wdJsInvoke("remove('%s')", id);
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
			}
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