package com.google.gwt.dom.client;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Clear;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.FontStyle;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.ListStyleType;
import com.google.gwt.dom.client.Style.OutlineStyle;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.TableLayout;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.TextDecoration;
import com.google.gwt.dom.client.Style.TextJustify;
import com.google.gwt.dom.client.Style.TextOverflow;
import com.google.gwt.dom.client.Style.TextTransform;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.Style.WhiteSpace;

public final class StyleJso extends JavaScriptObject implements ClientDomStyle {
	protected StyleJso() {
	}

	@Override
	public final void clearBackgroundColor() {
		ClientDomStyleStatic.clearBackgroundColor(this);
	}

	@Override
	public final void clearBackgroundImage() {
		ClientDomStyleStatic.clearBackgroundImage(this);
	}

	@Override
	public final void clearBorderColor() {
		ClientDomStyleStatic.clearBorderColor(this);
	}

	@Override
	public final void clearBorderStyle() {
		ClientDomStyleStatic.clearBorderStyle(this);
	}

	@Override
	public final void clearBorderWidth() {
		ClientDomStyleStatic.clearBorderWidth(this);
	}

	@Override
	public final void clearBottom() {
		ClientDomStyleStatic.clearBottom(this);
	}

	@Override
	public final void clearClear() {
		ClientDomStyleStatic.clearClear(this);
	}

	@Override
	public final void clearColor() {
		ClientDomStyleStatic.clearColor(this);
	}

	@Override
	public final void clearCursor() {
		ClientDomStyleStatic.clearCursor(this);
	}

	@Override
	public final void clearDisplay() {
		ClientDomStyleStatic.clearDisplay(this);
	}

	@Override
	public final void clearFloat() {
		ClientDomStyleStatic.clearFloat(this);
	}

	@Override
	public final void clearFontSize() {
		ClientDomStyleStatic.clearFontSize(this);
	}

	@Override
	public final void clearFontStyle() {
		ClientDomStyleStatic.clearFontStyle(this);
	}

	@Override
	public final void clearFontWeight() {
		ClientDomStyleStatic.clearFontWeight(this);
	}

	@Override
	public final void clearHeight() {
		ClientDomStyleStatic.clearHeight(this);
	}

	@Override
	public final void clearLeft() {
		ClientDomStyleStatic.clearLeft(this);
	}

	@Override
	public final void clearLineHeight() {
		ClientDomStyleStatic.clearLineHeight(this);
	}

	@Override
	public final void clearListStyleType() {
		ClientDomStyleStatic.clearListStyleType(this);
	}

	@Override
	public final void clearMargin() {
		ClientDomStyleStatic.clearMargin(this);
	}

	@Override
	public final void clearMarginBottom() {
		ClientDomStyleStatic.clearMarginBottom(this);
	}

	@Override
	public final void clearMarginLeft() {
		ClientDomStyleStatic.clearMarginLeft(this);
	}

	@Override
	public final void clearMarginRight() {
		ClientDomStyleStatic.clearMarginRight(this);
	}

	@Override
	public final void clearMarginTop() {
		ClientDomStyleStatic.clearMarginTop(this);
	}

	@Override
	public final void clearOpacity() {
		ClientDomStyleStatic.clearOpacity(this);
	}

	@Override
	public final void clearOutlineColor() {
		ClientDomStyleStatic.clearOutlineColor(this);
	}

	@Override
	public final void clearOutlineStyle() {
		ClientDomStyleStatic.clearOutlineStyle(this);
	}

	@Override
	public final void clearOutlineWidth() {
		ClientDomStyleStatic.clearOutlineWidth(this);
	}

	@Override
	public final void clearOverflow() {
		ClientDomStyleStatic.clearOverflow(this);
	}

	@Override
	public final void clearOverflowX() {
		ClientDomStyleStatic.clearOverflowX(this);
	}

	@Override
	public final void clearOverflowY() {
		ClientDomStyleStatic.clearOverflowY(this);
	}

