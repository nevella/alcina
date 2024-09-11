package cc.alcina.extras.webdriver.story;

import java.util.Objects;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Preconditions;

import cc.alcina.extras.webdriver.query.ElementQuery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Location;

public class UiPerformer extends WdActionPerformer<Story.Action.Ui> {
	public static class Go implements TypedPerformer<Story.Action.Ui.Go> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.Go action) throws Exception {
			Location.Url location = wdPerformer.context
					.getLocation(Location.Axis.URL);
			WdContext wdContext = wdPerformer.wdContext;
			WebDriver webDriver = wdContext.token.getWebDriver();
			String currentUrl = webDriver.getCurrentUrl();
			String to = location.getText();
			String toUrl = wdPerformer.context
					.performerResource(UrlRouter.class).route(to);
			wdPerformer.context.performerResource(UrlRouter.class);
			if (Objects.equals(currentUrl, toUrl)) {
				if (!wdContext.alwaysRefresh && wdContext.navigationPerformed) {
					return;
				}
			}
			webDriver.navigate().to(toUrl);
			wdPerformer.context.log("Navigate --> %s", toUrl);
			wdContext.navigationPerformed = true;
		}
	}

	public static class Refresh
			implements TypedPerformer<Story.Action.Ui.Refresh> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.Refresh action) throws Exception {
			Location.Url location = wdPerformer.context
					.getLocation(Location.Axis.URL);
			WdContext wdContext = wdPerformer.wdContext;
			WebDriver webDriver = wdContext.token.getWebDriver();
			String currentUrl = webDriver.getCurrentUrl();
			webDriver.navigate().refresh();
			wdPerformer.context.log("Refresh --> %s", currentUrl);
			wdContext.navigationPerformed = true;
		}
	}

	public static class Click implements TypedPerformer<Story.Action.Ui.Click> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.Click action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			query.click();
			wdPerformer.context.log("Click --> %s", query);
		}
	}

	public static class TestPresent
			implements TypedPerformer<Story.Action.Ui.TestPresent> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.TestPresent action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			boolean present = query.isPresent();
			wdPerformer.context.getVisit().onActionTestResult(present);
			wdPerformer.context.log("TestPresent --> %s", query);
		}
	}

	public static class AwaitPresent
			implements TypedPerformer<Story.Action.Ui.AwaitPresent> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.AwaitPresent action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			query.await();
			wdPerformer.context.log("AwaitPresent --> %s", query);
		}
	}

	public static class AwaitAbsent
			implements TypedPerformer<Story.Action.Ui.AwaitAbsent> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.AwaitAbsent action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			query.awaitAbsent();
			wdPerformer.context.log("AwaitAbsent --> %s", query);
		}
	}

	public static class TestAbsent
			implements TypedPerformer<Story.Action.Ui.TestAbsent> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.TestAbsent action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			boolean absent = !query.isPresent();
			wdPerformer.context.getVisit().onActionTestResult(absent);
			wdPerformer.context.log("TestAbsent --> %s", query);
		}
	}

	public static class Keys implements TypedPerformer<Story.Action.Ui.Keys> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.Keys action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			if (action.isClear()) {
				query.clear();
			}
			String text = action.getText();
			query.sendKeys(text);
			if (Objects.equals(query.getElement().getAttribute("type"),
					"file")) {
				// handle chrome weirdness
				query.emitChangeEvent();
			}
			wdPerformer.context.log("Keys :: '%s' --> %s", text, query);
		}
	}

	public static class ByText
			implements TypedPerformer<Story.Action.Ui.SelectByText> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.SelectByText action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			String text = action.getText();
			query.selectOption(text);
			wdPerformer.context.log("Select :: '%s' --> %s", text, query);
		}
	}

	public static class AwaitAttributePresent
			implements TypedPerformer<Story.Action.Ui.AwaitAttributePresent> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.AwaitAttributePresent action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			String text = action.getText();
			query.awaitAttributePresent(text);
			wdPerformer.context.log("AwaitAttributePresent [%s] --> %s", text,
					query);
		}
	}

	public static class TestAttributePresent
			implements TypedPerformer<Story.Action.Ui.TestAttributePresent> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.TestAttributePresent action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			String text = action.getText();
			boolean present = query.isAttributePresent(text);
			wdPerformer.context.getVisit().onActionTestResult(present);
			wdPerformer.context.log("TestAttributePresent [%s] --> %s", text,
					query);
		}
	}

	public static class TestAttributeValue
			implements TypedPerformer<Story.Action.Ui.TestAttributeValue> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.TestAttributeValue action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			String text = action.getText();
			String name = action.getName();
			String value = query.getAttributeValue(name);
			boolean test = action.operator.test(value, text);
			wdPerformer.context.getVisit().onActionTestResult(test);
			wdPerformer.context.log("TestAttributeValue [%s %s '%s'] --> %s",
					name, action.operator, text, query);
		}
	}

	public static class AwaitAttributeValue
			implements TypedPerformer<Story.Action.Ui.AwaitAttributeValue> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.AwaitAttributeValue action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			String text = action.getText();
			String name = action.getName();
			String value = query.getAttributeValue(name);
			boolean test = action.operator.test(value, text);
			wdPerformer.context.getVisit().onActionTestResult(test);
			wdPerformer.context.log("TestAttributeValue [%s %s '%s'] --> %s",
					name, action.operator, text, query);
		}
	}

	public static class Mark implements TypedPerformer<Story.Action.Ui.Mark> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.Mark action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			WebElement elem = query.getElement();
			Preconditions.checkNotNull(elem);
			wdPerformer.context
					.setAttribute(PerformerAttribute.MarkedElement.class, elem);
			wdPerformer.context.log("Mark [%s]", query);
		}
	}

	public static class Script
			implements TypedPerformer<Story.Action.Ui.Script> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.Script action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			WebElement elem = query == null ? null : query.getElement();
			JavascriptExecutor executor = (JavascriptExecutor) wdPerformer.wdContext.token
					.getWebDriver();
			executor.executeScript(action.getText(), elem);
		}
	}

	public static class ScrollIntoView
			implements TypedPerformer<Story.Action.Ui.ScrollIntoView> {
		@Override
		public void perform(WdActionPerformer wdPerformer,
				Story.Action.Ui.ScrollIntoView action) throws Exception {
			ElementQuery query = WdActionPerformer.createQuery(wdPerformer);
			query.withElement(elem -> {
				JavascriptExecutor executor = (JavascriptExecutor) wdPerformer.wdContext.token
						.getWebDriver();
				int fromTop = 150;
				String script = Io.read().resource("res/scrollIntoView.js")
						.asString();
				script = Ax.format(script, fromTop).replace("//", "");
				executor.executeScript(script, elem);
			});
		}
	}
}