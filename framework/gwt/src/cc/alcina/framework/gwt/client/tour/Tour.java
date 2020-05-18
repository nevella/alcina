package cc.alcina.framework.gwt.client.tour;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class Tour extends JavaScriptObject {
	static native Tour fromJson(String tourJson) /*-{
    var v = JSON.parse(tourJson);
    return v;
	}-*/;

	protected Tour() {
	}

	final native String getName()/*-{
    return this.name;
	}-*/;

	final native JsArray<Step> getSteps() /*-{
    return this.steps;
	}-*/;

	public static enum Operator {
		AND, OR, NOT
	}

	@ClientInstantiable
	public static enum Pointer {
		CENTER_UP, LEFT_UP, CENTER_DOWN, RIGHT_UP, RIGHT_DOWN
	}

	@ClientInstantiable
	public static enum PositioningDirection {
		CENTER_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, RIGHT_TOP
	}

	@ClientInstantiable
	public static enum TourAction {
		CLICK, SET_TEXT, NONE
	}

	static class Condition extends JavaScriptObject {
		protected Condition() {
		}

		final private native JsArray<Condition> getConditionsArray()/*-{
      return (this.conditions) ? this.conditions : [];
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

		final private native String getOperatorString()/*-{
      return this.action;
		}-*/;

		final List<Condition> getConditions() {
			return ClientUtils.jsArrayToTypedArray(getConditionsArray());
		}

		final Operator getOperator() {
			return CommonUtils.getEnumValueOrNull(Operator.class,
					getOperatorString(), true, Operator.AND);
		}

		final List<String> getSelectors() {
			return ClientUtils.jsStringArrayAsStringList(getJsSelectors());
		}
	}

	static class PopupInfo extends JavaScriptObject {
		protected PopupInfo() {
		}

		final public native String getCaption()/*-{
      return this.caption;
		}-*/;

		final public native String getDescription()/*-{
      return this.description;
		}-*/;

		final public native RelativeTo getRelativeTo()/*-{
      return this.relativeTo;
		}-*/;
	}

	static class RelativeTo extends JavaScriptObject {
		protected RelativeTo() {
		}

		final public native String getElement()/*-{
      return this.element;
		}-*/;

		final public native int getOffsetHorizontal()/*-{
      return (this.offsetHorizontal) ? this.offsetHorizontal : 0;
		}-*/;

		final public native int getOffsetVertical()/*-{
      return (this.offsetVertical) ? this.offsetVertical : 0;
		}-*/;

		final public native int getPointerRightMargin()/*-{
      return (this.pointerRightMargin) ? this.pointerRightMargin : 0;
		}-*/;

		final public native int getPopupFromBottom()/*-{
      return (this.popupFromBottom) ? this.popupFromBottom : 0;
		}-*/;

		private final native String getDirectionString()/*-{
      return this.direction;
		}-*/;

		final Pointer getPointer() {
			return CommonUtils.getEnumValueOrNull(Pointer.class,
					getPointerString(), true, Pointer.CENTER_UP);
		}

		final native String getPointerString()/*-{
      return this.pointer;
		}-*/;

		final PositioningDirection getPositioningDirection() {
			return CommonUtils.getEnumValueOrNull(PositioningDirection.class,
					getDirectionString(), true,
					PositioningDirection.LEFT_BOTTOM);
		}
	}

	static class Step extends JavaScriptObject {
		protected Step() {
		}

		final public native String asString()
		/*-{
      return JSON.stringify(this);
		}-*/;

		final public native String getActionValue()/*-{
      return this.actionValue;
		}-*/;

		final public native Condition getIgnoreActionIf()/*-{
      return this.ignoreActionIf;
		}-*/;

		final public native Condition getIgnoreIf()/*-{
      return this.ignoreIf;
		}-*/;

		final public native Condition getTarget()/*-{
      if (!(this.target)) {
        return null;
      }
      return {
        "selectors" : this.target
      }
		}-*/;

		final public native Condition getWaitFor()/*-{
      return this.waitFor;
		}-*/;

		final private native String getActionString()/*-{
      return this.action;
		}-*/;

		final private native JsArray<PopupInfo> getPopupsArray()/*-{
      return (this.popups) ? this.popups : [ {
        caption : this.caption,
        description : this.description,
        relativeTo : this.relativeTo

      } ];
		}-*/;

		final TourAction getAction() {
			return CommonUtils.getEnumValueOrNull(TourAction.class,
					getActionString(), true, TourAction.NONE);
		}

		final List<PopupInfo> getPopups() {
			return ClientUtils.jsArrayToTypedArray(getPopupsArray());
		}
	}
}
