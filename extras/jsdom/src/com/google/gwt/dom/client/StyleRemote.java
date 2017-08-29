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

public final class StyleRemote extends JavaScriptObject implements DomStyle {
	protected StyleRemote() {
	}

	/**
	 * Sets the value of a named property.
	 */
	@Override
	public final native void setPropertyImpl(String name, String value) /*-{
        this[name] = value;
	}-*/;

	@Override
	public final Style styleObject() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void clearBackgroundColor() {
		DomStyleStatic.clearBackgroundColor(this);
	}

	@Override
	public final void clearBackgroundImage() {
		DomStyleStatic.clearBackgroundImage(this);
	}

	@Override
	public final void clearBorderColor() {
		DomStyleStatic.clearBorderColor(this);
	}

	@Override
	public final void clearBorderStyle() {
		DomStyleStatic.clearBorderStyle(this);
	}

	@Override
	public final void clearBorderWidth() {
		DomStyleStatic.clearBorderWidth(this);
	}

	@Override
	public final void clearBottom() {
		DomStyleStatic.clearBottom(this);
	}

	@Override
	public final void clearClear() {
		DomStyleStatic.clearClear(this);
	}

	@Override
	public final void clearColor() {
		DomStyleStatic.clearColor(this);
	}

	@Override
	public final void clearCursor() {
		DomStyleStatic.clearCursor(this);
	}

	@Override
	public final void clearDisplay() {
		DomStyleStatic.clearDisplay(this);
	}

	@Override
	public final void clearFloat() {
		DomStyleStatic.clearFloat(this);
	}

	@Override
	public final void clearFontSize() {
		DomStyleStatic.clearFontSize(this);
	}

	@Override
	public final void clearFontStyle() {
		DomStyleStatic.clearFontStyle(this);
	}

	@Override
	public final void clearFontWeight() {
		DomStyleStatic.clearFontWeight(this);
	}

	@Override
	public final void clearHeight() {
		DomStyleStatic.clearHeight(this);
	}

	@Override
	public final void clearLeft() {
		DomStyleStatic.clearLeft(this);
	}

	@Override
	public final void clearLineHeight() {
		DomStyleStatic.clearLineHeight(this);
	}

	@Override
	public final void clearListStyleType() {
		DomStyleStatic.clearListStyleType(this);
	}

	@Override
	public final void clearMargin() {
		DomStyleStatic.clearMargin(this);
	}

	@Override
	public final void clearMarginBottom() {
		DomStyleStatic.clearMarginBottom(this);
	}

	@Override
	public final void clearMarginLeft() {
		DomStyleStatic.clearMarginLeft(this);
	}

	@Override
	public final void clearMarginRight() {
		DomStyleStatic.clearMarginRight(this);
	}

	@Override
	public final void clearMarginTop() {
		DomStyleStatic.clearMarginTop(this);
	}

	@Override
	public final void clearOpacity() {
		DomStyleStatic.clearOpacity(this);
	}

	@Override
	public final void clearOutlineColor() {
		DomStyleStatic.clearOutlineColor(this);
	}

	@Override
	public final void clearOutlineStyle() {
		DomStyleStatic.clearOutlineStyle(this);
	}

	@Override
	public final void clearOutlineWidth() {
		DomStyleStatic.clearOutlineWidth(this);
	}

	@Override
	public final void clearOverflow() {
		DomStyleStatic.clearOverflow(this);
	}

	@Override
	public final void clearOverflowX() {
		DomStyleStatic.clearOverflowX(this);
	}

	@Override
	public final void clearOverflowY() {
		DomStyleStatic.clearOverflowY(this);
	}

	@Override
	public final void clearPadding() {
		DomStyleStatic.clearPadding(this);
	}

	@Override
	public final void clearPaddingBottom() {
		DomStyleStatic.clearPaddingBottom(this);
	}

	@Override
	public final void clearPaddingLeft() {
		DomStyleStatic.clearPaddingLeft(this);
	}

	@Override
	public final void clearPaddingRight() {
		DomStyleStatic.clearPaddingRight(this);
	}

	@Override
	public final void clearPaddingTop() {
		DomStyleStatic.clearPaddingTop(this);
	}

	@Override
	public final void clearPosition() {
		DomStyleStatic.clearPosition(this);
	}

	@Override
	public final void clearProperty(String name) {
		DomStyleStatic.clearProperty(this, name);
	}

	@Override
	public final void clearRight() {
		DomStyleStatic.clearRight(this);
	}

