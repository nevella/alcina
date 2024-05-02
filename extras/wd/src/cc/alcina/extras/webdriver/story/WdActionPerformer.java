package cc.alcina.extras.webdriver.story;

import java.util.Objects;

import org.openqa.selenium.WebDriver;

import cc.alcina.extras.webdriver.query.ElementQuery;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Ui;
import cc.alcina.framework.gwt.client.story.StoryActionPerformer.ActionTypePerformer;

/*
 * Performs UI actions. This could be abstracted for a general UI action
 * performer
 * 
 * Each action gets its own performer instance, but story/performer state (WD
 * context) is maintained via the context
 */
public class WdActionPerformer implements ActionTypePerformer<Story.Action.Ui> {
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
				Story.Action.Location.Url location = wdPerformer.context
						.getLocation(Story.Action.Location.Axis.URL);
				WdContext wdContext = wdPerformer.wdContext;
				WebDriver webDriver = wdContext.token.getWebDriver();
				String currentUrl = webDriver.getCurrentUrl();
				String to = location.getText();
				if (Objects.equals(currentUrl, to)) {
					if (!wdContext.alwaysRefresh
							&& wdContext.navigationPerformed) {
						return;
					}
				}
				webDriver.navigate().to(to);
				wdPerformer.context.log("Navigate --> %s", to);
				wdContext.navigationPerformed = true;
			}
		}

		public static class Refresh
				implements TypedPerformer<Story.Action.Ui.Refresh> {
			@Override
			public void perform(WdActionPerformer wdPerformer,
					Story.Action.Ui.Refresh action) throws Exception {
				Story.Action.Location.Url location = wdPerformer.context
						.getLocation(Story.Action.Location.Axis.URL);
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
				String text = action.getText();
				query.sendKeys(text);
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
			Story.Action.Location.Xpath location = wdPerformer.context
					.getLocation(Story.Action.Location.Axis.DOCUMENT);
			return ElementQuery.xpath(
					wdPerformer.wdContext.token.getWebDriver(),
					location.getText());
		}
	}
}
