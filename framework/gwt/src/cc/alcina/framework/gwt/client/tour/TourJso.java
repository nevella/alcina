package cc.alcina.framework.gwt.client.tour;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public final class TourJso extends JavaScriptObject implements Tour {
	static native TourJso fromJson(String tourJson) /*-{
    var v = JSON.parse(tourJson);
    return v;
	}-*/;

	protected TourJso() {
	}

	@Override
	public final native String getName()/*-{
    return this.name;
	}-*/;

	@Override
	public List<? extends Tour.Step> getSteps() {
		return ClientUtils.jsArrayToTypedArray(getStepsArray());
	}

	final native JsArray<StepJso> getStepsArray() /*-{
    return this.steps;
	}-*/;

	static final class ConditionJso extends JavaScriptObject
			implements Tour.Condition {
		protected ConditionJso() {
		}

		@Override
		public final List<? extends Tour.Condition> getConditions() {
			return ClientUtils.jsArrayToTypedArray(getConditionsArray());
		}

		final private native JsArray<ConditionJso> getConditionsArray()/*-{
      return (this.conditions) ? this.conditions : [];
		}-*/;

		@Override
		public final native String getEvaluatorClassName()/*-{
      return this.evaluatorClassName;
		}-*/;

		final private native JsArrayString getJsSelectors()/*-{
      if (!(this.selectors)) {
        return [];
      }
      if (typeof this.selectors == "string") {
        return [ this.selectors ];
      }
      return this.selectors;
		}-*/;

		@Override
		public final Tour.Operator getOperator() {
			return CommonUtils.getEnumValueOrNull(Tour.Operator.class,
					getOperatorString(), true, Tour.Operator.AND);
		}

		final private native String getOperatorString()/*-{
      return this.action;
		}-*/;

		@Override
		public final List<String> getSelectors() {
			return ClientUtils.jsStringArrayAsStringList(getJsSelectors());
		}
	}

	static final class PopupInfoJso extends JavaScriptObject
			implements Tour.PopupInfo {
		protected PopupInfoJso() {
		}

		@Override
		final public native String getCaption()/*-{
      return this.caption;
		}-*/;

		@Override
		final public native String getDescription()/*-{
      return this.description;
		}-*/;

		@Override
		final public native RelativeToJso getRelativeTo()/*-{
      return this.relativeTo;
		}-*/;

		@Override
		final public native String getStyle()/*-{
      return this.style;
		}-*/;
	}

	static final class RelativeToJso extends JavaScriptObject
			implements Tour.RelativeTo {
		protected RelativeToJso() {
		}

		@Override
		public final Tour.PositioningDirection getDirection() {
			return CommonUtils.getEnumValueOrNull(
					Tour.PositioningDirection.class, getDirectionString(), true,
					Tour.PositioningDirection.LEFT_BOTTOM);
		}

		private final native String getDirectionString()/*-{
      return this.direction;
		}-*/;

		@Override
		final public native String getElement()/*-{
      return this.element;
		}-*/;

		@Override
		final public native int getOffsetHorizontal()/*-{
      return (this.offsetHorizontal) ? this.offsetHorizontal : 0;
		}-*/;

		@Override
		final public native int getOffsetVertical()/*-{
      return (this.offsetVertical) ? this.offsetVertical : 0;
		}-*/;

		@Override
		public final Tour.Pointer getPointer() {
			return CommonUtils.getEnumValueOrNull(Tour.Pointer.class,
					getPointerString(), true, Tour.Pointer.CENTER_UP);
		}

		@Override
		final public native int getPointerRightMargin()/*-{
      return (this.pointerRightMargin) ? this.pointerRightMargin : 0;
		}-*/;

		final native String getPointerString()/*-{
      return this.pointer;
		}-*/;

		@Override
		final public native int getPopupFromBottom()/*-{
      return (this.popupFromBottom) ? this.popupFromBottom : 0;
		}-*/;

		@Override
		final public native boolean isBubble()/*-{
      return !!this.bubble;
		}-*/;

		@Override
		final public native boolean isStepTarget()/*-{
      return !!this.stepTarget;
		}-*/;
	}

	static final class StepJso extends JavaScriptObject implements Tour.Step {
		protected StepJso() {
		}

		@Override
		final public native String asString()
		/*-{
		return JSON.stringify(this);
		}-*/;

		@Override
		public final Tour.Action getAction() {
			return CommonUtils.getEnumValueOrNull(Tour.Action.class,
					getActionString(), true, Tour.Action.TEST);
		}

		@Override
		final public native int getActionDelay()/*-{
      return this.actionDelay || 0;
		}-*/;

		final private native String getActionString()/*-{
      return this.action;
		}-*/;

		@Override
		final public native String getActionValue()/*-{
      return this.actionValue;
		}-*/;

		@Override
		final public native String getComment()/*-{
      return this.comment;
		}-*/;

		@Override
		final public native int getDelay()/*-{
      return this.delay || 0;
		}-*/;

		@Override
		final public native ConditionJso getIgnoreActionIf()/*-{
      return this.ignoreActionIf;
		}-*/;

		@Override
		final public native ConditionJso getIgnoreIf()/*-{
      return this.ignoreIf;
		}-*/;

		final private native JsArray<PopupInfoJso> getPopupsArray()/*-{
      return (this.popups) ? this.popups : [ {
        caption : this.caption,
        description : this.description,
        relativeTo : this.relativeTo,
        style : this.style

      } ];
		}-*/;

		@Override
		final public native ConditionJso getWaitFor()/*-{
      return this.waitFor;
		}-*/;

		@Override
		final public native boolean isActionBeforePopups()/*-{
      return this.actionBeforePopups === true;
		}-*/;

		@Override
		public final List<? extends Tour.PopupInfo> providePopups() {
			return ClientUtils.jsArrayToTypedArray(getPopupsArray());
		}

		@Override
		final public native ConditionJso provideTarget()/*-{
      if (!(this.target)) {
        return null;
      }
      return {
        "selectors" : this.target
      }
		}-*/;
	}
}