	@Override
	public final void clearTableLayout() {
		DomStyleStatic.clearTableLayout(this);
	}

	@Override
	public final void clearTextAlign() {
		DomStyleStatic.clearTextAlign(this);
	}

	@Override
	public final void clearTextDecoration() {
		DomStyleStatic.clearTextDecoration(this);
	}

	@Override
	public final void clearTextIndent() {
		DomStyleStatic.clearTextIndent(this);
	}

	@Override
	public final void clearTextJustify() {
		DomStyleStatic.clearTextJustify(this);
	}

	@Override
	public final void clearTextOverflow() {
		DomStyleStatic.clearTextOverflow(this);
	}

	@Override
	public final void clearTextTransform() {
		DomStyleStatic.clearTextTransform(this);
	}

	@Override
	public final void clearTop() {
		DomStyleStatic.clearTop(this);
	}

	@Override
	public final void clearVisibility() {
		DomStyleStatic.clearVisibility(this);
	}

	@Override
	public final void clearWhiteSpace() {
		DomStyleStatic.clearWhiteSpace(this);
	}

	@Override
	public final void clearWidth() {
		DomStyleStatic.clearWidth(this);
	}

	@Override
	public final void clearZIndex() {
		DomStyleStatic.clearZIndex(this);
	}

	@Override
	public final String getBackgroundColor() {
		return DomStyleStatic.getBackgroundColor(this);
	}

	@Override
	public final String getBackgroundImage() {
		return DomStyleStatic.getBackgroundImage(this);
	}

	@Override
	public final String getBorderColor() {
		return DomStyleStatic.getBorderColor(this);
	}

	@Override
	public final String getBorderStyle() {
		return DomStyleStatic.getBorderStyle(this);
	}

	@Override
	public final String getBorderWidth() {
		return DomStyleStatic.getBorderWidth(this);
	}

	@Override
	public final String getBottom() {
		return DomStyleStatic.getBottom(this);
	}

	@Override
	public final String getClear() {
		return DomStyleStatic.getClear(this);
	}

	@Override
	public final String getColor() {
		return DomStyleStatic.getColor(this);
	}

	@Override
	public final String getCursor() {
		return DomStyleStatic.getCursor(this);
	}

	@Override
	public final String getDisplay() {
		return DomStyleStatic.getDisplay(this);
	}

	@Override
	public final String getFontSize() {
		return DomStyleStatic.getFontSize(this);
	}

	@Override
	public final String getFontStyle() {
		return DomStyleStatic.getFontStyle(this);
	}

	@Override
	public final String getFontWeight() {
		return DomStyleStatic.getFontWeight(this);
	}

	@Override
	public final String getHeight() {
		return DomStyleStatic.getHeight(this);
	}

	@Override
	public final String getLeft() {
		return DomStyleStatic.getLeft(this);
	}

	@Override
	public final String getLineHeight() {
		return DomStyleStatic.getLineHeight(this);
	}

	@Override
	public final String getListStyleType() {
		return DomStyleStatic.getListStyleType(this);
	}

	@Override
	public final String getMargin() {
		return DomStyleStatic.getMargin(this);
	}

	@Override
	public final String getMarginBottom() {
		return DomStyleStatic.getMarginBottom(this);
	}

	@Override
	public final String getMarginLeft() {
		return DomStyleStatic.getMarginLeft(this);
	}

	@Override
	public final String getMarginRight() {
		return DomStyleStatic.getMarginRight(this);
	}

	@Override
	public final String getMarginTop() {
		return DomStyleStatic.getMarginTop(this);
	}

	@Override
	public final String getOpacity() {
		return DomStyleStatic.getOpacity(this);
	}

	@Override
	public final String getOverflow() {
		return DomStyleStatic.getOverflow(this);
	}

	@Override
	public final String getOverflowX() {
		return DomStyleStatic.getOverflowX(this);
	}

	@Override
	public final String getOverflowY() {
		return DomStyleStatic.getOverflowY(this);
	}

	@Override
	public final String getPadding() {
		return DomStyleStatic.getPadding(this);
	}

	@Override
	public final String getPaddingBottom() {
		return DomStyleStatic.getPaddingBottom(this);
	}

	@Override
	public final String getPaddingLeft() {
		return DomStyleStatic.getPaddingLeft(this);
	}

	@Override
	public final String getPaddingRight() {
		return DomStyleStatic.getPaddingRight(this);
	}

