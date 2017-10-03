package com.google.gwt.dom.client;

import java.util.Map;

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

public class StyleNull implements DomStyle {
	static final StyleNull INSTANCE = new StyleNull();

	private StyleNull() {
	}

	@Override
	public void clearBackgroundColor() {
	}

	@Override
	public void clearBackgroundImage() {
	}

	@Override
	public void clearBorderColor() {
	}

	@Override
	public void clearBorderStyle() {
	}

	@Override
	public void clearBorderWidth() {
	}

	@Override
	public void clearBottom() {
	}

	@Override
	public void clearClear() {
	}

	@Override
	public void clearColor() {
	}

	@Override
	public void clearCursor() {
	}

	@Override
	public void clearDisplay() {
	}

	@Override
	public void clearFloat() {
	}

	@Override
	public void clearFontSize() {
	}

	@Override
	public void clearFontStyle() {
	}

	@Override
	public void clearFontWeight() {
	}

	@Override
	public void clearHeight() {
	}

	@Override
	public void clearLeft() {
	}

	@Override
	public void clearLineHeight() {
	}

	@Override
	public void clearListStyleType() {
	}

	@Override
	public void clearMargin() {
	}

	@Override
	public void clearMarginBottom() {
	}

	@Override
	public void clearMarginLeft() {
	}

	@Override
	public void clearMarginRight() {
	}

	@Override
	public void clearMarginTop() {
	}

	@Override
	public void clearOpacity() {
	}

	@Override
	public void clearOutlineColor() {
	}

	@Override
	public void clearOutlineStyle() {
	}

	@Override
	public void clearOutlineWidth() {
	}

	@Override
	public void clearOverflow() {
	}

	@Override
	public void clearOverflowX() {
	}

	@Override
	public void clearOverflowY() {
	}

	@Override
	public void clearPadding() {
	}

	@Override
	public void clearPaddingBottom() {
	}

	@Override
	public void clearPaddingLeft() {
	}

	@Override
	public void clearPaddingRight() {
	}

	@Override
	public void clearPaddingTop() {
	}

	@Override
	public void clearPosition() {
	}

	@Override
	public void clearProperty(String name) {
	}

	@Override
	public void clearRight() {
	}

	@Override
	public void clearTableLayout() {
	}

	@Override
	public void clearTextAlign() {
	}

	@Override
	public void clearTextDecoration() {
	}

	@Override
	public void clearTextIndent() {
	}

	@Override
	public void clearTextJustify() {
	}

	@Override
	public void clearTextOverflow() {
	}

	@Override
	public void clearTextTransform() {
	}

	@Override
	public void clearTop() {
	}

	@Override
	public void clearVisibility() {
	}

	@Override
	public void clearWhiteSpace() {
	}

	@Override
	public void clearWidth() {
	}

	@Override
	public void clearZIndex() {
	}

	@Override
	public String getBackgroundColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBackgroundImage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBorderColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBorderStyle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBorderWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBottom() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCursor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDisplay() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFontSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFontStyle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFontWeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLineHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getListStyleType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMargin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMarginBottom() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMarginLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMarginRight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMarginTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getOpacity() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getOverflow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getOverflowX() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getOverflowY() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPadding() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPaddingBottom() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPaddingLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPaddingRight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPaddingTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPosition() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, String> getProperties() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getProperty(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPropertyImpl(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTableLayout() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTextAlign() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTextDecoration() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTextIndent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTextJustify() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTextOverflow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTextTransform() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getVerticalAlign() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getVisibility() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getWhiteSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getZIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBackgroundColor(String value) {
	}

	@Override
	public void setBackgroundImage(String value) {
	}

	@Override
	public void setBorderColor(String value) {
	}

	@Override
	public void setBorderStyle(BorderStyle value) {
	}

	@Override
	public void setBorderWidth(double value, Unit unit) {
	}

	@Override
	public void setBottom(double value, Unit unit) {
	}

	@Override
	public void setClear(Clear value) {
	}

	@Override
	public void setColor(String value) {
	}

	@Override
	public void setCursor(Cursor value) {
	}

	@Override
	public void setDisplay(Display value) {
	}

	@Override
	public void setFloat(Float value) {
	}

	@Override
	public void setFontSize(double value, Unit unit) {
	}

	@Override
	public void setFontStyle(FontStyle value) {
	}

	@Override
	public void setFontWeight(FontWeight value) {
	}

	@Override
	public void setHeight(double value, Unit unit) {
	}

	@Override
	public void setLeft(double value, Unit unit) {
	}

	@Override
	public void setLineHeight(double value, Unit unit) {
	}

	@Override
	public void setListStyleType(ListStyleType value) {
	}

	@Override
	public void setMargin(double value, Unit unit) {
	}

	@Override
	public void setMarginBottom(double value, Unit unit) {
	}

	@Override
	public void setMarginLeft(double value, Unit unit) {
	}

	@Override
	public void setMarginRight(double value, Unit unit) {
	}

	@Override
	public void setMarginTop(double value, Unit unit) {
	}

	@Override
	public void setOpacity(double value) {
	}

	@Override
	public void setOutlineColor(String value) {
	}

	@Override
	public void setOutlineStyle(OutlineStyle value) {
	}

	@Override
	public void setOutlineWidth(double value, Unit unit) {
	}

	@Override
	public void setOverflow(Overflow value) {
	}

	@Override
	public void setOverflowX(Overflow value) {
	}

	@Override
	public void setOverflowY(Overflow value) {
	}

	@Override
	public void setPadding(double value, Unit unit) {
	}

	@Override
	public void setPaddingBottom(double value, Unit unit) {
	}

	@Override
	public void setPaddingLeft(double value, Unit unit) {
	}

	@Override
	public void setPaddingRight(double value, Unit unit) {
	}

	@Override
	public void setPaddingTop(double value, Unit unit) {
	}

	@Override
	public void setPosition(Position value) {
	}

	@Override
	public void setProperty(String name, double value, Unit unit) {
	}

	@Override
	public void setProperty(String name, String value) {
	}

	@Override
	public void setPropertyImpl(String name, String value) {
	}

	@Override
	public void setPropertyPx(String name, int value) {
	}

	@Override
	public void setRight(double value, Unit unit) {
	}

	@Override
	public void setTableLayout(TableLayout value) {
	}

	@Override
	public void setTextAlign(TextAlign value) {
	}

	@Override
	public void setTextDecoration(TextDecoration value) {
	}

	@Override
	public void setTextIndent(double value, Unit unit) {
	}

	@Override
	public void setTextJustify(TextJustify value) {
	}

	@Override
	public void setTextOverflow(TextOverflow value) {
	}

	@Override
	public void setTextTransform(TextTransform value) {
	}

	@Override
	public void setTop(double value, Unit unit) {
	}

	@Override
	public void setVerticalAlign(double value, Unit unit) {
	}

	@Override
	public void setVerticalAlign(VerticalAlign value) {
	}

	@Override
	public void setVisibility(Visibility value) {
	}

	@Override
	public void setWhiteSpace(WhiteSpace value) {
	}

	@Override
	public void setWidth(double value, Unit unit) {
	}

	@Override
	public void setZIndex(int value) {
	}

	@Override
	public Style styleObject() {
		throw new UnsupportedOperationException();
	}
}
