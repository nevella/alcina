package cc.alcina.extras.webdriver.story;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;

import cc.alcina.extras.webdriver.query.ElementQuery;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.doc.ScreenshotData;

public class AnnotatePerformer
		extends WdActionPerformer<Story.Action.Annotate> {
	public static interface AnnotatedElementsAttr
			extends PerformerAttribute<AnnotatedElements> {
		static AnnotatedElements ensure(Story.Action.Context context) {
			return context.ensureAttribute(AnnotatedElementsAttr.class,
					() -> new AnnotatedElements()).get();
		}
	}

	static class AnnotatedElements {
		List<AnnotatedElement> elements = new ArrayList<>();

		AnnotatedElement createAnnotated(WebElement element) {
			AnnotatedElement annotated = new AnnotatedElement(element);
			elements.add(annotated);
			return annotated;
		}

		static class AnnotatedElement {
			WebElement element;

			AnnotatedElement(WebElement element) {
				this.element = element;
			}

			public void highlight(WdActionPerformer wdPerformer) {
				JavascriptExecutor executor = (JavascriptExecutor) wdPerformer.wdContext.token
						.getWebDriver();
				String script = Io.read().resource("res/highlight.js")
						.asString();
				executor.executeScript(script, element);
			}

			public void clear(WdActionPerformer wdPerformer) {
				JavascriptExecutor executor = (JavascriptExecutor) wdPerformer.wdContext.token
						.getWebDriver();
				String script = Io.read().resource("res/removeHighlight.js")
						.asString();
				executor.executeScript(script, element);
			}
		}
	}

	public static class Highlight
			implements TypedPerformer<Story.Action.Annotate.Highlight> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Annotate.Highlight action) throws Exception {
			AnnotatedElements elements = AnnotatedElementsAttr
					.ensure(wdPerformer.context);
			ElementQuery query = createQuery(wdPerformer);
			WebElement element = query.getElement();
			elements.createAnnotated(element).highlight(wdPerformer);
		}
	}

	public static class Screenshot
			implements TypedPerformer<Story.Action.Annotate.Screenshot> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Annotate.Screenshot action) throws Exception {
			byte[] pngBytes = ((TakesScreenshot) wdPerformer.wdContext.token
					.getWebDriver()).getScreenshotAs(OutputType.BYTES);
			wdPerformer.context.setAttribute(ScreenshotData.class, pngBytes);
		}
	}

	public static class Clear
			implements TypedPerformer<Story.Action.Annotate.Clear> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Annotate.Clear action) throws Exception {
			AnnotatedElements elements = AnnotatedElementsAttr
					.ensure(wdPerformer.context);
			ElementQuery query = createQuery(wdPerformer);
			WebElement element = query.getElement();
			elements.createAnnotated(element).clear(wdPerformer);
		}
	}

	public static class HighlightScreenshotClear implements
			TypedPerformer<Story.Action.Annotate.HighlightScreenshotClear> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Annotate.HighlightScreenshotClear action)
				throws Exception {
			new Highlight().perform(wdPerformer, null);
			new Screenshot().perform(wdPerformer, null);
			new Clear().perform(wdPerformer, null);
		}
	}

	public static class Caption
			implements TypedPerformer<Story.Action.Annotate.Caption> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Annotate.Caption action) throws Exception {
			throw new UnsupportedOperationException();
		}
	}
}