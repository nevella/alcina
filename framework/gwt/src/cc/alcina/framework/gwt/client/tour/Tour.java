package cc.alcina.framework.gwt.client.tour;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;

public interface Tour {
	public String getName();

	public List<? extends Tour.Step> getSteps();

	@Reflected
	enum Action {
		CLICK, SET_TEXT, NONE, SCRIPT, SELECT, EVAL, TEST
	}

	interface Condition {
		List<? extends Condition> getConditions();

		String getEvaluatorClassName();

		Operator getOperator();

		List<String> getSelectors();

		default Optional<ConditionEvaluator> provideEvaluator() {
			if (getEvaluatorClassName() == null) {
				return Optional.empty();
			} else {
				Class<ConditionEvaluator> evaluatorClass = Reflections
						.forName(getEvaluatorClassName());
				return Optional.of(Reflections.newInstance(evaluatorClass));
			}
		}

		default String soleSelector() {
			Preconditions.checkState(getSelectors().size() == 1);
			return getSelectors().get(0);
		}
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
		BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, BOTTOM_CENTER
	}

	interface RelativeTo {
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

		default String provideElement(Step currentStep) {
			if (isStepTarget()) {
				return currentStep.provideTarget().soleSelector();
			} else {
				return getElement();
			}
		}
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

		public List<? extends PopupInfo> providePopups();

		public Condition provideTarget();
	}
}
