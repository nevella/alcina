package cc.alcina.framework.gwt.client.tour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;

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

		private String evaluatorClassName;

		public boolean contains(String s) {
			return toString().contains(s);
		}

		@Override
		public List<? extends Condition> getConditions() {
			return this.conditions;
		}

		@Override
		public String getEvaluatorClassName() {
			return this.evaluatorClassName;
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

		public void setEvaluatorClassName(String evaluatorClassName) {
			this.evaluatorClassName = evaluatorClassName;
		}

		public void setOperator(Operator operator) {
			this.operator = operator;
		}

		public void setSelectors(List<String> selectors) {
			this.selectors = selectors;
		}

		@Override
		public String toString() {
			return Ax.format("%s - %s - %s - %s", operator, selectors,
					conditions, evaluatorClassName);
		}

		public ConditionImpl withSelector(String target) {
			selectors.add(target);
			return this;
		}
	}

	public static class PopupInfoImpl implements PopupInfo {
		private String caption;

		private String description;

		private RelativeTo relativeTo;

		private String style;

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

		@Override
		public String getStyle() {
			return this.style;
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

		public void setStyle(String style) {
			this.style = style;
		}
	}

	public static class RelativeToImpl implements Tour.RelativeTo {
		private String element;

		private int offsetHorizontal;

		private int offsetVertical;

		private Pointer pointer;

		private int pointerRightMargin;

		private int popupFromBottom;

		private PositioningDirection direction;

		private boolean bubble = true;

		private boolean stepTarget;

		@Override
		public PositioningDirection getDirection() {
			return this.direction;
		}

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
		public boolean isBubble() {
			return this.bubble;
		}

		@Override
		public boolean isStepTarget() {
			return this.stepTarget;
		}

		public void setBubble(boolean bubble) {
			this.bubble = bubble;
		}

		public void setDirection(PositioningDirection direction) {
			this.direction = direction;
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

		public void setStepTarget(boolean stepTarget) {
			this.stepTarget = stepTarget;
		}

		@Override
		public String toString() {
			return element;
		}
	}

	public static class StepImpl implements Tour.Step, Tour.PopupInfo {
		private Action action;

		private String actionValue;

		private Condition ignoreActionIf;

		private Condition ignoreIf;

		private List<? extends PopupInfo> popups = new ArrayList<>();

		private String target;

		private Condition waitFor;

		private String caption;

		private String description;

		private int delay;

		private int actionDelay;

		private RelativeTo relativeTo;

		private String style;

		private String comment;

		private boolean actionBeforePopups;

		@Override
		public String asString() {
			return toString();
		}

		@Override
		public Action getAction() {
			return this.action;
		}

		@Override
		public int getActionDelay() {
			return this.actionDelay;
		}

		@Override
		public String getActionValue() {
			return this.actionValue;
		}

		@Override
		public String getCaption() {
			return this.caption;
		}

		@Override
		public String getComment() {
			return this.comment;
		}

		@Override
		public int getDelay() {
			return this.delay;
		}

		@Override
		// blank for no popup
		public String getDescription() {
			return this.description;
		}

		@Override
		public Condition getIgnoreActionIf() {
			return this.ignoreActionIf;
		}

		@Override
		public Condition getIgnoreIf() {
			return this.ignoreIf;
		}

		public List<? extends PopupInfo> getPopups() {
			return this.popups;
		}

		@Override
		public RelativeTo getRelativeTo() {
			return this.relativeTo;
		}

		@Override
		public String getStyle() {
			return this.style;
		}

		public String getTarget() {
			return this.target;
		}

		@Override
		public Condition getWaitFor() {
			return this.waitFor;
		}

		@Override
		public boolean isActionBeforePopups() {
			return this.actionBeforePopups;
		}

		@Override
		public List<? extends PopupInfo> providePopups() {
			return popups.size() > 0 ? popups
					: description == null ? Collections.emptyList()
							: Collections.singletonList(this);
		}

		@Override
		public Condition provideTarget() {
			return this.target == null ? null
					: new ConditionImpl().withSelector(this.target);
		}

		public void setAction(Action action) {
			this.action = action;
		}

		public void setActionBeforePopups(boolean actionBeforePopups) {
			this.actionBeforePopups = actionBeforePopups;
		}

		public void setActionDelay(int actionDelay) {
			this.actionDelay = actionDelay;
		}

		public void setActionValue(String actionValue) {
			this.actionValue = actionValue;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}

		public void setDescription(String description) {
			this.description = description;
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

		public void setRelativeTo(RelativeTo relativeTo) {
			this.relativeTo = relativeTo;
		}

		public void setStyle(String style) {
			this.style = style;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public void setWaitFor(Condition waitFor) {
			this.waitFor = waitFor;
		}

		@Override
		public String toString() {
			if (Ax.isBlank(getCaption())) {
				return Ax.format("Non-UI: %s : %s : %s", getComment(),
						getAction(), getTarget());
			} else {
				String commentSuffix = Ax.isBlank(getComment()) ? ""
						: " - " + getComment();
				return Ax.format("%s : %s%s", getCaption(), getRelativeTo(),
						commentSuffix);
			}
		}
	}
}
