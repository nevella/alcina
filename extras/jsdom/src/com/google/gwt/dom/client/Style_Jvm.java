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

public class Style_Jvm implements DomStyle {
	StringMap properties = new StringMap();

	@Override
	public final Style styleObject() {
		return LocalDomBridge.styleObjectFor(this);
	}

	@Override
	public String getPropertyImpl(String name) {
		return properties.computeIfAbsent(name, lambda_name -> "");
	}

	@Override
	public void setPropertyImpl(String name, String value) {
		properties.put(name, value);
	}

	@Override
	public final void clearBackgroundColor() {
		DomStyle_Static.clearBackgroundColor(this);
	}

	@Override
	public final void clearBackgroundImage() {
		DomStyle_Static.clearBackgroundImage(this);
	}

	@Override
	public final void clearBorderColor() {
		DomStyle_Static.clearBorderColor(this);
	}

	@Override
	public final void clearBorderStyle() {
		DomStyle_Static.clearBorderStyle(this);
	}

	@Override
	public final void clearBorderWidth() {
		DomStyle_Static.clearBorderWidth(this);
	}

	@Override
	public final void clearBottom() {
		DomStyle_Static.clearBottom(this);
	}

	@Override
	public final void clearClear() {
		DomStyle_Static.clearClear(this);
	}

	@Override
	public final void clearColor() {
		DomStyle_Static.clearColor(this);
	}

	@Override
	public final void clearCursor() {
		DomStyle_Static.clearCursor(this);
	}

	@Override
	public final void clearDisplay() {
		DomStyle_Static.clearDisplay(this);
	}

	@Override
	public final void clearFloat() {
		DomStyle_Static.clearFloat(this);
	}

	@Override
	public final void clearFontSize() {
		DomStyle_Static.clearFontSize(this);
	}

	@Override
	public final void clearFontStyle() {
		DomStyle_Static.clearFontStyle(this);
	}

	@Override
	public final void clearFontWeight() {
		DomStyle_Static.clearFontWeight(this);
	}

	@Override
	public final void clearHeight() {
		DomStyle_Static.clearHeight(this);
	}

	@Override
	public final void clearLeft() {
		DomStyle_Static.clearLeft(this);
	}

	@Override
	public final void clearLineHeight() {
		DomStyle_Static.clearLineHeight(this);
	}

	@Override
	public final void clearListStyleType() {
		DomStyle_Static.clearListStyleType(this);
	}

	@Override
	public final void clearMargin() {
		DomStyle_Static.clearMargin(this);
	}

	@Override
	public final void clearMarginBottom() {
		DomStyle_Static.clearMarginBottom(this);
	}

	@Override
	public final void clearMarginLeft() {
		DomStyle_Static.clearMarginLeft(this);
	}

	@Override
	public final void clearMarginRight() {
		DomStyle_Static.clearMarginRight(this);
	}

	@Override
	public final void clearMarginTop() {
		DomStyle_Static.clearMarginTop(this);
	}

	@Override
	public final void clearOpacity() {
		DomStyle_Static.clearOpacity(this);
	}

	@Override
	public final void clearOutlineColor() {
		DomStyle_Static.clearOutlineColor(this);
	}

	@Override
	public final void clearOutlineStyle() {
		DomStyle_Static.clearOutlineStyle(this);
	}

	@Override
	public final void clearOutlineWidth() {
		DomStyle_Static.clearOutlineWidth(this);
	}

	@Override
	public final void clearOverflow() {
		DomStyle_Static.clearOverflow(this);
	}

	@Override
	public final void clearOverflowX() {
		DomStyle_Static.clearOverflowX(this);
	}

	@Override
	public final void clearOverflowY() {
		DomStyle_Static.clearOverflowY(this);
	}

	@Override
	public final void clearPadding() {
		DomStyle_Static.clearPadding(this);
	}

	@Override
	public final void clearPaddingBottom() {
		DomStyle_Static.clearPaddingBottom(this);
	}

	@Override
	public final void clearPaddingLeft() {
		DomStyle_Static.clearPaddingLeft(this);
	}

	@Override
	public final void clearPaddingRight() {
		DomStyle_Static.clearPaddingRight(this);
	}

	@Override
	public final void clearPaddingTop() {
		DomStyle_Static.clearPaddingTop(this);
	}

	@Override
	public final void clearPosition() {
		DomStyle_Static.clearPosition(this);
	}

