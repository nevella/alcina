package cc.alcina.framework.gwt.client.tour;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.reflection.Reflections;

public interface Tour {
	public String getName();

	public List<? extends Tour.Step> getSteps();

	@Reflected
	enum Action {
		CLICK, SET_TEXT, SCRIPT, SELECT, EVAL, TEST, SEND_KEYS
	}

	interface Condition {
		public static Optional<ConditionEvaluator>
				provideEvaluator(Condition condition) {
			if (condition.getEvaluatorClassName() == null) {
				return Optional.empty();
			} else {
				Class<ConditionEvaluator> evaluatorClass = Reflections
						.forName(condition.getEvaluatorClassName());
				return Optional.of(Reflections.newInstance(evaluatorClass));
			}
		}

		public static String soleSelector(Condition condition) {
			Preconditions.checkState(condition.getSelectors().size() == 1);
			return condition.getSelectors().get(0);
		}

		List<? extends Condition> getConditions();

		String getEvaluatorClassName();

		Operator getOperator();

		List<String> getSelectors();
	}

	public static class ConditionEvaluationContext {
		private TourState tourState;

		public ConditionEvaluationContext(TourState tourState) {
			this.tourState = tourState;
		}

		public TourState getTourState() {
			return this.tourState;
		}

		public boolean provideIsFirstStep() {
			return getTourState().getCurrentStepIndex() == 0;
		}
	}

	public interface ConditionEvaluator {
		// return true if the condition is met
		boolean evaluate(ConditionEvaluationContext context);
	}

	enum Operator {
		AND, OR, NOT
	}

	@Reflected
	enum Pointer {
		CENTER_UP, LEFT_UP, CENTER_DOWN, RIGHT_UP, RIGHT_DOWN
	}

	interface PopupInfo {
		public String getCaption();

		public String getDescription();

		public RelativeTo getRelativeTo();

		public String getStyle();
	}

	@Reflected
	enum PositioningDirection {
		CENTER_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, RIGHT_TOP, TOP_LEFT, LEFT_TOP,
		BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, BOTTOM_CENTER, TOP_CENTER
	}

	interface RelativeTo {
		public static String provideElement(RelativeTo relativeTo,
				Step currentStep) {
			if (relativeTo.isStepTarget()) {
				return Tour.Condition.soleSelector(currentStep.provideTarget());
			} else {
				return relativeTo.getElement();
			}
		}

		public PositioningDirection getDirection();

		// FIXME - to 'selector';
		public String getElement();

		public int getOffsetHorizontal();

		public int getOffsetVertical();

		public Pointer getPointer();

		public int getPointerRightMargin();

		public int getPopupFromBottom();

		public boolean isBubble();

		public boolean isStepTarget();
	}

	// @JsonDeserialize(as = StepImpl.class)
	interface Step {
		public String asString();

		public Action getAction();

		public int getActionDelay();

		public String getActionValue();

		public String getComment();

		public int getDelay();

		public Condition getIgnoreActionIf();

		public Condition getIgnoreIf();

		public Condition getWaitFor();

		public boolean isActionBeforePopups();

		public List<? extends PopupInfo> providePopups();

		public Condition provideTarget();

		public static class Observable implements ProcessObservable {
			private final Step step;

			public Observable(Step step) {
				this.step = step;
			}

			public Step getStep() {
				return this.step;
			}

			@Override
			public String toString() {
				return step.toString();
			}
		}
	}
}
