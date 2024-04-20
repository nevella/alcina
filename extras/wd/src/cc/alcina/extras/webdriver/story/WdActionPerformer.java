package cc.alcina.extras.webdriver.story;

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
				wdPerformer.wdContext.token.getWebDriver().navigate()
						.to(location.getText());
				wdPerformer.context.log("Navigate --> %s", location.getText());
			}
		}
	}
}
