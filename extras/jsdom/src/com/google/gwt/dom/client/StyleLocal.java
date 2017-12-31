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

import cc.alcina.framework.common.client.util.StringMap;

public class StyleLocal implements DomStyle {
	StringMap properties = new StringMap();

	private Style styleObject;

	public StyleLocal(Style style) {
		this.styleObject = style;
	}

	@Override
	public void clearBackgroundColor() {
		DomStyleStatic.clearBackgroundColor(this);
	}

	@Override
	public void clearBackgroundImage() {
		DomStyleStatic.clearBackgroundImage(this);
	}

	@Override
	public void clearBorderColor() {
		DomStyleStatic.clearBorderColor(this);
	}

	@Override
	public void clearBorderStyle() {
		DomStyleStatic.clearBorderStyle(this);
	}

	@Override
	public void clearBorderWidth() {
		DomStyleStatic.clearBorderWidth(this);
	}

	@Override
	public void clearBottom() {
		DomStyleStatic.clearBottom(this);
	}

	@Override
	public void clearClear() {
		DomStyleStatic.clearClear(this);
	}

	@Override
	public void clearColor() {
		DomStyleStatic.clearColor(this);
	}

	@Override
	public void clearCursor() {
		DomStyleStatic.clearCursor(this);
	}

	@Override
	public void clearDisplay() {
		DomStyleStatic.clearDisplay(this);
	}

	@Override
	public void clearFloat() {
		DomStyleStatic.clearFloat(this);
	}

	@Override
	public void clearFontSize() {
		DomStyleStatic.clearFontSize(this);
	}

	@Override
	public void clearFontStyle() {
		DomStyleStatic.clearFontStyle(this);
	}

	@Override
	public void clearFontWeight() {
		DomStyleStatic.clearFontWeight(this);
	}

	@Override
	public void clearHeight() {
		DomStyleStatic.clearHeight(this);
	}

	@Override
	public void clearLeft() {
		DomStyleStatic.clearLeft(this);
	}

	@Override
	public void clearLineHeight() {
		DomStyleStatic.clearLineHeight(this);
	}

	@Override
	public void clearListStyleType() {
		DomStyleStatic.clearListStyleType(this);
	}

	@Override
	public void clearMargin() {
		DomStyleStatic.clearMargin(this);
	}

	@Override
	public void clearMarginBottom() {
		DomStyleStatic.clearMarginBottom(this);
	}

	@Override
	public void clearMarginLeft() {
		DomStyleStatic.clearMarginLeft(this);
	}

	@Override
	public void clearMarginRight() {
		DomStyleStatic.clearMarginRight(this);
	}

	@Override
	public void clearMarginTop() {
		DomStyleStatic.clearMarginTop(this);
	}

	@Override
	public void clearOpacity() {
		DomStyleStatic.clearOpacity(this);
	}

	@Override
	public void clearOutlineColor() {
		DomStyleStatic.clearOutlineColor(this);
	}

	@Override
	public void clearOutlineStyle() {
		DomStyleStatic.clearOutlineStyle(this);
	}

	@Override
	public void clearOutlineWidth() {
		DomStyleStatic.clearOutlineWidth(this);
	}

	@Override
	public void clearOverflow() {
		DomStyleStatic.clearOverflow(this);
	}

	@Override
	public void clearOverflowX() {
		DomStyleStatic.clearOverflowX(this);
	}

	@Override
	public void clearOverflowY() {
		DomStyleStatic.clearOverflowY(this);
	}

	@Override
	public void clearPadding() {
		DomStyleStatic.clearPadding(this);
	}

	@Override
	public void clearPaddingBottom() {
		DomStyleStatic.clearPaddingBottom(this);
	}

	@Override
	public void clearPaddingLeft() {
		DomStyleStatic.clearPaddingLeft(this);
	}

	@Override
	public void clearPaddingRight() {
		DomStyleStatic.clearPaddingRight(this);
	}

	@Override
	public void clearPaddingTop() {
		DomStyleStatic.clearPaddingTop(this);
	}

	@Override
	public void clearPosition() {
		DomStyleStatic.clearPosition(this);
	}

	@Override
	public void clearProperty(String name) {
		DomStyleStatic.clearProperty(this, name);
	}

	@Override
	public void clearRight() {
		DomStyleStatic.clearRight(this);
	}

	@Override
	public void clearTableLayout() {
		DomStyleStatic.clearTableLayout(this);
	}

