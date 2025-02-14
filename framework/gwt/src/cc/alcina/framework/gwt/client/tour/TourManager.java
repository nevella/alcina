package cc.alcina.framework.gwt.client.tour;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.consort.AllStatesConsort;
import cc.alcina.framework.common.client.consort.Consort;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.tour.StepPopupView.Action;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluationContext;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluator;
import cc.alcina.framework.gwt.client.tour.Tour.Operator;
import cc.alcina.framework.gwt.client.tour.Tour.Step;

public abstract class TourManager {
	private static final transient String CONTEXT_IMMEDIATE_GET = TourManager.class
			.getName() + ".CONTEXT_IMMEDIATE_GET";

	public static final transient String CONTEXT_IN_IGNORE_TEST = TourManager.class
			.getName() + ".CONTEXT_IN_IGNORE_TEST";

	public static boolean isImmediateGet() {
		return LooseContext.is(CONTEXT_IMMEDIATE_GET);
	}

	protected TourState currentTour;

	String tourJson = "";

	private DisplayStepConsort consort;

	protected boolean autoplay;

	protected Tour.Step step;

	public Topic<Step> afterStepRendered = Topic.create();

	public Topic<Void> getElementException = Topic.create();

	boolean exit;

	TopicListener<StepPopupView.Action> stepListener = new TopicListener<StepPopupView.Action>() {
		@Override
		public void topicPublished(Action message) {
			switch (message) {
			case CLOSE:
				if (consort != null) {
					consort.cancel();
				}
				UIRenderer.get().clearPopups(step.getDelay());
				break;
			case NEXT:
				currentTour.gotoStep(currentTour.getCurrentStepIndex() + 1);
				log("Moved to tour step %s :: %s",
						currentTour.getCurrentStepIndex(), step.asString());
				onNext();
				refreshTourView();
				break;
			case BACK:
				currentTour.gotoStep(currentTour.getCurrentStepIndex() - 1);
				refreshTourView();
				break;
			}
			UIRenderer.get().afterStepListenerAction();
		}
	};

	private AsyncCallback completionCallback;

	protected TourManager() {
		super();
	}

	public void allSteps(Tour tour) {
		startTour(tour);
		while (step != Ax.last(tour.getSteps()) && !exit) {
			currentTour.gotoStep(currentTour.getCurrentStepIndex() + 1);
			refreshTourView();
			onNext();
		}
	}

	public ConditionEvaluationContext createConditionEvaluationContext() {
		return new ConditionEvaluationContext(currentTour);
	}

	protected void exitTour(String message) {
		UIRenderer.get().exitTour(message);
	}

	public Tour.Step getStep() {
		return this.step;
	}

	protected void log(String template, Object... args) {
		if (GWT.isClient() && !GWT.isScript()) {
			ClientNotifications.get().log(template, args);
		}
	}

	protected void onNext() {
	}

	protected void refreshTourView() {
		if (consort != null) {
			consort.cancel();
		}
		consort = new DisplayStepConsort(null);
		consort.start();
	}

	protected boolean shouldRetry(DisplayStepPhase state) {
		return true;
	}

	public void startTour(Tour tour) {
		UIRenderer.get().startTour(this);
		currentTour = new TourState(tour);
		autoplay = true;
		refreshTourView();
	}

	public void startTourWithJson(String tourJson, boolean autoplay,
			AsyncCallback completionCallback) {
		this.completionCallback = completionCallback;
		int idx1 = 0;
		this.autoplay = autoplay;
		while (true) {
			idx1 = tourJson.indexOf("/*");
			if (idx1 == -1) {
				break;
			}
			int idx2 = tourJson.indexOf("*/");
			tourJson = tourJson.substring(0, idx1)
					+ tourJson.substring(idx2 + 2);
		}
		this.tourJson = tourJson.replaceFirst("var sample = ", "");
		currentTour = TourState.fromJson(this.tourJson);
		UIRenderer.get().startTour(this);
		refreshTourView();
	}

