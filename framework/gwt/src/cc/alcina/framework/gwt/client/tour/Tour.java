package cc.alcina.framework.gwt.client.tour;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.ClientUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class Tour extends JavaScriptObject {
	protected Tour() {
	}

	static class RelativeTo extends JavaScriptObject {
		final public native String getElement()/*-{
			return this.element;
		}-*/;

		final native String getPointerString()/*-{
			return this.pointer;
		}-*/;

		private final native String getDirectionString()/*-{
			return this.direction;
		}-*/;

		final Pointer getPointer() {
			return CommonUtils.getEnumValueOrNull(Pointer.class,
					getPointerString(), true, Pointer.CENTER_UP);
		}

		final PositioningDirection getPositioningDirection() {
			return CommonUtils.getEnumValueOrNull(PositioningDirection.class,
					getDirectionString(), true,
					PositioningDirection.LEFT_BOTTOM);
		}

		final public native int getOffsetHorizontal()/*-{
			return (this.offsetHorizontal) ? this.offsetHorizontal : 0;
		}-*/;

		final public native int getOffsetVertical()/*-{
			return (this.offsetVertical) ? this.offsetVertical : 0;
		}-*/;

		protected RelativeTo() {
		}

		final public native int getPopupFromBottom()/*-{
			return (this.popupFromBottom) ? this.popupFromBottom : 0;
		}-*/;

		final public native int getPointerRightMargin()/*-{
			return (this.pointerRightMargin) ? this.pointerRightMargin : 0;
		}-*/;
	}

	@ClientInstantiable
	public static enum PositioningDirection {
		CENTER_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, RIGHT_TOP
	}

	@ClientInstantiable
	public static enum TourAction {
		CLICK, SET_TEXT, NONE
	}

	@ClientInstantiable
	public static enum Pointer {
		CENTER_UP, LEFT_UP, CENTER_DOWN, RIGHT_UP, RIGHT_DOWN
	}

	public static enum Operator {
		AND, OR, NOT
	}

	static class Condition extends JavaScriptObject {
		final private native JsArrayString getJsSelectors()/*-{
			if (!(this.selectors)) {
				return [];
			}
			if (typeof this.selectors == "string") {
				return [ this.selectors ];
			}
			return this.selectors;
		}-*/;

		final List<String> getSelectors() {
			return ClientUtils.jsStringArrayAsStringList(getJsSelectors());
		}

		final private native JsArray<Condition> getConditionsArray()/*-{
			return (this.conditions) ? this.conditions : [];
		}-*/;

		final List<Condition> getConditions() {
			return ClientUtils.jsArrayToTypedArray(getConditionsArray());
		}

		final private native String getOperatorString()/*-{
			return this.action;
		}-*/;

		final Operator getOperator() {
			return CommonUtils.getEnumValueOrNull(Operator.class,
					getOperatorString(), true, Operator.AND);
		}

		protected Condition() {
		}
	}

	static class PopupInfo extends JavaScriptObject {
		final public native String getCaption()/*-{
			return this.caption;
		}-*/;

		final public native String getDescription()/*-{
			return this.description;
		}-*/;

		final public native RelativeTo getRelativeTo()/*-{
			return this.relativeTo;
		}-*/;

		protected PopupInfo() {
		}
	}

	static class Step extends JavaScriptObject {
		final public native Condition getTarget()/*-{
			if (!(this.target)) {
				return null;
			}
			return {
				"selectors" : this.target
			}
		}-*/;

		final List<PopupInfo> getPopups() {
			return ClientUtils.jsArrayToTypedArray(getPopupsArray());
		}

		final private native JsArray<PopupInfo> getPopupsArray()/*-{
			return (this.popups) ? this.popups : [ {
				caption : this.caption,
				description : this.description,
				relativeTo : this.relativeTo

			} ];
		}-*/;

		final public native Condition getIgnoreIf()/*-{
			return this.ignoreIf;
		}-*/;

		final public native Condition getIgnoreActionIf()/*-{
			return this.ignoreActionIf;
		}-*/;

		final public native Condition getWaitFor()/*-{
			return this.waitFor;
		}-*/;

		final private native String getActionString()/*-{
			return this.action;
		}-*/;

		final TourAction getAction() {
			return CommonUtils.getEnumValueOrNull(TourAction.class,
					getActionString(), true, TourAction.NONE);
		}

		protected Step() {
		}

		final public native String getActionValue()/*-{
			return this.actionValue;
		}-*/;

		final public native String asString()
		/*-{
			return JSON.stringify(this);
		}-*/;
	}

	final native String getName()/*-{
		return this.name;
	}-*/;

	final native JsArray<Step> getSteps() /*-{
		return this.steps;
	}-*/;

	static native Tour fromJson(String tourJson) /*-{
		var v = JSON.parse(tourJson);
		return v;
	}-*/;
}