	@Override
	public final String getPaddingTop() {
		return DomStyleStatic.getPaddingTop(this);
	}

	@Override
	public final String getPosition() {
		return DomStyleStatic.getPosition(this);
	}

	@Override
	public final String getProperty(String name) {
		return DomStyleStatic.getProperty(this, name);
	}

	@Override
	public final String getRight() {
		return DomStyleStatic.getRight(this);
	}

	@Override
	public final String getTableLayout() {
		return DomStyleStatic.getTableLayout(this);
	}

	@Override
	public final String getTextAlign() {
		return DomStyleStatic.getTextAlign(this);
	}

	@Override
	public final String getTextDecoration() {
		return DomStyleStatic.getTextDecoration(this);
	}

	@Override
	public final String getTextIndent() {
		return DomStyleStatic.getTextIndent(this);
	}

	@Override
	public final String getTextJustify() {
		return DomStyleStatic.getTextJustify(this);
	}

	@Override
	public final String getTextOverflow() {
		return DomStyleStatic.getTextOverflow(this);
	}

	@Override
	public final String getTextTransform() {
		return DomStyleStatic.getTextTransform(this);
	}

	@Override
	public final String getTop() {
		return DomStyleStatic.getTop(this);
	}

	@Override
	public final String getVerticalAlign() {
		return DomStyleStatic.getVerticalAlign(this);
	}

	@Override
	public final String getVisibility() {
		return DomStyleStatic.getVisibility(this);
	}

	@Override
	public final String getWhiteSpace() {
		return DomStyleStatic.getWhiteSpace(this);
	}

	@Override
	public final String getWidth() {
		return DomStyleStatic.getWidth(this);
	}

	@Override
	public final String getZIndex() {
		return DomStyleStatic.getZIndex(this);
	}

	@Override
	public final void setBackgroundColor(String value) {
		DomStyleStatic.setBackgroundColor(this, value);
	}

	@Override
	public final void setBackgroundImage(String value) {
		DomStyleStatic.setBackgroundImage(this, value);
	}

	@Override
	public final void setBorderColor(String value) {
		DomStyleStatic.setBorderColor(this, value);
	}

	@Override
	public final void setBorderStyle(BorderStyle value) {
		DomStyleStatic.setBorderStyle(this, value);
	}

	@Override
	public final void setBorderWidth(double value, Unit unit) {
		DomStyleStatic.setBorderWidth(this, value, unit);
	}

	@Override
	public final void setBottom(double value, Unit unit) {
		DomStyleStatic.setBottom(this, value, unit);
	}

	@Override
	public final void setClear(Clear value) {
		DomStyleStatic.setClear(this, value);
	}

	@Override
	public final void setColor(String value) {
		DomStyleStatic.setColor(this, value);
	}

	@Override
	public final void setCursor(Cursor value) {
		DomStyleStatic.setCursor(this, value);
	}

	@Override
	public final void setDisplay(Display value) {
		DomStyleStatic.setDisplay(this, value);
	}

	@Override
	public final void setFloat(Float value) {
		DomStyleStatic.setFloat(this, value);
	}

	@Override
	public final void setFontSize(double value, Unit unit) {
		DomStyleStatic.setFontSize(this, value, unit);
	}

	@Override
	public final void setFontStyle(FontStyle value) {
		DomStyleStatic.setFontStyle(this, value);
	}

	@Override
	public final void setFontWeight(FontWeight value) {
		DomStyleStatic.setFontWeight(this, value);
	}

	@Override
	public final void setHeight(double value, Unit unit) {
		DomStyleStatic.setHeight(this, value, unit);
	}

	@Override
	public final void setLeft(double value, Unit unit) {
		DomStyleStatic.setLeft(this, value, unit);
	}

	@Override
	public final void setLineHeight(double value, Unit unit) {
		DomStyleStatic.setLineHeight(this, value, unit);
	}

	@Override
	public final void setListStyleType(ListStyleType value) {
		DomStyleStatic.setListStyleType(this, value);
	}

	@Override
	public final void setMargin(double value, Unit unit) {
		DomStyleStatic.setMargin(this, value, unit);
	}

	@Override
	public final void setMarginBottom(double value, Unit unit) {
		DomStyleStatic.setMarginBottom(this, value, unit);
	}

	@Override
	public final void setMarginLeft(double value, Unit unit) {
		DomStyleStatic.setMarginLeft(this, value, unit);
	}

