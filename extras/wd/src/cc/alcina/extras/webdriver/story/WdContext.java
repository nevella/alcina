package cc.alcina.extras.webdriver.story;

import java.awt.Toolkit;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

import com.google.common.base.Preconditions;

import cc.alcina.extras.webdriver.WDConfiguration;
import cc.alcina.extras.webdriver.WDConfiguration.WebDriverType;
import cc.alcina.extras.webdriver.WDDriverHandler;
import cc.alcina.extras.webdriver.WDToken;
import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Context.PerformerResource;
import cc.alcina.framework.gwt.client.story.StoryPerformer;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.StoryTeller.AfterStory;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;

public class WdContext implements PerformerResource {
	public WDToken token;

	public WdExec exec;

	boolean alwaysRefresh = false;

	boolean navigationPerformed = false;

	private WdContextPart part;

	@Override
	public void initialise(Context context) {
		part = context.tellerContext().getPart(WdContextPart.class);
		if (part.waitForEmptyRocomEventQueue) {
			new ActionPerformedObserver().bind();
		}
		WDConfiguration configuration = new WDConfiguration();
		configuration.driverType = getDriverType();
		token = new WDToken();
		token.setConfiguration(configuration);
		token.setDriverHandler(configuration.driverHandler());
		try {
			LooseContext.push();
			LooseContext.set(WDDriverHandler.CONTEXT_REUSE_SESSION,
					part.reuseSession);
			WebDriver driver = token.getDriverHandler().getDriver();
			token.setWebDriver(driver);
		} finally {
			LooseContext.pop();
		}
		exec = new WdExec();
		exec.timeout(getDefaultTimeout());
		exec.token(token);
		if (part.shouldMaximiseTab) {
			token.getWebDriver().manage().window().maximize();
		}
		if (part.shouldFocusTabAtStart) {
			token.getWebDriver().switchTo()
					.window(token.getWebDriver().getWindowHandle());
		}
		if (part.shouldFocusTabAtEnd) {
			new AfterStoryObserver().bind();
		}
	}

	public int getDefaultTimeout() {
		return part.defaultTimeout;
	}

	class AfterStoryObserver
			implements ProcessObserver<StoryTeller.AfterStory> {
		@Override
		public void topicPublished(AfterStory afterStory) {
			token.getWebDriver().switchTo()
					.window(token.getWebDriver().getWindowHandle());
		}
	}

	@Feature.Ref(Feature_RemoteObjectComponent.Feature_ClientMessageState.class)
	public class ActionPerformedObserver
			implements ProcessObserver<StoryPerformer.ActionPerformed> {
		static final String INFLIGHT_NON_AWAIT_MESSAGE = "__romcom_inflightNonAwaitMessage";

		@Override
		public void topicPublished(StoryPerformer.ActionPerformed action) {
			boolean disabled = action.context.getAttribute(
					StoryPerformer.PerformerAttribute.RomcomMessageQueueAwaitDisabled.class)
					.orElse(false);
			if (disabled) {
				return;
			}
			if (!(action.action instanceof Story.Action.Ui.CausesDomEvent)) {
				return;
			}
			String script = Ax.format("return window.hasOwnProperty('%s');",
					INFLIGHT_NON_AWAIT_MESSAGE);
			JavascriptExecutor executor = (JavascriptExecutor) WdContext.this.token
					.getWebDriver();
			try {
				long start = System.currentTimeMillis();
				int fromTop = 150;
				long timeout = part.defaultTimeout * 1000;
				boolean matched = false;
				while (System.currentTimeMillis() - start < timeout) {
					boolean result = (Boolean) executor.executeScript(script,
							new Object[] { null });
					if (result) {
						Ax.out("romcom sleep");
						Thread.sleep(5);
					} else {
						matched = true;
						break;
					}
				}
				/*
				 * try to force a reflow
				 */
				executor.executeScript(
						"var v=window.innerHeight; var s = document.body.firstElementChild.outerHTML",
						new Object[] { null });
				long end = System.currentTimeMillis();
				Ax.out("romcom await - %s ms", end - start);
				Preconditions.checkState(matched,
						"await __romcom_inflightNonAwaitMessage absence failed");
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}

	WebDriverType getDriverType() {
		// this could be replaced/overriden with/by a peer resource
		return WebDriverType.valueOf(Configuration.get("driverType"));
	}
}