	@Override
	public final void clearPadding() {
		ClientDomStyleStatic.clearPadding(this);
	}

	@Override
	public final void clearPaddingBottom() {
		ClientDomStyleStatic.clearPaddingBottom(this);
	}

	@Override
	public final void clearPaddingLeft() {
		ClientDomStyleStatic.clearPaddingLeft(this);
	}

	@Override
	public final void clearPaddingRight() {
		ClientDomStyleStatic.clearPaddingRight(this);
	}

	@Override
	public final void clearPaddingTop() {
		ClientDomStyleStatic.clearPaddingTop(this);
	}

	@Override
	public final void clearPosition() {
		ClientDomStyleStatic.clearPosition(this);
	}

	@Override
	public final void clearProperty(String name) {
		ClientDomStyleStatic.clearProperty(this, name);
	}

	@Override
	public final void clearRight() {
		ClientDomStyleStatic.clearRight(this);
	}

	@Override
	public final void clearTableLayout() {
		ClientDomStyleStatic.clearTableLayout(this);
	}

	@Override
	public final void clearTextAlign() {
		ClientDomStyleStatic.clearTextAlign(this);
	}

	@Override
	public final void clearTextDecoration() {
		ClientDomStyleStatic.clearTextDecoration(this);
	}

	@Override
	public final void clearTextIndent() {
		ClientDomStyleStatic.clearTextIndent(this);
	}

	@Override
	public final void clearTextJustify() {
		ClientDomStyleStatic.clearTextJustify(this);
	}

	@Override
	public final void clearTextOverflow() {
		ClientDomStyleStatic.clearTextOverflow(this);
	}

	@Override
	public final void clearTextTransform() {
		ClientDomStyleStatic.clearTextTransform(this);
	}

	@Override
	public final void clearTop() {
		ClientDomStyleStatic.clearTop(this);
	}

	@Override
	public final void clearVisibility() {
		ClientDomStyleStatic.clearVisibility(this);
	}

	@Override
	public final void clearWhiteSpace() {
		ClientDomStyleStatic.clearWhiteSpace(this);
	}

	@Override
	public final void clearWidth() {
		ClientDomStyleStatic.clearWidth(this);
	}

	@Override
	public final void clearZIndex() {
		ClientDomStyleStatic.clearZIndex(this);
	}

	@Override
	public final String getBackgroundColor() {
		return ClientDomStyleStatic.getBackgroundColor(this);
	}

	@Override
	public final String getBackgroundImage() {
		return ClientDomStyleStatic.getBackgroundImage(this);
	}

	@Override
	public final String getBorderColor() {
		return ClientDomStyleStatic.getBorderColor(this);
	}

	@Override
	public final String getBorderStyle() {
		return ClientDomStyleStatic.getBorderStyle(this);
	}

	@Override
	public final String getBorderWidth() {
		return ClientDomStyleStatic.getBorderWidth(this);
	}

	@Override
	public final String getBottom() {
		return ClientDomStyleStatic.getBottom(this);
	}

	@Override
	public final String getClear() {
		return ClientDomStyleStatic.getClear(this);
	}

	@Override
	public final String getColor() {
		return ClientDomStyleStatic.getColor(this);
	}

	@Override
	public final String getCursor() {
		return ClientDomStyleStatic.getCursor(this);
	}

	@Override
	public final String getDisplay() {
		return ClientDomStyleStatic.getDisplay(this);
	}

	@Override
	public final Style.Display getDisplayTyped() {
		return ClientDomStyleStatic.getDisplayTyped(this);
	}

	@Override
	public final String getFontSize() {
		return ClientDomStyleStatic.getFontSize(this);
	}

	@Override
	public final String getFontStyle() {
		return ClientDomStyleStatic.getFontStyle(this);
	}

	@Override
	public final String getFontWeight() {
		return ClientDomStyleStatic.getFontWeight(this);
	}

