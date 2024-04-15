package cc.alcina.framework.gwt.client.story.teller;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Result;
import cc.alcina.framework.gwt.client.story.teller.StoryTeller.Visit;

public class StoryActionPerformer {
	public Result result = new Result();

	private ContextImpl context;

	public class ContextImpl implements Story.Action.Context {
		public Result getResult() {
			return result;
		}
	}

	public Result perform(Visit visit) {
		Story.Action action = visit.getAction();
		if (action == null) {
			return result;
		}
		Class<? extends Story.Action> actionClass = action.getActionClass();
		ActionTypePerformer performer = Registry.impl(ActionTypePerformer.class,
				actionClass);
		context = new ContextImpl();
		try {
			performer.perform(context, action);
		} catch (Throwable t) {
			t.printStackTrace();
			result.ok = false;
			result.throwable = t;
		}
		return result;
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