	public static class AfterActionPerformed extends Step.Observable {
		public AfterActionPerformed(Step step) {
			super(step);
		}
	}

	public static class AfterStepRendered extends Step.Observable {
		public AfterStepRendered(Step step) {
			super(step);
		}
	}

	public static class BeforeActionPerformed extends Step.Observable {
		public BeforeActionPerformed(Step step) {
			super(step);
		}
	}

	public static class BeforeStepRendered extends Step.Observable {
		public BeforeStepRendered(Step step) {
			super(step);
		}
	}

	class DisplayStepConsort extends AllStatesConsort<DisplayStepPhase> {
		private TopicListener exitListener = new TopicListener() {
			@Override
			public void topicPublished(Object message) {
				if (!Consort.TopicChannel.FINISHED
						.equals(getFiringTopicChannel())) {
					UIRenderer.get().clearPopups(step.getDelay());
				}
			}
		};

		public DisplayStepConsort(AsyncCallback callback) {
			super(DisplayStepPhase.class, callback);
			this.timeout = 20000;
			exitListenerDelta(exitListener, false, true);
			Ax.out(currentTour.getCurrentStep());
		}

		/*
		 * return true if we should ignore this step
		 */
		private boolean checkIgnore() {
			Tour.Condition ignoreIf = step.getIgnoreIf();
			if (ignoreIf != null) {
				return evaluateCondition(ignoreIf);
			}
			return false;
		}

		private boolean checkIgnoreAction() {
			Tour.Condition ignoreActionIf = step.getIgnoreActionIf();
			if (ignoreActionIf != null) {
				try {
					LooseContext.pushWithTrue(CONTEXT_IN_IGNORE_TEST);
					return evaluateCondition(ignoreActionIf);
				} finally {
					LooseContext.pop();
				}
			}
			return false;
		}

