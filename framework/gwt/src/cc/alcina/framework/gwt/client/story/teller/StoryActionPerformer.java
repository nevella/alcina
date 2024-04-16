package cc.alcina.framework.gwt.client.story.teller;

import java.lang.System.Logger.Level;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.teller.StoryTeller.Visit;
import cc.alcina.framework.gwt.client.story.teller.StoryTeller.Visit.Result;

public class StoryActionPerformer {
	private ContextImpl context;

	public class ContextImpl implements Story.Action.Context {
		public Visit visit;

		public Result getResult() {
			return visit.result;
		}

		@Override
		public void log(Level level, String template, Object... args) {
			visit.result.logEntry().level(level).template(template).args(args)
					.log();
		}
	}

	public void perform(Visit visit) {
		Story.Action action = visit.getAction();
		if (action == null) {
			return;
		}
		context = new ContextImpl();
		context.visit = visit;
		Class<? extends Story.Action> actionClass = action.getActionClass();
		ActionTypePerformer performer = Registry.impl(ActionTypePerformer.class,
				actionClass);
		try {
			performer.perform(context, action);
		} catch (Throwable t) {
			t.printStackTrace();
			visit.result.ok = false;
			visit.result.throwable = t;
		}
	}

	@Registration.NonGenericSubtypes(ActionTypePerformer.class)
	public interface ActionTypePerformer<A extends Story.Action>
			extends Registration.AllSubtypes {
		void perform(Story.Action.Context context, A action) throws Exception;
	}

	public static class Code implements ActionTypePerformer<Story.Action.Code> {
		@Override
		public void perform(Context context, Story.Action.Code action)
				throws Exception {
			action.perform(context);
		}
	}
}
