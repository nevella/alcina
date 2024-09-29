package cc.alcina.extras.webdriver.story;

import org.openqa.selenium.WebElement;

import cc.alcina.extras.webdriver.query.ElementQuery;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Location;
import cc.alcina.framework.gwt.client.story.StoryPerformer;
import cc.alcina.framework.gwt.client.story.StoryPerformer.ActionTypePerformer;

/*
 * Performs UI actions (mutate or annotate). This could be abstracted for a
 * general UI action performer
 * 
 * Each action gets its own performer instance, but story/performer state (WD
 * context) is maintained via the context
 */
public abstract class WdActionPerformer<A extends Story.Action>
		implements ActionTypePerformer<A> {
	public interface PerformerAttribute<T> extends Story.Attribute<T> {
		// Note that this is single use - it's de-reffed once the value is
		// retrieved
		public static interface MarkedElement
				extends PerformerAttribute<WebElement> {
		}
	}

	Context context;

	Story.Action action;

	WdContext wdContext;

	@Override
	public void perform(Context context, A action) throws Exception {
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

	static ElementQuery createQuery(WdActionPerformer wdPerformer) {
		Location mark = wdPerformer.context.getLocation(Location.Axis.MARK);
		Location.Xpath xpath = wdPerformer.context
				.getLocation(Location.Axis.DOCUMENT);
		if (mark != null) {
			WebElement markedElement = wdPerformer.context
					.getAttribute(PerformerAttribute.MarkedElement.class).get();
			wdPerformer.context
					.removeAttribute(PerformerAttribute.MarkedElement.class);
			return ElementQuery.fromElement(markedElement);
		} else if (xpath != null) {
			int timeout = wdPerformer.context
					.getAttribute(
							StoryPerformer.PerformerAttribute.Timeout.class)
					.orElse(5);
			return ElementQuery
					.xpath(wdPerformer.wdContext.token.getWebDriver(),
							xpath.getText())
					.withTimeout(timeout);
		} else {
			return null;
		}
	}

	@Registration.NonGenericSubtypes(TypedPerformer.class)
	interface TypedPerformer<U extends Story.Action>
			extends Registration.AllSubtypes {
		void perform(WdActionPerformer wdPerformer, U action) throws Exception;
	}
}