		private boolean evaluateCondition(Tour.Condition condition) {
			Optional<ConditionEvaluator> evaluator = Tour.Condition
					.provideEvaluator(condition);
			if (evaluator.isPresent()) {
				return evaluator.get()
						.evaluate(createConditionEvaluationContext());
			}
			Tour.Operator operator = condition.getOperator();
			int conditionCount = 0;
			int passCount = 0;
			for (String selector : condition.getSelectors()) {
				conditionCount++;
				try {
					LooseContext.pushWithTrue(CONTEXT_IMMEDIATE_GET);
					passCount += UIRenderer.get().hasElement(
							Collections.singletonList(selector)) ? 1 : 0;
				} catch (Exception e) {
					// condition not met, but do not throw
					//
				} finally {
					LooseContext.pop();
				}
			}
			for (Tour.Condition child : condition.getConditions()) {
				conditionCount++;
				passCount += evaluateCondition(child) ? 1 : 0;
			}
			if (operator == null) {
				operator = Operator.AND;
			}
			switch (operator) {
			case AND:
				return conditionCount == passCount;
			case OR:
				return passCount > 0;
			case NOT:
				return passCount == 0;
			default:
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public void finished() {
			super.finished();
			if (autoplay && currentTour.hasNext()) {
				UIRenderer.get().publishNext();
			}
			if (completionCallback != null && autoplay
					&& !currentTour.hasNext()) {
				completionCallback.onSuccess(null);
			}
		}

		@Override
		protected DisplayStepPhase[] getStates() {
			if (currentTour.getCurrentStep().isActionBeforePopups()) {
				Comparator<DisplayStepPhase> cmp = new Comparator<TourManager.DisplayStepPhase>() {
					@Override
					public int compare(DisplayStepPhase o1,
							DisplayStepPhase o2) {
						return swapActionPopups(o1)
								.compareTo(swapActionPopups(o2));
					}

					private DisplayStepPhase
							swapActionPopups(DisplayStepPhase phase) {
						switch (phase) {
						case SHOW_POPUPS:
							return DisplayStepPhase.PERFORM_ACTION;
						case PERFORM_ACTION:
							return DisplayStepPhase.SHOW_POPUPS;
						default:
							return phase;
						}
					}
				};
				List<DisplayStepPhase> list = Arrays
						.stream(enumClass.getEnumConstants()).sorted(cmp)
						.collect(Collectors.toList());
				return (DisplayStepPhase[]) list
						.toArray(new DisplayStepPhase[list.size()]);
			} else {
				return super.getStates();
			}
		}

		@Override
		public void onFailure(Throwable throwable) {
			super.onFailure(throwable);
			finished();
			if (completionCallback != null) {
				completionCallback.onFailure(throwable);
			}
		}

		private boolean performAction() {
			return UIRenderer.get().performAction(step);
		}

		private void render() {
			step = currentTour.getCurrentStep();
			UIRenderer.get().render(step);
		}

		@Override
		public void retry(
				AllStatesConsort<DisplayStepPhase>.AllStatesPlayer allStatesPlayer,
				DisplayStepPhase state, int delay) {
			if (shouldRetry(state)) {
				super.retry(allStatesPlayer, state, delay);
			} else {
				cancel();
			}
		}

		@Override
		public void runPlayer(AllStatesPlayer player, DisplayStepPhase next) {
			if (!isRunning()) {
				return;
			}
			switch (next) {
			case SETUP:
				render();
				wasPlayed(player);
				break;
			case WAIT_FOR:
				if (waitFor()) {
					wasPlayed(player);
				} else {
					retry(player, next, 200);
				}
				break;
			case IGNORE_IF:
				if (checkIgnore()) {
					finished();
					if (currentTour.hasNext()) {
						stepListener.topicPublished(Action.NEXT);
					}
				} else {
					wasPlayed(player);
				}
				break;
			case SHOW_POPUPS:
				if (showStepPopups()) {
					wasPlayed(player);
				} else {
					retry(player, next, 200);
				}
				break;
			case PERFORM_ACTION:
				if (checkIgnoreAction() || performAction()) {
					wasPlayed(player);
					if (step.provideTarget() == null && currentTour.hasNext()) {
						stepListener.topicPublished(Action.NEXT);
					}
				} else {
					retry(player, next, 200);
				}
				break;
			}
		}

		protected boolean showStepPopups() {
			return UIRenderer.get().showStepPopups();
		}

		@Override
		protected void timedOut(AllStatesPlayer allStatesPlayer,
				DisplayStepPhase state) {
			System.out.println(Ax.format("Timed out - %s - %s",
					currentTour.getCurrentStep(), state));
			super.timedOut(allStatesPlayer, state);
		}

		/*
		 * return false if we need to keep waiting
		 */
		private boolean waitFor() {
			Tour.Condition waitFor = step.getWaitFor();
			if (waitFor != null) {
				return evaluateCondition(waitFor);
			}
			return true;
		}
	}

	public static enum DisplayStepPhase {
		SETUP, WAIT_FOR, IGNORE_IF, SHOW_POPUPS, PERFORM_ACTION
	}

	public static abstract class UIRenderer {
		public static TourManager.UIRenderer get() {
			return Registry.impl(TourManager.UIRenderer.class);
		}

		protected TourManager tourManager;

		protected abstract void afterStepListenerAction();

		protected abstract void clearPopups(int delay);

		protected Step currentStep() {
			return tourManager.currentTour.getCurrentStep();
		}

		protected abstract void exitTour(String message);

		protected abstract boolean hasElement(List<String> selectors);

		protected abstract boolean performAction(Step step);

		protected abstract void publishNext();

		protected abstract void render(Step step);

		public void setTourManager(TourManager tourManager) {
			this.tourManager = tourManager;
		}

		protected abstract boolean showStepPopups();

		protected abstract void startTour(TourManager tourManager);
	}
}