	@Override
	public final String getHeight() {
		return ClientDomStyleStatic.getHeight(this);
	}

	@Override
	public final String getLeft() {
		return ClientDomStyleStatic.getLeft(this);
	}

	@Override
	public final String getLineHeight() {
		return ClientDomStyleStatic.getLineHeight(this);
	}

	@Override
	public final String getListStyleType() {
		return ClientDomStyleStatic.getListStyleType(this);
	}

	@Override
	public final String getMargin() {
		return ClientDomStyleStatic.getMargin(this);
	}

	@Override
	public final String getMarginBottom() {
		return ClientDomStyleStatic.getMarginBottom(this);
	}

	@Override
	public final String getMarginLeft() {
		return ClientDomStyleStatic.getMarginLeft(this);
	}

	@Override
	public final String getMarginRight() {
		return ClientDomStyleStatic.getMarginRight(this);
	}

	@Override
	public final String getMarginTop() {
		return ClientDomStyleStatic.getMarginTop(this);
	}

	@Override
	public final String getOpacity() {
		return ClientDomStyleStatic.getOpacity(this);
	}

	@Override
	public final String getOverflow() {
		return ClientDomStyleStatic.getOverflow(this);
	}

	@Override
	public final String getOverflowX() {
		return ClientDomStyleStatic.getOverflowX(this);
	}

	@Override
	public final String getOverflowY() {
		return ClientDomStyleStatic.getOverflowY(this);
	}

	@Override
	public final String getPadding() {
		return ClientDomStyleStatic.getPadding(this);
	}

	@Override
	public final String getPaddingBottom() {
		return ClientDomStyleStatic.getPaddingBottom(this);
	}

	@Override
	public final String getPaddingLeft() {
		return ClientDomStyleStatic.getPaddingLeft(this);
	}

	@Override
	public final String getPaddingRight() {
		return ClientDomStyleStatic.getPaddingRight(this);
	}

	@Override
	public final String getPaddingTop() {
		return ClientDomStyleStatic.getPaddingTop(this);
	}

	@Override
	public final String getPosition() {
		return ClientDomStyleStatic.getPosition(this);
	}

	@Override
	public final Style.Position getPositionTyped() {
		return ClientDomStyleStatic.getPositionTyped(this);
	}

	@Override
	public Map<String, String> getProperties() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String getProperty(String name) {
		return ClientDomStyleStatic.getProperty(this, name);
	}

	@Override
	// called for direct StyleRemote (computedStyle e.g.) access
	public final native String getPropertyImpl(String name) /*-{
    return this[name];
	}-*/;

	@Override
	public final String getRight() {
		return ClientDomStyleStatic.getRight(this);
	}

	@Override
	public final String getTableLayout() {
		return ClientDomStyleStatic.getTableLayout(this);
	}

	@Override
	public final String getTextAlign() {
		return ClientDomStyleStatic.getTextAlign(this);
	}

	@Override
	public final String getTextDecoration() {
		return ClientDomStyleStatic.getTextDecoration(this);
	}

	@Override
	public final String getTextIndent() {
		return ClientDomStyleStatic.getTextIndent(this);
	}

	@Override
	public final String getTextJustify() {
		return ClientDomStyleStatic.getTextJustify(this);
	}

	@Override
	public final String getTextOverflow() {
		return ClientDomStyleStatic.getTextOverflow(this);
	}

	@Override
	public final String getTextTransform() {
		return ClientDomStyleStatic.getTextTransform(this);
	}

	@Override
	public final String getTop() {
		return ClientDomStyleStatic.getTop(this);
	}

	@Override
	public final String getVerticalAlign() {
		return ClientDomStyleStatic.getVerticalAlign(this);
	}

	@Override
	public final String getVisibility() {
		return ClientDomStyleStatic.getVisibility(this);
	}

	@Override
	public final String getWhiteSpace() {
		return ClientDomStyleStatic.getWhiteSpace(this);
	}

