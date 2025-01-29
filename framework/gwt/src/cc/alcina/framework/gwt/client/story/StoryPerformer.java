package cc.alcina.framework.gwt.client.story;

import java.lang.System.Logger.Level;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.gwt.client.story.Story.Action;
import cc.alcina.framework.gwt.client.story.Story.Action.Annotate;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Location;
import cc.alcina.framework.gwt.client.story.Story.Action.Location.Axis;
import cc.alcina.framework.gwt.client.story.Story.Attribute;
import cc.alcina.framework.gwt.client.story.Story.Attribute.Entry;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit.Result;
import cc.alcina.framework.gwt.client.util.LineCallback;

public class StoryPerformer {
	public interface PerformerAttribute<T> extends Story.Attribute<T> {
		public static interface Timeout extends PerformerAttribute<Integer> {
		}

		public static interface RomcomMessageQueueAwaitDisabled
				extends PerformerAttribute<Boolean> {
		}
	}

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

		@Override
		public <V> Entry<V, Attribute<V>>
				getAttribute(Class<? extends Attribute<V>> clazz) {
			return visit.teller().state.getAttribute(clazz);
		}

		@Override
		public <V> void setAttribute(Class<? extends Attribute<V>> clazz,
				V value) {
			visit.teller().state.setAttribute(clazz, value);
		}

		@Override
		public void removeAttribute(Class<? extends Attribute<?>> clazz) {
			visit.teller().state.removeAttribute(clazz);
		}
	}

	public void perform(Visit visit) {
		if (!visit.result.ok) {
			// already failed
			return;
		}
		// actions that change the UI
		performAction(visit);
		// actions that decorate the UI, but should not affect the performance
		// of the story
		performAnnotate(visit);
	}

	void performAction(Visit visit) {
		Story.Action action = visit.getAction();
		if (action == null) {
			return;
		}
		// required for the conditional logic (which uses parent attributes)
		Preconditions.checkState(visit.getParent() != null,
				"Root visit/points cannot have an action");
		context = new ContextImpl();
		context.visit = visit;
		Class<? extends Story.Action> actionClass = action.getActionClass();
		ActionTypePerformer performer = Registry.impl(ActionTypePerformer.class,
				actionClass);
		try {
			performer.perform(context, action);
			new ActionPerformed(context, action).publish();
		} catch (Throwable t) {
			System.out.println();
			t.printStackTrace();
			visit.result.ok = false;
			visit.result.throwable = t;
		}
	}

	void performAnnotate(Visit visit) {
		List<Annotate> annotates = visit.getAnnotateActions();
		if (annotates.isEmpty()) {
			return;
		}
		// required for the conditional logic (which uses parent attributes)
		Preconditions.checkState(visit.getParent() != null,
				"Root visit/points cannot have annotates");
		for (Annotate annotate : annotates) {
			context = new ContextImpl();
			context.visit = visit;
			Class<? extends Story.Action> actionClass = annotate
					.getActionClass();
			ActionTypePerformer performer = Registry
					.impl(ActionTypePerformer.class, actionClass);
			try {
				performer.perform(context, annotate);
				new ActionPerformed(context, annotate).publish();
			} catch (Throwable t) {
				System.out.println();
				t.printStackTrace();
				visit.result.ok = false;
				visit.result.throwable = t;
				break;
			}
		}
	}

	@Registration.NonGenericSubtypes(ActionTypePerformer.class)
	public interface ActionTypePerformer<A extends Story.Action>
			extends Registration.AllSubtypes {
		void perform(Story.Action.Context context, A action) throws Exception;
	}

	public static class ActionPerformed implements ProcessObservable {
		public Context context;

		public Action action;

		public ActionPerformed(Story.Action.Context context,
				Story.Action action) {
			this.context = context;
			this.action = action;
		}
	}

	public static class Code implements ActionTypePerformer<Story.Action.Code> {
		@Override
		public void perform(Context context, Story.Action.Code action)
				throws Exception {
			action.perform(context);
		}
	}

	public void beforeChildren(Visit visit) {
		Story.Action action = visit.getAction();
		if (action == null) {
			return;
		}
		if (action instanceof Story.Point.BeforeChildren) {
			context = new ContextImpl();
			context.visit = visit;
			Story.Point.BeforeChildren beforeChildren = (Story.Point.BeforeChildren) action;
			try {
				beforeChildren.beforeChildren(context);
			} catch (Throwable t) {
				System.out.println();
				t.printStackTrace();
				visit.result.ok = false;
				visit.result.throwable = t;
			}
		}
	}
}
