package cc.alcina.framework.gwt.client.tour;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

public interface Tour {
	public String getName();

	public List<? extends Tour.Step> getSteps();

	@ClientInstantiable
	enum Action {
		CLICK, SET_TEXT, NONE
	}

	interface Condition {
		List<? extends Condition> getConditions();

		Operator getOperator();

		List<String> getSelectors();
	}

	enum Operator {
		AND, OR, NOT
	}

	@ClientInstantiable
	enum Pointer {
		CENTER_UP, LEFT_UP, CENTER_DOWN, RIGHT_UP, RIGHT_DOWN
	}

	interface PopupInfo {
		public String getCaption();

		public String getDescription();

		public RelativeTo getRelativeTo();
	}

	@ClientInstantiable
	enum PositioningDirection {
		CENTER_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, RIGHT_TOP, TOP_LEFT
	}

	interface RelativeTo {
		public String getElement();

		public int getOffsetHorizontal();

		public int getOffsetVertical();

		public Pointer getPointer();

		public int getPointerRightMargin();

		public int getPopupFromBottom();

		public PositioningDirection getPositioningDirection();
	}

	// @JsonDeserialize(as = StepImpl.class)
	interface Step {
		public Action getAction();

		public String getActionValue();

		public Condition getIgnoreActionIf();

		public Condition getIgnoreIf();

		public List<? extends PopupInfo> getPopups();

		public Condition getTarget();

		public Condition getWaitFor();
	}
}