	@Override
	public final void setMarginRight(double value, Unit unit) {
		DomStyleStatic.setMarginRight(this, value, unit);
	}

	@Override
	public final void setMarginTop(double value, Unit unit) {
		DomStyleStatic.setMarginTop(this, value, unit);
	}

	@Override
	public final void setOpacity(double value) {
		DomStyleStatic.setOpacity(this, value);
	}

	@Override
	public final void setOutlineColor(String value) {
		DomStyleStatic.setOutlineColor(this, value);
	}

	@Override
	public final void setOutlineStyle(OutlineStyle value) {
		DomStyleStatic.setOutlineStyle(this, value);
	}

	@Override
	public final void setOutlineWidth(double value, Unit unit) {
		DomStyleStatic.setOutlineWidth(this, value, unit);
	}

	@Override
	public final void setOverflow(Overflow value) {
		DomStyleStatic.setOverflow(this, value);
	}

	@Override
	public final void setOverflowX(Overflow value) {
		DomStyleStatic.setOverflowX(this, value);
	}

	@Override
	public final void setOverflowY(Overflow value) {
		DomStyleStatic.setOverflowY(this, value);
	}

	@Override
	public final void setPadding(double value, Unit unit) {
		DomStyleStatic.setPadding(this, value, unit);
	}

	@Override
	public final void setPaddingBottom(double value, Unit unit) {
		DomStyleStatic.setPaddingBottom(this, value, unit);
	}

	@Override
	public final void setPaddingLeft(double value, Unit unit) {
		DomStyleStatic.setPaddingLeft(this, value, unit);
	}

	@Override
	public final void setPaddingRight(double value, Unit unit) {
		DomStyleStatic.setPaddingRight(this, value, unit);
	}

	@Override
	public final void setPaddingTop(double value, Unit unit) {
		DomStyleStatic.setPaddingTop(this, value, unit);
	}

	@Override
	public final void setPosition(Position value) {
		DomStyleStatic.setPosition(this, value);
	}

	@Override
	public final void setProperty(String name, String value) {
		DomStyleStatic.setProperty(this, name, value);
	}

	@Override
	public final void setProperty(String name, double value, Unit unit) {
		DomStyleStatic.setProperty(this, name, value, unit);
	}

	@Override
	public final void setPropertyPx(String name, int value) {
		DomStyleStatic.setPropertyPx(this, name, value);
	}

	@Override
	public final void setRight(double value, Unit unit) {
		DomStyleStatic.setRight(this, value, unit);
	}

	@Override
	public final void setTableLayout(TableLayout value) {
		DomStyleStatic.setTableLayout(this, value);
	}

	@Override
	public final void setTextAlign(TextAlign value) {
		DomStyleStatic.setTextAlign(this, value);
	}

	@Override
	public final void setTextDecoration(TextDecoration value) {
		DomStyleStatic.setTextDecoration(this, value);
	}

	@Override
	public final void setTextIndent(double value, Unit unit) {
		DomStyleStatic.setTextIndent(this, value, unit);
	}

	@Override
	public final void setTextJustify(TextJustify value) {
		DomStyleStatic.setTextJustify(this, value);
	}

	@Override
	public final void setTextOverflow(TextOverflow value) {
		DomStyleStatic.setTextOverflow(this, value);
	}

	@Override
	public final void setTextTransform(TextTransform value) {
		DomStyleStatic.setTextTransform(this, value);
	}

	@Override
	public final void setTop(double value, Unit unit) {
		DomStyleStatic.setTop(this, value, unit);
	}

	@Override
	public final void setVerticalAlign(VerticalAlign value) {
		DomStyleStatic.setVerticalAlign(this, value);
	}

	@Override
	public final void setVerticalAlign(double value, Unit unit) {
		DomStyleStatic.setVerticalAlign(this, value, unit);
	}

	@Override
	public final void setVisibility(Visibility value) {
		DomStyleStatic.setVisibility(this, value);
	}

	@Override
	public final void setWhiteSpace(WhiteSpace value) {
		DomStyleStatic.setWhiteSpace(this, value);
	}

	@Override
	public final void setWidth(double value, Unit unit) {
		DomStyleStatic.setWidth(this, value, unit);
	}

	@Override
	public final void setZIndex(int value) {
		DomStyleStatic.setZIndex(this, value);
	}

	@Override
	// LD2 - never called
	public final String getPropertyImpl(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, String> getProperties() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cloneStyleFrom(DomStyle local) {
		throw new UnsupportedOperationException();
	}
}