	@Override
	public void clearTextAlign() {
		DomStyleStatic.clearTextAlign(this);
	}

	@Override
	public void clearTextDecoration() {
		DomStyleStatic.clearTextDecoration(this);
	}

	@Override
	public void clearTextIndent() {
		DomStyleStatic.clearTextIndent(this);
	}

	@Override
	public void clearTextJustify() {
		DomStyleStatic.clearTextJustify(this);
	}

	@Override
	public void clearTextOverflow() {
		DomStyleStatic.clearTextOverflow(this);
	}

	@Override
	public void clearTextTransform() {
		DomStyleStatic.clearTextTransform(this);
	}

	@Override
	public void clearTop() {
		DomStyleStatic.clearTop(this);
	}

	@Override
	public void clearVisibility() {
		DomStyleStatic.clearVisibility(this);
	}

	@Override
	public void clearWhiteSpace() {
		DomStyleStatic.clearWhiteSpace(this);
	}

	@Override
	public void clearWidth() {
		DomStyleStatic.clearWidth(this);
	}

	@Override
	public void clearZIndex() {
		DomStyleStatic.clearZIndex(this);
	}

	@Override
	public String getBackgroundColor() {
		return DomStyleStatic.getBackgroundColor(this);
	}

	@Override
	public String getBackgroundImage() {
		return DomStyleStatic.getBackgroundImage(this);
	}

	@Override
	public String getBorderColor() {
		return DomStyleStatic.getBorderColor(this);
	}

	@Override
	public String getBorderStyle() {
		return DomStyleStatic.getBorderStyle(this);
	}

	@Override
	public String getBorderWidth() {
		return DomStyleStatic.getBorderWidth(this);
	}

	@Override
	public String getBottom() {
		return DomStyleStatic.getBottom(this);
	}

	@Override
	public String getClear() {
		return DomStyleStatic.getClear(this);
	}

	@Override
	public String getColor() {
		return DomStyleStatic.getColor(this);
	}

	@Override
	public String getCursor() {
		return DomStyleStatic.getCursor(this);
	}

	@Override
	public String getDisplay() {
		return DomStyleStatic.getDisplay(this);
	}

	@Override
	public String getFontSize() {
		return DomStyleStatic.getFontSize(this);
	}

	@Override
	public String getFontStyle() {
		return DomStyleStatic.getFontStyle(this);
	}

	@Override
	public String getFontWeight() {
		return DomStyleStatic.getFontWeight(this);
	}

	@Override
	public String getHeight() {
		return DomStyleStatic.getHeight(this);
	}

	@Override
	public String getLeft() {
		return DomStyleStatic.getLeft(this);
	}

	@Override
	public String getLineHeight() {
		return DomStyleStatic.getLineHeight(this);
	}

	@Override
	public String getListStyleType() {
		return DomStyleStatic.getListStyleType(this);
	}

	@Override
	public String getMargin() {
		return DomStyleStatic.getMargin(this);
	}

	@Override
	public String getMarginBottom() {
		return DomStyleStatic.getMarginBottom(this);
	}

	@Override
	public String getMarginLeft() {
		return DomStyleStatic.getMarginLeft(this);
	}

	@Override
	public String getMarginRight() {
		return DomStyleStatic.getMarginRight(this);
	}

	@Override
	public String getMarginTop() {
		return DomStyleStatic.getMarginTop(this);
	}

	@Override
	public String getOpacity() {
		return DomStyleStatic.getOpacity(this);
	}

	@Override
	public String getOverflow() {
		return DomStyleStatic.getOverflow(this);
	}

	@Override
	public String getOverflowX() {
		return DomStyleStatic.getOverflowX(this);
	}

	@Override
	public String getOverflowY() {
		return DomStyleStatic.getOverflowY(this);
	}

	@Override
	public String getPadding() {
		return DomStyleStatic.getPadding(this);
	}

	@Override
	public String getPaddingBottom() {
		return DomStyleStatic.getPaddingBottom(this);
	}

	@Override
	public String getPaddingLeft() {
		return DomStyleStatic.getPaddingLeft(this);
	}

	@Override
	public String getPaddingRight() {
		return DomStyleStatic.getPaddingRight(this);
	}

	@Override
	public String getPaddingTop() {
		return DomStyleStatic.getPaddingTop(this);
	}

