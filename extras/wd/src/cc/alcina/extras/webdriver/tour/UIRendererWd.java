package cc.alcina.extras.webdriver.tour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.openqa.selenium.WebElement;

import cc.alcina.extras.webdriver.WDUtils.TimedOutException;
import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeBuilder;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.gwt.client.tour.Tour;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluator;
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

	@Override
	protected void afterStepListenerAction() {
	}

	@Override
	protected void clearPopups(int delay) {
		if (delay != 0) {
			try {
				Thread.sleep(delay);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		popups.forEach(RenderedPopup::remove);
		popups.clear();
	}

	public String dashedEnum(Enum e) {
		return CommonUtils.friendlyConstant(e, "-");
	}

	@Override
	protected void exitTour(String message) {
	}

	protected WebElement getElement(List<String> selectors) {
		long timeout = timeout();
		while (System.currentTimeMillis() < timeout) {
			for (String selector : selectors) {
				WebElement result = getElement(selector);
				if (result != null) {
					return result;
				}
			}
		}
		tourManager.getElementException.signal();
		throw new TimedOutException(selectors.size() == 1 ? selectors.get(0)
				: selectors.toString());
	}

	private WebElement getElement(String selector) {
		if (selector.startsWith("/")) {
			exec.xpath(selector);
		} else {
			exec.css(selector);
		}
		if (exec.immediateTest()) {
			return exec.getElement();
		} else {
			try {
				Thread.sleep(200);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return null;
		}
	}

	@Override
	protected boolean hasElement(List<String> selectors) {
		return getElement(selectors) != null;
	}

	TourManagerWd managerWd() {
		return (TourManagerWd) tourManager;
	}

	public void onTourInit() {
		exec.executeScript("document.body.className+=' webdriver'");
	}

	@Override
	protected boolean performAction(Step step) {
		Tour.Condition targetCondition = step.provideTarget();
		if (targetCondition == null) {
			return true;
		}
		ProcessObservers.publish(TourManager.BeforeActionPerformed.class,
				() -> new TourManager.BeforeActionPerformed(step));
		final WebElement target = getElement(targetCondition.getSelectors());
		if (target == null) {
			return false;
		}
		switch (step.getAction()) {
		case CLICK:
			exec.click();
			break;
		case SCRIPT:
			try {
				exec.executeScript(step.getActionValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case EVAL:
			Class<? extends ConditionEvaluator> clazz = Reflections
					.forName(step.getActionValue());
			ConditionEvaluator evaluator = Reflections.newInstance(clazz);
			evaluator.evaluate(tourManager.createConditionEvaluationContext());
			exec.clearBy();
			break;
		case SELECT:
			exec.selectItemByText(step.getActionValue());
			break;
		case SET_TEXT:
			exec.clearAndSetText(step.getActionValue());
			break;
		case SEND_KEYS:
			exec.sendKeys(step.getActionValue());
			break;
		case TEST:
			// a noop, but forces evaluation/popup in wd mode
			break;
		}
		new TourManager.AfterActionPerformed(step).publish();
		return true;
	}

	@Override
	protected void publishNext() {
	}

	@Override
	protected void render(Step step) {
		clearPopups(step.getDelay());
		for (Tour.PopupInfo popupInfo : step.providePopups()) {
			RenderedPopup popup = new RenderedPopup(popupInfo);
			popups.add(popup);
		}
	}

	@Override
	protected boolean showStepPopups() {
		Step step = tourManager.getStep();
		ProcessObservers.publish(TourManager.BeforeStepRendered.class,
				() -> new TourManager.BeforeStepRendered(step));
		popups.forEach(popup -> {
			managerWd().beforePopup.publish(popup);
			popup.waitForSelector();
			popup.render();
			managerWd().afterPopup.publish(popup);
		});
		tourManager.afterStepRendered.publish(step);
		ProcessObservers.publish(TourManager.AfterStepRendered.class,
				() -> new TourManager.AfterStepRendered(step));
		return true;
	}

	@Override
	protected void startTour(TourManager tourManager) {
		popups.clear();
		this.tourManager = tourManager;
		injectResources();
	}

	void injectResources() {
		String js = Io.read().resource("res/UIRendererWd.js").asString();
		String css = Io.read().resource("res/UIRendererWd.css").asString();
		if (managerWd().hidePopups) {
			css += "\n" + Io.read().resource("res/UIRendererWd.hidePopups.css")
					.asString();
		}
		String cmd = Ax.format(
				"var __UIRendererWd_css=\"%s\";\n" + "%s\n"
						+ "window.__UIRendererWd = new UIRendererWd();\n"
						+ "__UIRendererWd.start()",
				StringEscapeUtils.escapeJavaScript(css), js);
		exec.executeScript(cmd);
	}

	public long timeout() {
		return System.currentTimeMillis() + (TourManager.isImmediateGet() ? 1
				: Configuration.getInt("timeout"));
	}

	<T> T wdJsInvoke(boolean escape, String template, Object... args) {
		return wdJsInvoke0(0, escape, template, args);
	}

	<T> T wdJsInvoke0(int counter, boolean escape, String template,
			Object... args) {
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
			if (counter == 0
					&& e.toString().contains("__UIRendererWd is not defined")) {
				injectResources();
				return wdJsInvoke0(1, escape, template, args);
			}
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

		void remove() {
			try {
				exec.clearBy();
				wdJsInvoke("remove('%s')", id);
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
			}
		}

		void render() {
			DomDocument doc = DomDocument.from("<div/>");
			DomNode root = doc.getDocumentElementNode();
			root.setClassName("tmwd-popup");
			String direction = dashedEnum(
					popupInfo.getRelativeTo().getDirection());
			root.style().addClassName(direction);
			root.builder().tag("caption-area").text(popupInfo.getCaption())
					.append();
			String description = popupInfo.getDescription();
			String mdRegex = "(?s)[^\n]+\\.md\n(.+)";
			String descriptionTag = "description";
			boolean markdown = description.matches(mdRegex);
			if (markdown) {
				descriptionTag = "description-md";
				description = description.replaceFirst(mdRegex, "$1");
				List<Extension> extensions = Arrays
						.asList(TablesExtension.create());
				Parser parser = Parser.builder().extensions(extensions).build();
				Node document = parser.parse(description);
				HtmlRenderer renderer = HtmlRenderer.builder()
						.extensions(extensions).build();
				String html = renderer.render(document);
				description = html;
			}
			DomNodeBuilder builder = root.builder().tag(descriptionTag);
			if (markdown) {
				DomNode node = builder.build();
				node.setInnerXml(Ax.format("<div>%s</div>", description));
				root.children.append(node);
			} else {
				builder.text(description).append();
			}
			root.setAttr("style", Ax.blankToEmpty(popupInfo.getStyle()));
			root.setAttr("id", id);
			String soleSelector = popupInfo.getRelativeTo().isStepTarget()
					? Tour.Condition.soleSelector(currentStep().provideTarget())
					: "";
			wdJsInvoke("renderRelative('%s','%s', '%s')",
					JacksonUtils.serializeNoTypes(popupInfo), soleSelector,
					root.toMarkup());
		}

		public boolean textMatches(String string) {
			return popupInfo.getCaption().matches(string);
		}

		@Override
		public String toString() {
			return Ax
					.format("%s :: %s\n\n%s", popupInfo.getCaption(),
							Tour.RelativeTo.provideElement(
									popupInfo.getRelativeTo(), currentStep()),
							popupInfo.getDescription());
		}

		public void waitForSelector() {
			getElement(Collections.singletonList(Tour.RelativeTo
					.provideElement(popupInfo.getRelativeTo(), currentStep())));
		}
	}
}
