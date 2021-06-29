package cc.alcina.framework.gwt.client.tour;

import java.util.ArrayList;
import java.util.List;

public class TourImpl implements Tour {
	private String name;

	private List<? extends Step> steps = new ArrayList<>();

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public List<? extends Step> getSteps() {
		return this.steps;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSteps(List<? extends Step> steps) {
		this.steps = steps;
	}

	public static class ConditionImpl implements Condition {
		private List<? extends Condition> conditions = new ArrayList<>();

		private Operator operator;

		private List<String> selectors = new ArrayList<>();

		@Override
		public List<? extends Condition> getConditions() {
			return this.conditions;
		}

		@Override
		public Operator getOperator() {
			return this.operator;
		}

		@Override
		public List<String> getSelectors() {
			return this.selectors;
		}

		public void setConditions(List<? extends Condition> conditions) {
			this.conditions = conditions;
		}

		public void setOperator(Operator operator) {
			this.operator = operator;
		}

		public void setSelectors(List<String> selectors) {
			this.selectors = selectors;
		}
	}

	public static class PopupInfoImpl implements PopupInfo {
		private String caption;

		private String description;

		private RelativeTo relativeTo;

		@Override
		public String getCaption() {
			return this.caption;
		}

		@Override
		public String getDescription() {
			return this.description;
		}

		@Override
		public RelativeTo getRelativeTo() {
			return this.relativeTo;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public void setRelativeTo(RelativeTo relativeTo) {
			this.relativeTo = relativeTo;
		}
	}

	public static class RelativeToImpl implements Tour.RelativeTo {
		private String element;

		private int offsetHorizontal;

		private int offsetVertical;

		private Pointer pointer;

		private int pointerRightMargin;

		private int popupFromBottom;

		private PositioningDirection positioningDirection;

		@Override
		public String getElement() {
			return this.element;
		}

		@Override
		public int getOffsetHorizontal() {
			return this.offsetHorizontal;
		}

		@Override
		public int getOffsetVertical() {
			return this.offsetVertical;
		}

		@Override
		public Pointer getPointer() {
			return this.pointer;
		}

		@Override
		public int getPointerRightMargin() {
			return this.pointerRightMargin;
		}

		@Override
		public int getPopupFromBottom() {
			return this.popupFromBottom;
		}

		@Override
		public PositioningDirection getPositioningDirection() {
			return this.positioningDirection;
		}

		public void setElement(String element) {
			this.element = element;
		}

		public void setOffsetHorizontal(int offsetHorizontal) {
			this.offsetHorizontal = offsetHorizontal;
		}

		public void setOffsetVertical(int offsetVertical) {
			this.offsetVertical = offsetVertical;
		}

		public void setPointer(Pointer pointer) {
			this.pointer = pointer;
		}

		public void setPointerRightMargin(int pointerRightMargin) {
			this.pointerRightMargin = pointerRightMargin;
		}

		public void setPopupFromBottom(int popupFromBottom) {
			this.popupFromBottom = popupFromBottom;
		}

		public void setPositioningDirection(
				PositioningDirection positioningDirection) {
			this.positioningDirection = positioningDirection;
		}
	}

	public static class StepImpl implements Tour.Step {
		private Action action;

		private String actionValue;

		private Condition ignoreActionIf;

		private Condition ignoreIf;

		private List<? extends PopupInfo> popups = new ArrayList<>();

		private Condition target;

		private Condition waitFor;

		@Override
		public Action getAction() {
			return this.action;
		}

		@Override
		public String getActionValue() {
			return this.actionValue;
		}

		@Override
		public Condition getIgnoreActionIf() {
			return this.ignoreActionIf;
		}

		@Override
		public Condition getIgnoreIf() {
			return this.ignoreIf;
		}

		@Override
		public List<? extends PopupInfo> getPopups() {
			return this.popups;
		}

		@Override
		public Condition getTarget() {
			return this.target;
		}

		@Override
		public Condition getWaitFor() {
			return this.waitFor;
		}

		public void setAction(Action action) {
			this.action = action;
		}

		public void setActionValue(String actionValue) {
			this.actionValue = actionValue;
		}

		public void setIgnoreActionIf(Condition ignoreActionIf) {
			this.ignoreActionIf = ignoreActionIf;
		}

		public void setIgnoreIf(Condition ignoreIf) {
			this.ignoreIf = ignoreIf;
		}

		public void setPopups(List<? extends PopupInfo> popups) {
			this.popups = popups;
		}

		public void setTarget(Condition target) {
			this.target = target;
		}

		public void setWaitFor(Condition waitFor) {
			this.waitFor = waitFor;
		}
	}
}