	@Override
	public String getPosition() {
		return DomStyleStatic.getPosition(this);
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String getProperty(String name) {
		return DomStyleStatic.getProperty(this, name);
	}

	@Override
	public String getPropertyImpl(String name) {
		return properties.computeIfAbsent(name, lambda_name -> "");
	}

	@Override
	public String getRight() {
		return DomStyleStatic.getRight(this);
	}

	@Override
	public String getTableLayout() {
		return DomStyleStatic.getTableLayout(this);
	}

	@Override
	public String getTextAlign() {
		return DomStyleStatic.getTextAlign(this);
	}

	@Override
	public String getTextDecoration() {
		return DomStyleStatic.getTextDecoration(this);
	}

	@Override
	public String getTextIndent() {
		return DomStyleStatic.getTextIndent(this);
	}

	@Override
	public String getTextJustify() {
		return DomStyleStatic.getTextJustify(this);
	}

	@Override
	public String getTextOverflow() {
		return DomStyleStatic.getTextOverflow(this);
	}

	@Override
	public String getTextTransform() {
		return DomStyleStatic.getTextTransform(this);
	}

	@Override
	public String getTop() {
		return DomStyleStatic.getTop(this);
	}

	@Override
	public String getVerticalAlign() {
		return DomStyleStatic.getVerticalAlign(this);
	}

	@Override
	public String getVisibility() {
		return DomStyleStatic.getVisibility(this);
	}

	@Override
	public String getWhiteSpace() {
		return DomStyleStatic.getWhiteSpace(this);
	}

	@Override
	public String getWidth() {
		return DomStyleStatic.getWidth(this);
	}

	@Override
	public String getZIndex() {
		return DomStyleStatic.getZIndex(this);
	}

	@Override
	public void setBackgroundColor(String value) {
		DomStyleStatic.setBackgroundColor(this, value);
	}

	@Override
	public void setBackgroundImage(String value) {
		DomStyleStatic.setBackgroundImage(this, value);
	}

	@Override
	public void setBorderColor(String value) {
		DomStyleStatic.setBorderColor(this, value);
	}

	@Override
	public void setBorderStyle(BorderStyle value) {
		DomStyleStatic.setBorderStyle(this, value);
	}

	@Override
	public void setBorderWidth(double value, Unit unit) {
		DomStyleStatic.setBorderWidth(this, value, unit);
	}

	@Override
	public void setBottom(double value, Unit unit) {
		DomStyleStatic.setBottom(this, value, unit);
	}

	@Override
	public void setClear(Clear value) {
		DomStyleStatic.setClear(this, value);
	}

	@Override
	public void setColor(String value) {
		DomStyleStatic.setColor(this, value);
	}

	@Override
	public void setCursor(Cursor value) {
		DomStyleStatic.setCursor(this, value);
	}

	@Override
	public void setDisplay(Display value) {
		DomStyleStatic.setDisplay(this, value);
	}

	@Override
	public void setFloat(Float value) {
		DomStyleStatic.setFloat(this, value);
	}

	@Override
	public void setFontSize(double value, Unit unit) {
		DomStyleStatic.setFontSize(this, value, unit);
	}

	@Override
	public void setFontStyle(FontStyle value) {
		DomStyleStatic.setFontStyle(this, value);
	}

	@Override
	public void setFontWeight(FontWeight value) {
		DomStyleStatic.setFontWeight(this, value);
	}

	@Override
	public void setHeight(double value, Unit unit) {
		DomStyleStatic.setHeight(this, value, unit);
	}

	@Override
	public void setLeft(double value, Unit unit) {
		DomStyleStatic.setLeft(this, value, unit);
	}

	@Override
	public void setLineHeight(double value, Unit unit) {
		DomStyleStatic.setLineHeight(this, value, unit);
	}

	@Override
	public void setListStyleType(ListStyleType value) {
		DomStyleStatic.setListStyleType(this, value);
	}

	@Override
	public void setMargin(double value, Unit unit) {
		DomStyleStatic.setMargin(this, value, unit);
	}

	@Override
	public void setMarginBottom(double value, Unit unit) {
		DomStyleStatic.setMarginBottom(this, value, unit);
	}

	@Override
	public void setMarginLeft(double value, Unit unit) {
		DomStyleStatic.setMarginLeft(this, value, unit);
	}

	@Override
	public void setMarginRight(double value, Unit unit) {
		DomStyleStatic.setMarginRight(this, value, unit);
	}

	@Override
	public void setMarginTop(double value, Unit unit) {
		DomStyleStatic.setMarginTop(this, value, unit);
	}

	@Override
	public void setOpacity(double value) {
		DomStyleStatic.setOpacity(this, value);
	}

	@Override
	public void setOutlineColor(String value) {
		DomStyleStatic.setOutlineColor(this, value);
	}

	@Override
	public void setOutlineStyle(OutlineStyle value) {
		DomStyleStatic.setOutlineStyle(this, value);
	}

	@Override
	public void setOutlineWidth(double value, Unit unit) {
		DomStyleStatic.setOutlineWidth(this, value, unit);
	}

	@Override
	public void setOverflow(Overflow value) {
		DomStyleStatic.setOverflow(this, value);
	}

	@Override
	public void setOverflowX(Overflow value) {
		DomStyleStatic.setOverflowX(this, value);
	}

	@Override
	public void setOverflowY(Overflow value) {
		DomStyleStatic.setOverflowY(this, value);
	}

	@Override
	public void setPadding(double value, Unit unit) {
		DomStyleStatic.setPadding(this, value, unit);
	}

	@Override
	public void setPaddingBottom(double value, Unit unit) {
		DomStyleStatic.setPaddingBottom(this, value, unit);
	}

	@Override
	public void setPaddingLeft(double value, Unit unit) {
		DomStyleStatic.setPaddingLeft(this, value, unit);
	}

	@Override
	public void setPaddingRight(double value, Unit unit) {
		DomStyleStatic.setPaddingRight(this, value, unit);
	}

	@Override
	public void setPaddingTop(double value, Unit unit) {
		DomStyleStatic.setPaddingTop(this, value, unit);
	}

	@Override
	public void setPosition(Position value) {
		DomStyleStatic.setPosition(this, value);
	}

	@Override
	public void setProperty(String name, double value, Unit unit) {
		DomStyleStatic.setProperty(this, name, value, unit);
	}

	@Override
	public void setProperty(String name, String value) {
		DomStyleStatic.setProperty(this, name, value);
	}

	@Override
	public void setPropertyImpl(String name, String value) {
		properties.put(name, value);
	}

	@Override
	public void setPropertyPx(String name, int value) {
		DomStyleStatic.setPropertyPx(this, name, value);
	}

	@Override
	public void setRight(double value, Unit unit) {
		DomStyleStatic.setRight(this, value, unit);
	}

	@Override
	public void setTableLayout(TableLayout value) {
		DomStyleStatic.setTableLayout(this, value);
	}

	@Override
	public void setTextAlign(TextAlign value) {
		DomStyleStatic.setTextAlign(this, value);
	}

	@Override
	public void setTextDecoration(TextDecoration value) {
		DomStyleStatic.setTextDecoration(this, value);
	}

	@Override
	public void setTextIndent(double value, Unit unit) {
		DomStyleStatic.setTextIndent(this, value, unit);
	}

	@Override
	public void setTextJustify(TextJustify value) {
		DomStyleStatic.setTextJustify(this, value);
	}

	@Override
	public void setTextOverflow(TextOverflow value) {
		DomStyleStatic.setTextOverflow(this, value);
	}

	@Override
	public void setTextTransform(TextTransform value) {
		DomStyleStatic.setTextTransform(this, value);
	}

	@Override
	public void setTop(double value, Unit unit) {
		DomStyleStatic.setTop(this, value, unit);
	}

	@Override
	public void setVerticalAlign(double value, Unit unit) {
		DomStyleStatic.setVerticalAlign(this, value, unit);
	}

	@Override
	public void setVerticalAlign(VerticalAlign value) {
		DomStyleStatic.setVerticalAlign(this, value);
	}

	@Override
	public void setVisibility(Visibility value) {
		DomStyleStatic.setVisibility(this, value);
	}

	@Override
	public void setWhiteSpace(WhiteSpace value) {
		DomStyleStatic.setWhiteSpace(this, value);
	}

	@Override
	public void setWidth(double value, Unit unit) {
		DomStyleStatic.setWidth(this, value, unit);
	}

	@Override
	public void setZIndex(int value) {
		DomStyleStatic.setZIndex(this, value);
	}

	@Override
	public Style styleObject() {
		return styleObject;
	}

	void cloneStyleFrom(DomStyle other, Style to) {
		StyleLocal clone = new StyleLocal(to);
		clone.properties = new StringMap(((StyleLocal) other).properties);
	}

	boolean isEmpty() {
		return properties.isEmpty();
	}

	void removeProperty(String key) {
		properties.remove(key);
	}
}