	@Override
	public final String getWidth() {
		return ClientDomStyleStatic.getWidth(this);
	}

	@Override
	public final String getZIndex() {
		return ClientDomStyleStatic.getZIndex(this);
	}

	@Override
	public final void setBackgroundColor(String value) {
		ClientDomStyleStatic.setBackgroundColor(this, value);
	}

	@Override
	public final void setBackgroundImage(String value) {
		ClientDomStyleStatic.setBackgroundImage(this, value);
	}

	@Override
	public final void setBorderColor(String value) {
		ClientDomStyleStatic.setBorderColor(this, value);
	}

	@Override
	public final void setBorderStyle(BorderStyle value) {
		ClientDomStyleStatic.setBorderStyle(this, value);
	}

	@Override
	public final void setBorderWidth(double value, Unit unit) {
		ClientDomStyleStatic.setBorderWidth(this, value, unit);
	}

	@Override
	public final void setBottom(double value, Unit unit) {
		ClientDomStyleStatic.setBottom(this, value, unit);
	}

	@Override
	public final void setClear(Clear value) {
		ClientDomStyleStatic.setClear(this, value);
	}

	@Override
	public final void setColor(String value) {
		ClientDomStyleStatic.setColor(this, value);
	}

	@Override
	public final void setCursor(Cursor value) {
		ClientDomStyleStatic.setCursor(this, value);
	}

	@Override
	public final void setDisplay(Display value) {
		ClientDomStyleStatic.setDisplay(this, value);
	}

	@Override
	public final void setFloat(Float value) {
		ClientDomStyleStatic.setFloat(this, value);
	}

	@Override
	public final void setFontSize(double value, Unit unit) {
		ClientDomStyleStatic.setFontSize(this, value, unit);
	}

	@Override
	public final void setFontStyle(FontStyle value) {
		ClientDomStyleStatic.setFontStyle(this, value);
	}

	@Override
	public final void setFontWeight(FontWeight value) {
		ClientDomStyleStatic.setFontWeight(this, value);
	}

	@Override
	public final void setHeight(double value, Unit unit) {
		ClientDomStyleStatic.setHeight(this, value, unit);
	}

	@Override
	public final void setLeft(double value, Unit unit) {
		ClientDomStyleStatic.setLeft(this, value, unit);
	}

	@Override
	public final void setLineHeight(double value, Unit unit) {
		ClientDomStyleStatic.setLineHeight(this, value, unit);
	}

	@Override
	public final void setListStyleType(ListStyleType value) {
		ClientDomStyleStatic.setListStyleType(this, value);
	}

	@Override
	public final void setMargin(double value, Unit unit) {
		ClientDomStyleStatic.setMargin(this, value, unit);
	}

	@Override
	public final void setMarginBottom(double value, Unit unit) {
		ClientDomStyleStatic.setMarginBottom(this, value, unit);
	}

	@Override
	public final void setMarginLeft(double value, Unit unit) {
		ClientDomStyleStatic.setMarginLeft(this, value, unit);
	}

	@Override
	public final void setMarginRight(double value, Unit unit) {
		ClientDomStyleStatic.setMarginRight(this, value, unit);
	}

	@Override
	public final void setMarginTop(double value, Unit unit) {
		ClientDomStyleStatic.setMarginTop(this, value, unit);
	}

	@Override
	public final void setOpacity(String value) {
		ClientDomStyleStatic.setOpacity(this, value);
	}

	@Override
	public final void setOutlineColor(String value) {
		ClientDomStyleStatic.setOutlineColor(this, value);
	}

	@Override
	public final void setOutlineStyle(OutlineStyle value) {
		ClientDomStyleStatic.setOutlineStyle(this, value);
	}

	@Override
	public final void setOutlineWidth(double value, Unit unit) {
		ClientDomStyleStatic.setOutlineWidth(this, value, unit);
	}

	@Override
	public final void setOverflow(Overflow value) {
		ClientDomStyleStatic.setOverflow(this, value);
	}