	@Override
	public final void clearProperty(String name) {
		DomStyle_Static.clearProperty(this, name);
	}

	@Override
	public final void clearRight() {
		DomStyle_Static.clearRight(this);
	}

	@Override
	public final void clearTableLayout() {
		DomStyle_Static.clearTableLayout(this);
	}

	@Override
	public final void clearTextAlign() {
		DomStyle_Static.clearTextAlign(this);
	}

	@Override
	public final void clearTextDecoration() {
		DomStyle_Static.clearTextDecoration(this);
	}

	@Override
	public final void clearTextIndent() {
		DomStyle_Static.clearTextIndent(this);
	}

	@Override
	public final void clearTextJustify() {
		DomStyle_Static.clearTextJustify(this);
	}

	@Override
	public final void clearTextOverflow() {
		DomStyle_Static.clearTextOverflow(this);
	}

	@Override
	public final void clearTextTransform() {
		DomStyle_Static.clearTextTransform(this);
	}

	@Override
	public final void clearTop() {
		DomStyle_Static.clearTop(this);
	}

	@Override
	public final void clearVisibility() {
		DomStyle_Static.clearVisibility(this);
	}

	@Override
	public final void clearWhiteSpace() {
		DomStyle_Static.clearWhiteSpace(this);
	}

	@Override
	public final void clearWidth() {
		DomStyle_Static.clearWidth(this);
	}

	@Override
	public final void clearZIndex() {
		DomStyle_Static.clearZIndex(this);
	}

	@Override
	public final String getBackgroundColor() {
		return DomStyle_Static.getBackgroundColor(this);
	}

	@Override
	public final String getBackgroundImage() {
		return DomStyle_Static.getBackgroundImage(this);
	}

	@Override
	public final String getBorderColor() {
		return DomStyle_Static.getBorderColor(this);
	}

	@Override
	public final String getBorderStyle() {
		return DomStyle_Static.getBorderStyle(this);
	}

	@Override
	public final String getBorderWidth() {
		return DomStyle_Static.getBorderWidth(this);
	}

	@Override
	public final String getBottom() {
		return DomStyle_Static.getBottom(this);
	}

	@Override
	public final String getClear() {
		return DomStyle_Static.getClear(this);
	}

	@Override
	public final String getColor() {
		return DomStyle_Static.getColor(this);
	}

	@Override
	public final String getCursor() {
		return DomStyle_Static.getCursor(this);
	}

	@Override
	public final String getDisplay() {
		return DomStyle_Static.getDisplay(this);
	}

	@Override
	public final String getFontSize() {
		return DomStyle_Static.getFontSize(this);
	}

	@Override
	public final String getFontStyle() {
		return DomStyle_Static.getFontStyle(this);
	}

	@Override
	public final String getFontWeight() {
		return DomStyle_Static.getFontWeight(this);
	}

	@Override
	public final String getHeight() {
		return DomStyle_Static.getHeight(this);
	}

	@Override
	public final String getLeft() {
		return DomStyle_Static.getLeft(this);
	}

	@Override
	public final String getLineHeight() {
		return DomStyle_Static.getLineHeight(this);
	}

	@Override
	public final String getListStyleType() {
		return DomStyle_Static.getListStyleType(this);
	}

	@Override
	public final String getMargin() {
		return DomStyle_Static.getMargin(this);
	}

	@Override
	public final String getMarginBottom() {
		return DomStyle_Static.getMarginBottom(this);
	}

	@Override
	public final String getMarginLeft() {
		return DomStyle_Static.getMarginLeft(this);
	}

	@Override
	public final String getMarginRight() {
		return DomStyle_Static.getMarginRight(this);
	}

	@Override
	public final String getMarginTop() {
		return DomStyle_Static.getMarginTop(this);
	}

	@Override
	public final String getOpacity() {
		return DomStyle_Static.getOpacity(this);
	}

	@Override
	public final String getOverflow() {
		return DomStyle_Static.getOverflow(this);
	}

	@Override
	public final String getOverflowX() {
		return DomStyle_Static.getOverflowX(this);
	}

	@Override
	public final String getOverflowY() {
		return DomStyle_Static.getOverflowY(this);
	}

	@Override
	public final String getPadding() {
		return DomStyle_Static.getPadding(this);
	}

	@Override
	public final String getPaddingBottom() {
		return DomStyle_Static.getPaddingBottom(this);
	}

	@Override
	public final String getPaddingLeft() {
		return DomStyle_Static.getPaddingLeft(this);
	}

