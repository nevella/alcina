package cc.alcina.extras.webdriver.story;

import java.util.Objects;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Preconditions;

import cc.alcina.extras.webdriver.query.ElementQuery;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Location;
import cc.alcina.framework.gwt.client.story.Story.Action.Ui;
import cc.alcina.framework.gwt.client.story.StoryActionPerformer;
import cc.alcina.framework.gwt.client.story.StoryActionPerformer.ActionTypePerformer;

/*
 * Performs UI actions. This could be abstracted for a general UI action
 * performer
 * 
 * Each action gets its own performer instance, but story/performer state (WD
 * context) is maintained via the context
 */
public class WdActionPerformer implements ActionTypePerformer<Story.Action.Ui> {
	public interface PerformerAttribute<T> extends Story.Attribute<T> {
		// Note that this is single use - it's de-reffed once the value is
		// retrieved
		public static interface MarkedElement
				extends PerformerAttribute<WebElement> {
		}
	}

	Context context;

	Ui action;

	WdContext wdContext;

	@Override
	public void perform(Context context, Ui action) throws Exception {
		this.context = context;
		this.action = action;
		ensureWd();
		TypedPerformer typedPerformer = Registry.impl(TypedPerformer.class,
				action.getClass());
		typedPerformer.perform(this, action);
	}

	void ensureWd() {
		wdContext = context.performerResource(WdContext.class);
	}

	@Registration.NonGenericSubtypes(TypedPerformer.class)
	interface TypedPerformer<U extends Story.Action.Ui>
			extends Registration.AllSubtypes {
		void perform(WdActionPerformer wdPerformer, U action) throws Exception;

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
					if (!wdContext.alwaysRefresh
							&& wdContext.navigationPerformed) {
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

		public static class Click
				implements TypedPerformer<Story.Action.Ui.Click> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.Click action) throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				query.click();
				wdPerformer.context.log("Click --> %s", query);
			}
		}

		public static class TestPresent
				implements TypedPerformer<Story.Action.Ui.TestPresent> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.TestPresent action) throws Exception {
				ElementQuery query = createQuery(wdPerformer);
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
				ElementQuery query = createQuery(wdPerformer);
				query.await();
				wdPerformer.context.log("AwaitPresent --> %s", query);
			}
		}

		public static class AwaitAbsent
				implements TypedPerformer<Story.Action.Ui.AwaitAbsent> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.AwaitAbsent action) throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				query.awaitAbsent();
				wdPerformer.context.log("AwaitAbsent --> %s", query);
			}
		}

		public static class TestAbsent
				implements TypedPerformer<Story.Action.Ui.TestAbsent> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.TestAbsent action) throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				boolean absent = !query.isPresent();
				wdPerformer.context.getVisit().onActionTestResult(absent);
				wdPerformer.context.log("TestAbsent --> %s", query);
			}
		}

		public static class Keys
				implements TypedPerformer<Story.Action.Ui.Keys> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.Keys action) throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				if (action.isClear()) {
					query.clear();
				}
				String text = action.getText();
				query.sendKeys(text);
				if (query.getElement().getAttribute("type").equals("file")) {
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
				ElementQuery query = createQuery(wdPerformer);
				String text = action.getText();
				query.selectOption(text);
				wdPerformer.context.log("Select :: '%s' --> %s", text, query);
			}
		}

		static ElementQuery createQuery(WdActionPerformer wdPerformer) {
			Location mark = wdPerformer.context.getLocation(Location.Axis.MARK);
			Location.Xpath xpath = wdPerformer.context
					.getLocation(Location.Axis.DOCUMENT);
			if (mark != null) {
				WebElement markedElement = wdPerformer.context
						.getAttribute(PerformerAttribute.MarkedElement.class)
						.get();
				wdPerformer.context.removeAttribute(
						PerformerAttribute.MarkedElement.class);
				return ElementQuery.fromElement(markedElement);
			} else if (xpath != null) {
				int timeout = wdPerformer.context.getAttribute(
						StoryActionPerformer.PerformerAttribute.Timeout.class)
						.orElse(5);
				return ElementQuery
						.xpath(wdPerformer.wdContext.token.getWebDriver(),
								xpath.getText())
						.withTimeout(timeout);
			} else {
				return null;
			}
		}

		public static class AwaitAttributePresent implements
				TypedPerformer<Story.Action.Ui.AwaitAttributePresent> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.AwaitAttributePresent action)
					throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				String text = action.getText();
				query.awaitAttributePresent(text);
				wdPerformer.context.log("AwaitAttributePresent [%s] --> %s",
						text, query);
			}
		}

		public static class TestAttributePresent implements
				TypedPerformer<Story.Action.Ui.TestAttributePresent> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.TestAttributePresent action)
					throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				String text = action.getText();
				boolean present = query.isAttributePresent(text);
				wdPerformer.context.getVisit().onActionTestResult(present);
				wdPerformer.context.log("TestAttributePresent [%s] --> %s",
						text, query);
			}
		}

		public static class TestAttributeValue
				implements TypedPerformer<Story.Action.Ui.TestAttributeValue> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.TestAttributeValue action)
					throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				String text = action.getText();
				String name = action.getName();
				String value = query.getAttributeValue(name);
				boolean test = action.operator.test(value, text);
				wdPerformer.context.getVisit().onActionTestResult(test);
				wdPerformer.context.log(
						"TestAttributeValue [%s %s '%s'] --> %s", name,
						action.operator, text, query);
			}
		}

		public static class AwaitAttributeValue
				implements TypedPerformer<Story.Action.Ui.AwaitAttributeValue> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.AwaitAttributeValue action)
					throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				String text = action.getText();
				String name = action.getName();
				String value = query.getAttributeValue(name);
				boolean test = action.operator.test(value, text);
				wdPerformer.context.getVisit().onActionTestResult(test);
				wdPerformer.context.log(
						"TestAttributeValue [%s %s '%s'] --> %s", name,
						action.operator, text, query);
			}
		}

		public static class Mark
				implements TypedPerformer<Story.Action.Ui.Mark> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.Mark action) throws Exception {
				ElementQuery query = createQuery(wdPerformer);
				WebElement elem = query.getElement();
				Preconditions.checkNotNull(elem);
				wdPerformer.context.setAttribute(
						PerformerAttribute.MarkedElement.class, elem);
				wdPerformer.context.log("Mark [%s]", query);
			}
		}

		public static class Script
				implements TypedPerformer<Story.Action.Ui.Script> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.Script action) throws Exception {
				ElementQuery query = createQuery(wdPerformer);
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
				ElementQuery query = createQuery(wdPerformer);
				WebElement elem = query.getElement();
				Preconditions.checkNotNull(elem);
				JavascriptExecutor executor = (JavascriptExecutor) wdPerformer.wdContext.token
						.getWebDriver();
				executor.executeScript("arguments[0].scrollIntoView()", elem);
			}
		}
	}
}