	@Override
	public final void setOverflowX(Overflow value) {
		ClientDomStyleStatic.setOverflowX(this, value);
	}

	@Override
	public final void setOverflowY(Overflow value) {
		ClientDomStyleStatic.setOverflowY(this, value);
	}

	@Override
	public final void setPadding(double value, Unit unit) {
		ClientDomStyleStatic.setPadding(this, value, unit);
	}

	@Override
	public final void setPaddingBottom(double value, Unit unit) {
		ClientDomStyleStatic.setPaddingBottom(this, value, unit);
	}

	@Override
	public final void setPaddingLeft(double value, Unit unit) {
		ClientDomStyleStatic.setPaddingLeft(this, value, unit);
	}

	@Override
	public final void setPaddingRight(double value, Unit unit) {
		ClientDomStyleStatic.setPaddingRight(this, value, unit);
	}

	@Override
	public final void setPaddingTop(double value, Unit unit) {
		ClientDomStyleStatic.setPaddingTop(this, value, unit);
	}

	@Override
	public final void setPosition(Position value) {
		ClientDomStyleStatic.setPosition(this, value);
	}

	@Override
	public final void setProperty(String name, double value, Unit unit) {
		ClientDomStyleStatic.setProperty(this, name, value, unit);
	}

	@Override
	public final void setProperty(String name, String value) {
		ClientDomStyleStatic.setProperty(this, name, value);
	}

	/**
	 * Sets the value of a named property.
	 */
	@Override
	public final native void setPropertyImpl(String name, String value) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this[name] = value;
	}-*/;

	@Override
	public final void setPropertyPx(String name, int value) {
		ClientDomStyleStatic.setPropertyPx(this, name, value);
	}

	@Override
	public final void setRight(double value, Unit unit) {
		ClientDomStyleStatic.setRight(this, value, unit);
	}

	@Override
	public final void setTableLayout(TableLayout value) {
		ClientDomStyleStatic.setTableLayout(this, value);
	}

	@Override
	public final void setTextAlign(TextAlign value) {
		ClientDomStyleStatic.setTextAlign(this, value);
	}

	@Override
	public final void setTextDecoration(TextDecoration value) {
		ClientDomStyleStatic.setTextDecoration(this, value);
	}

	@Override
	public final void setTextIndent(double value, Unit unit) {
		ClientDomStyleStatic.setTextIndent(this, value, unit);
	}

	@Override
	public final void setTextJustify(TextJustify value) {
		ClientDomStyleStatic.setTextJustify(this, value);
	}

	@Override
	public final void setTextOverflow(TextOverflow value) {
		ClientDomStyleStatic.setTextOverflow(this, value);
	}

	@Override
	public final void setTextTransform(TextTransform value) {
		ClientDomStyleStatic.setTextTransform(this, value);
	}

	@Override
	public final void setTop(double value, Unit unit) {
		ClientDomStyleStatic.setTop(this, value, unit);
	}

	@Override
	public final void setVerticalAlign(double value, Unit unit) {
		ClientDomStyleStatic.setVerticalAlign(this, value, unit);
	}

	@Override
	public final void setVerticalAlign(VerticalAlign value) {
		ClientDomStyleStatic.setVerticalAlign(this, value);
	}

	@Override
	public final void setVisibility(Visibility value) {
		ClientDomStyleStatic.setVisibility(this, value);
	}

	@Override
	public final void setWhiteSpace(WhiteSpace value) {
		ClientDomStyleStatic.setWhiteSpace(this, value);
	}

	@Override
	public final void setWidth(double value, Unit unit) {
		ClientDomStyleStatic.setWidth(this, value, unit);
	}

	@Override
	public final void setZIndex(int value) {
		ClientDomStyleStatic.setZIndex(this, value);
	}

	@Override
	public final Style styleObject() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final native void removeProperty(String key) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
     this.removeProperty(key);
	}-*/;
}