	@Override
	public final String getPaddingRight() {
		return DomStyle_Static.getPaddingRight(this);
	}

	@Override
	public final String getPaddingTop() {
		return DomStyle_Static.getPaddingTop(this);
	}

	@Override
	public final String getPosition() {
		return DomStyle_Static.getPosition(this);
	}

	@Override
	public final String getProperty(String name) {
		return DomStyle_Static.getProperty(this, name);
	}

	@Override
	public final String getRight() {
		return DomStyle_Static.getRight(this);
	}

	@Override
	public final String getTableLayout() {
		return DomStyle_Static.getTableLayout(this);
	}

	@Override
	public final String getTextAlign() {
		return DomStyle_Static.getTextAlign(this);
	}

	@Override
	public final String getTextDecoration() {
		return DomStyle_Static.getTextDecoration(this);
	}

	@Override
	public final String getTextIndent() {
		return DomStyle_Static.getTextIndent(this);
	}

	@Override
	public final String getTextJustify() {
		return DomStyle_Static.getTextJustify(this);
	}

	@Override
	public final String getTextOverflow() {
		return DomStyle_Static.getTextOverflow(this);
	}

	@Override
	public final String getTextTransform() {
		return DomStyle_Static.getTextTransform(this);
	}

	@Override
	public final String getTop() {
		return DomStyle_Static.getTop(this);
	}

	@Override
	public final String getVerticalAlign() {
		return DomStyle_Static.getVerticalAlign(this);
	}

	@Override
	public final String getVisibility() {
		return DomStyle_Static.getVisibility(this);
	}

	@Override
	public final String getWhiteSpace() {
		return DomStyle_Static.getWhiteSpace(this);
	}

	@Override
	public final String getWidth() {
		return DomStyle_Static.getWidth(this);
	}

	@Override
	public final String getZIndex() {
		return DomStyle_Static.getZIndex(this);
	}

	@Override
	public final void setBackgroundColor(String value) {
		DomStyle_Static.setBackgroundColor(this, value);
	}

	@Override
	public final void setBackgroundImage(String value) {
		DomStyle_Static.setBackgroundImage(this, value);
	}

	@Override
	public final void setBorderColor(String value) {
		DomStyle_Static.setBorderColor(this, value);
	}

	@Override
	public final void setBorderStyle(BorderStyle value) {
		DomStyle_Static.setBorderStyle(this, value);
	}

	@Override
	public final void setBorderWidth(double value, Unit unit) {
		DomStyle_Static.setBorderWidth(this, value, unit);
	}

	@Override
	public final void setBottom(double value, Unit unit) {
		DomStyle_Static.setBottom(this, value, unit);
	}

	@Override
	public final void setClear(Clear value) {
		DomStyle_Static.setClear(this, value);
	}

	@Override
	public final void setColor(String value) {
		DomStyle_Static.setColor(this, value);
	}

	@Override
	public final void setCursor(Cursor value) {
		DomStyle_Static.setCursor(this, value);
	}

	@Override
	public final void setDisplay(Display value) {
		DomStyle_Static.setDisplay(this, value);
	}

	@Override
	public final void setFloat(Float value) {
		DomStyle_Static.setFloat(this, value);
	}

	@Override
	public final void setFontSize(double value, Unit unit) {
		DomStyle_Static.setFontSize(this, value, unit);
	}

	@Override
	public final void setFontStyle(FontStyle value) {
		DomStyle_Static.setFontStyle(this, value);
	}

	@Override
	public final void setFontWeight(FontWeight value) {
		DomStyle_Static.setFontWeight(this, value);
	}

	@Override
	public final void setHeight(double value, Unit unit) {
		DomStyle_Static.setHeight(this, value, unit);
	}

	@Override
	public final void setLeft(double value, Unit unit) {
		DomStyle_Static.setLeft(this, value, unit);
	}

	@Override
	public final void setLineHeight(double value, Unit unit) {
		DomStyle_Static.setLineHeight(this, value, unit);
	}

	@Override
	public final void setListStyleType(ListStyleType value) {
		DomStyle_Static.setListStyleType(this, value);
	}

	@Override
	public final void setMargin(double value, Unit unit) {
		DomStyle_Static.setMargin(this, value, unit);
	}

	@Override
	public final void setMarginBottom(double value, Unit unit) {
		DomStyle_Static.setMarginBottom(this, value, unit);
	}

