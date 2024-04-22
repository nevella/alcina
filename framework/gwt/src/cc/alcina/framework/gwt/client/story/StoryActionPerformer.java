package cc.alcina.framework.gwt.client.story;

import java.lang.System.Logger.Level;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Location;
import cc.alcina.framework.gwt.client.story.Story.Action.Location.Axis;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit.Result;
import cc.alcina.framework.gwt.client.util.LineCallback;

public class StoryActionPerformer {
	private ContextImpl context;

	public class ContextImpl implements Story.Action.Context {
		Visit visit;

		public Result getResult() {
			return visit.result;
		}

		@Override
		public void log(Level level, String template, Object... args) {
			visit.result.logEntry().level(level).template(template).args(args)
					.log();
		}

		@Override
		public LineCallback createLogCallback(Level level) {
			return new LogCallback(level);
		}

		class LogCallback implements LineCallback {
			Level level;

			LogCallback(Level level) {
				this.level = level;
			}

			@Override
			public void accept(String message) {
				log(level, message);
			}
		}

		@Override
		public Visit getVisit() {
			return visit;
		}

		@Override
		public <PR extends PerformerResource> PR
				performerResource(Class<PR> clazz) {
			return visit.teller().state.performerResource(clazz, this);
		}

		@Override
		public <L extends Location> L getLocation(Axis axis) {
			return visit.teller().state.getLocation(axis);
		}

		@Override
		public TellerContext tellerContext() {
			return visit.teller().context;
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
			System.out.println();
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