	@Override
	public final void setMarginLeft(double value, Unit unit) {
		DomStyle_Static.setMarginLeft(this, value, unit);
	}

	@Override
	public final void setMarginRight(double value, Unit unit) {
		DomStyle_Static.setMarginRight(this, value, unit);
	}

	@Override
	public final void setMarginTop(double value, Unit unit) {
		DomStyle_Static.setMarginTop(this, value, unit);
	}

	@Override
	public final void setOpacity(double value) {
		DomStyle_Static.setOpacity(this, value);
	}

	@Override
	public final void setOutlineColor(String value) {
		DomStyle_Static.setOutlineColor(this, value);
	}

	@Override
	public final void setOutlineStyle(OutlineStyle value) {
		DomStyle_Static.setOutlineStyle(this, value);
	}

	@Override
	public final void setOutlineWidth(double value, Unit unit) {
		DomStyle_Static.setOutlineWidth(this, value, unit);
	}

	@Override
	public final void setOverflow(Overflow value) {
		DomStyle_Static.setOverflow(this, value);
	}

	@Override
	public final void setOverflowX(Overflow value) {
		DomStyle_Static.setOverflowX(this, value);
	}

	@Override
	public final void setOverflowY(Overflow value) {
		DomStyle_Static.setOverflowY(this, value);
	}

	@Override
	public final void setPadding(double value, Unit unit) {
		DomStyle_Static.setPadding(this, value, unit);
	}

	@Override
	public final void setPaddingBottom(double value, Unit unit) {
		DomStyle_Static.setPaddingBottom(this, value, unit);
	}

	@Override
	public final void setPaddingLeft(double value, Unit unit) {
		DomStyle_Static.setPaddingLeft(this, value, unit);
	}

	@Override
	public final void setPaddingRight(double value, Unit unit) {
		DomStyle_Static.setPaddingRight(this, value, unit);
	}

	@Override
	public final void setPaddingTop(double value, Unit unit) {
		DomStyle_Static.setPaddingTop(this, value, unit);
	}

	@Override
	public final void setPosition(Position value) {
		DomStyle_Static.setPosition(this, value);
	}

	@Override
	public final void setProperty(String name, String value) {
		DomStyle_Static.setProperty(this, name, value);
	}

	@Override
	public final void setProperty(String name, double value, Unit unit) {
		DomStyle_Static.setProperty(this, name, value, unit);
	}

	@Override
	public final void setPropertyPx(String name, int value) {
		DomStyle_Static.setPropertyPx(this, name, value);
	}

	@Override
	public final void setRight(double value, Unit unit) {
		DomStyle_Static.setRight(this, value, unit);
	}

	@Override
	public final void setTableLayout(TableLayout value) {
		DomStyle_Static.setTableLayout(this, value);
	}

	@Override
	public final void setTextAlign(TextAlign value) {
		DomStyle_Static.setTextAlign(this, value);
	}

	@Override
	public final void setTextDecoration(TextDecoration value) {
		DomStyle_Static.setTextDecoration(this, value);
	}

	@Override
	public final void setTextIndent(double value, Unit unit) {
		DomStyle_Static.setTextIndent(this, value, unit);
	}

	@Override
	public final void setTextJustify(TextJustify value) {
		DomStyle_Static.setTextJustify(this, value);
	}

	@Override
	public final void setTextOverflow(TextOverflow value) {
		DomStyle_Static.setTextOverflow(this, value);
	}

	@Override
	public final void setTextTransform(TextTransform value) {
		DomStyle_Static.setTextTransform(this, value);
	}

	@Override
	public final void setTop(double value, Unit unit) {
		DomStyle_Static.setTop(this, value, unit);
	}

	@Override
	public final void setVerticalAlign(VerticalAlign value) {
		DomStyle_Static.setVerticalAlign(this, value);
	}

	@Override
	public final void setVerticalAlign(double value, Unit unit) {
		DomStyle_Static.setVerticalAlign(this, value, unit);
	}

	@Override
	public final void setVisibility(Visibility value) {
		DomStyle_Static.setVisibility(this, value);
	}

	@Override
	public final void setWhiteSpace(WhiteSpace value) {
		DomStyle_Static.setWhiteSpace(this, value);
	}

	@Override
	public final void setWidth(double value, Unit unit) {
		DomStyle_Static.setWidth(this, value, unit);
	}

	@Override
	public final void setZIndex(int value) {
		DomStyle_Static.setZIndex(this, value);
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}
}
