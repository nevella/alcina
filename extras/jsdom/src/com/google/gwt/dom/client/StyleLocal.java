package com.google.gwt.dom.client;

import java.util.Arrays;
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

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TextUtils;

public class StyleLocal implements ClientDomStyle {
	LightMap<String, String> properties = new LightMap<>();

	private Style styleObject;

	public StyleLocal(Style style) {
		this.styleObject = style;
		String styleAttribute = style.element.getAttribute("style");
		if (Ax.notBlank(styleAttribute)) {
			Arrays.stream(styleAttribute.split(";")).forEach(entry -> {
				String kv = TextUtils.normalizeWhitespaceAndTrim(entry);
				String key = kv.replaceFirst("(.+?):\\s?(.+)", "$1");
				String value = kv.replaceFirst("(.+?):\\s?(.+)", "$2");
				setPropertyImpl(LocalDom.jsCssName(key), value);
			});
		}
	}

	@Override
	public void clearBackgroundColor() {
		ClientDomStyleStatic.clearBackgroundColor(this);
	}

	@Override
	public void clearBackgroundImage() {
		ClientDomStyleStatic.clearBackgroundImage(this);
	}

	@Override
	public void clearBorderColor() {
		ClientDomStyleStatic.clearBorderColor(this);
	}

	@Override
	public void clearBorderStyle() {
		ClientDomStyleStatic.clearBorderStyle(this);
	}

	@Override
	public void clearBorderWidth() {
		ClientDomStyleStatic.clearBorderWidth(this);
	}

	@Override
	public void clearBottom() {
		ClientDomStyleStatic.clearBottom(this);
	}

	@Override
	public void clearClear() {
		ClientDomStyleStatic.clearClear(this);
	}

	@Override
	public void clearColor() {
		ClientDomStyleStatic.clearColor(this);
	}

	@Override
	public void clearCursor() {
		ClientDomStyleStatic.clearCursor(this);
	}

	@Override
	public void clearDisplay() {
		ClientDomStyleStatic.clearDisplay(this);
	}

	@Override
	public void clearFloat() {
		ClientDomStyleStatic.clearFloat(this);
	}

	@Override
	public void clearFontSize() {
		ClientDomStyleStatic.clearFontSize(this);
	}

	@Override
	public void clearFontStyle() {
		ClientDomStyleStatic.clearFontStyle(this);
	}

	@Override
	public void clearFontWeight() {
		ClientDomStyleStatic.clearFontWeight(this);
	}

	@Override
	public void clearHeight() {
		ClientDomStyleStatic.clearHeight(this);
	}

	@Override
	public void clearLeft() {
		ClientDomStyleStatic.clearLeft(this);
	}

	@Override
	public void clearLineHeight() {
		ClientDomStyleStatic.clearLineHeight(this);
	}

	@Override
	public void clearListStyleType() {
		ClientDomStyleStatic.clearListStyleType(this);
	}

	@Override
	public void clearMargin() {
		ClientDomStyleStatic.clearMargin(this);
	}

	@Override
	public void clearMarginBottom() {
		ClientDomStyleStatic.clearMarginBottom(this);
	}

	@Override
	public void clearMarginLeft() {
		ClientDomStyleStatic.clearMarginLeft(this);
	}

	@Override
	public void clearMarginRight() {
		ClientDomStyleStatic.clearMarginRight(this);
	}

	@Override
	public void clearMarginTop() {
		ClientDomStyleStatic.clearMarginTop(this);
	}

	@Override
	public void clearOpacity() {
		ClientDomStyleStatic.clearOpacity(this);
	}

	@Override
	public void clearOutlineColor() {
		ClientDomStyleStatic.clearOutlineColor(this);
	}

	@Override
	public void clearOutlineStyle() {
		ClientDomStyleStatic.clearOutlineStyle(this);
	}

	@Override
	public void clearOutlineWidth() {
		ClientDomStyleStatic.clearOutlineWidth(this);
	}

	@Override
	public void clearOverflow() {
		ClientDomStyleStatic.clearOverflow(this);
	}

	@Override
	public void clearOverflowX() {
		ClientDomStyleStatic.clearOverflowX(this);
	}

	@Override
	public void clearOverflowY() {
		ClientDomStyleStatic.clearOverflowY(this);
	}

	@Override
	public void clearPadding() {
		ClientDomStyleStatic.clearPadding(this);
	}

	@Override
	public void clearPaddingBottom() {
		ClientDomStyleStatic.clearPaddingBottom(this);
	}

	@Override
	public void clearPaddingLeft() {
		ClientDomStyleStatic.clearPaddingLeft(this);
	}

	@Override
	public void clearPaddingRight() {
		ClientDomStyleStatic.clearPaddingRight(this);
	}

	@Override
	public void clearPaddingTop() {
		ClientDomStyleStatic.clearPaddingTop(this);
	}

	@Override
	public void clearPosition() {
		ClientDomStyleStatic.clearPosition(this);
	}

	@Override
	public void clearProperty(String name) {
		ClientDomStyleStatic.clearProperty(this, name);
	}

	@Override
	public void clearRight() {
		ClientDomStyleStatic.clearRight(this);
	}

	@Override
	public void clearTableLayout() {
		ClientDomStyleStatic.clearTableLayout(this);
	}

	@Override
	public void clearTextAlign() {
		ClientDomStyleStatic.clearTextAlign(this);
	}

	@Override
	public void clearTextDecoration() {
		ClientDomStyleStatic.clearTextDecoration(this);
	}

	@Override
	public void clearTextIndent() {
		ClientDomStyleStatic.clearTextIndent(this);
	}

	@Override
	public void clearTextJustify() {
		ClientDomStyleStatic.clearTextJustify(this);
	}

	@Override
	public void clearTextOverflow() {
		ClientDomStyleStatic.clearTextOverflow(this);
	}

	@Override
	public void clearTextTransform() {
		ClientDomStyleStatic.clearTextTransform(this);
	}

	@Override
	public void clearTop() {
		ClientDomStyleStatic.clearTop(this);
	}

	@Override
	public void clearVisibility() {
		ClientDomStyleStatic.clearVisibility(this);
	}

	@Override
	public void clearWhiteSpace() {
		ClientDomStyleStatic.clearWhiteSpace(this);
	}

	@Override
	public void clearWidth() {
		ClientDomStyleStatic.clearWidth(this);
	}

	@Override
	public void clearZIndex() {
		ClientDomStyleStatic.clearZIndex(this);
	}

	void cloneStyleFrom(ClientDomStyle other, Style to) {
		StyleLocal clone = new StyleLocal(to);
		clone.properties.putAll(properties);
	}

	@Override
	public String getBackgroundColor() {
		return ClientDomStyleStatic.getBackgroundColor(this);
	}

	@Override
	public String getBackgroundImage() {
		return ClientDomStyleStatic.getBackgroundImage(this);
	}

	@Override
	public String getBorderColor() {
		return ClientDomStyleStatic.getBorderColor(this);
	}

	@Override
	public String getBorderStyle() {
		return ClientDomStyleStatic.getBorderStyle(this);
	}

	@Override
	public String getBorderWidth() {
		return ClientDomStyleStatic.getBorderWidth(this);
	}

	@Override
	public String getBottom() {
		return ClientDomStyleStatic.getBottom(this);
	}

	@Override
	public String getClear() {
		return ClientDomStyleStatic.getClear(this);
	}

	@Override
	public String getColor() {
		return ClientDomStyleStatic.getColor(this);
	}

	@Override
	public String getCursor() {
		return ClientDomStyleStatic.getCursor(this);
	}

	@Override
	public String getDisplay() {
		return ClientDomStyleStatic.getDisplay(this);
	}

	@Override
	public final Style.Display getDisplayTyped() {
		return ClientDomStyleStatic.getDisplayTyped(this);
	}

	@Override
	public String getFontSize() {
		return ClientDomStyleStatic.getFontSize(this);
	}

	@Override
	public String getFontStyle() {
		return ClientDomStyleStatic.getFontStyle(this);
	}

	@Override
	public String getFontWeight() {
		return ClientDomStyleStatic.getFontWeight(this);
	}

	@Override
	public String getHeight() {
		return ClientDomStyleStatic.getHeight(this);
	}

	@Override
	public String getLeft() {
		return ClientDomStyleStatic.getLeft(this);
	}

	@Override
	public String getLineHeight() {
		return ClientDomStyleStatic.getLineHeight(this);
	}

	@Override
	public String getListStyleType() {
		return ClientDomStyleStatic.getListStyleType(this);
	}

	@Override
	public String getMargin() {
		return ClientDomStyleStatic.getMargin(this);
	}

	@Override
	public String getMarginBottom() {
		return ClientDomStyleStatic.getMarginBottom(this);
	}

	@Override
	public String getMarginLeft() {
		return ClientDomStyleStatic.getMarginLeft(this);
	}

	@Override
	public String getMarginRight() {
		return ClientDomStyleStatic.getMarginRight(this);
	}

	@Override
	public String getMarginTop() {
		return ClientDomStyleStatic.getMarginTop(this);
	}

	@Override
	public String getOpacity() {
		return ClientDomStyleStatic.getOpacity(this);
	}

	@Override
	public String getOverflow() {
		return ClientDomStyleStatic.getOverflow(this);
	}

	@Override
	public String getOverflowX() {
		return ClientDomStyleStatic.getOverflowX(this);
	}

	@Override
	public String getOverflowY() {
		return ClientDomStyleStatic.getOverflowY(this);
	}

	@Override
	public String getPadding() {
		return ClientDomStyleStatic.getPadding(this);
	}

	@Override
	public String getPaddingBottom() {
		return ClientDomStyleStatic.getPaddingBottom(this);
	}

	@Override
	public String getPaddingLeft() {
		return ClientDomStyleStatic.getPaddingLeft(this);
	}

	@Override
	public String getPaddingRight() {
		return ClientDomStyleStatic.getPaddingRight(this);
	}

	@Override
	public String getPaddingTop() {
		return ClientDomStyleStatic.getPaddingTop(this);
	}

	@Override
	public String getPosition() {
		return ClientDomStyleStatic.getPosition(this);
	}

	@Override
	public final Style.Position getPositionTyped() {
		return ClientDomStyleStatic.getPositionTyped(this);
	}

	public boolean hasProperty(String propertyName) {
		return properties.containsKey(propertyName);
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String getProperty(String name) {
		return ClientDomStyleStatic.getProperty(this, name);
	}

	@Override
	public String getPropertyImpl(String name) {
		return properties.getOrDefault(name, "");
	}

	@Override
	public String getRight() {
		return ClientDomStyleStatic.getRight(this);
	}

	@Override
	public String getTableLayout() {
		return ClientDomStyleStatic.getTableLayout(this);
	}

	@Override
	public String getTextAlign() {
		return ClientDomStyleStatic.getTextAlign(this);
	}

	@Override
	public String getTextDecoration() {
		return ClientDomStyleStatic.getTextDecoration(this);
	}

	@Override
	public String getTextIndent() {
		return ClientDomStyleStatic.getTextIndent(this);
	}

	@Override
	public String getTextJustify() {
		return ClientDomStyleStatic.getTextJustify(this);
	}

	@Override
	public String getTextOverflow() {
		return ClientDomStyleStatic.getTextOverflow(this);
	}

	@Override
	public String getTextTransform() {
		return ClientDomStyleStatic.getTextTransform(this);
	}

	@Override
	public String getTop() {
		return ClientDomStyleStatic.getTop(this);
	}

	@Override
	public String getVerticalAlign() {
		return ClientDomStyleStatic.getVerticalAlign(this);
	}

	@Override
	public String getVisibility() {
		return ClientDomStyleStatic.getVisibility(this);
	}

	@Override
	public String getWhiteSpace() {
		return ClientDomStyleStatic.getWhiteSpace(this);
	}

	@Override
	public String getWidth() {
		return ClientDomStyleStatic.getWidth(this);
	}

	@Override
	public String getZIndex() {
		return ClientDomStyleStatic.getZIndex(this);
	}

	boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public void removeProperty(String key) {
		properties.remove(key);
	}

	@Override
	public void setBackgroundColor(String value) {
		ClientDomStyleStatic.setBackgroundColor(this, value);
	}

	@Override
	public void setBackgroundImage(String value) {
		ClientDomStyleStatic.setBackgroundImage(this, value);
	}

	@Override
	public void setBorderColor(String value) {
		ClientDomStyleStatic.setBorderColor(this, value);
	}

	@Override
	public void setBorderStyle(BorderStyle value) {
		ClientDomStyleStatic.setBorderStyle(this, value);
	}

	@Override
	public void setBorderWidth(double value, Unit unit) {
		ClientDomStyleStatic.setBorderWidth(this, value, unit);
	}

	@Override
	public void setBottom(double value, Unit unit) {
		ClientDomStyleStatic.setBottom(this, value, unit);
	}

	@Override
	public void setClear(Clear value) {
		ClientDomStyleStatic.setClear(this, value);
	}

	@Override
	public void setColor(String value) {
		ClientDomStyleStatic.setColor(this, value);
	}

	@Override
	public void setCursor(Cursor value) {
		ClientDomStyleStatic.setCursor(this, value);
	}

	@Override
	public void setDisplay(Display value) {
		ClientDomStyleStatic.setDisplay(this, value);
	}

	@Override
	public void setFloat(Float value) {
		ClientDomStyleStatic.setFloat(this, value);
	}

	@Override
	public void setFontSize(double value, Unit unit) {
		ClientDomStyleStatic.setFontSize(this, value, unit);
	}

	@Override
	public void setFontStyle(FontStyle value) {
		ClientDomStyleStatic.setFontStyle(this, value);
	}

	@Override
	public void setFontWeight(FontWeight value) {
		ClientDomStyleStatic.setFontWeight(this, value);
	}

	@Override
	public void setHeight(double value, Unit unit) {
		ClientDomStyleStatic.setHeight(this, value, unit);
	}

	@Override
	public void setLeft(double value, Unit unit) {
		ClientDomStyleStatic.setLeft(this, value, unit);
	}

	@Override
	public void setLineHeight(double value, Unit unit) {
		ClientDomStyleStatic.setLineHeight(this, value, unit);
	}

	@Override
	public void setListStyleType(ListStyleType value) {
		ClientDomStyleStatic.setListStyleType(this, value);
	}

	@Override
	public void setMargin(double value, Unit unit) {
		ClientDomStyleStatic.setMargin(this, value, unit);
	}

	@Override
	public void setMarginBottom(double value, Unit unit) {
		ClientDomStyleStatic.setMarginBottom(this, value, unit);
	}

	@Override
	public void setMarginLeft(double value, Unit unit) {
		ClientDomStyleStatic.setMarginLeft(this, value, unit);
	}

	@Override
	public void setMarginRight(double value, Unit unit) {
		ClientDomStyleStatic.setMarginRight(this, value, unit);
	}

	@Override
	public void setMarginTop(double value, Unit unit) {
		ClientDomStyleStatic.setMarginTop(this, value, unit);
	}

	@Override
	public void setOpacity(String value) {
		ClientDomStyleStatic.setOpacity(this, value);
	}

	@Override
	public void setOutlineColor(String value) {
		ClientDomStyleStatic.setOutlineColor(this, value);
	}

	@Override
	public void setOutlineStyle(OutlineStyle value) {
		ClientDomStyleStatic.setOutlineStyle(this, value);
	}

	@Override
	public void setOutlineWidth(double value, Unit unit) {
		ClientDomStyleStatic.setOutlineWidth(this, value, unit);
	}

	@Override
	public void setOverflow(Overflow value) {
		ClientDomStyleStatic.setOverflow(this, value);
	}

	@Override
	public void setOverflowX(Overflow value) {
		ClientDomStyleStatic.setOverflowX(this, value);
	}

	@Override
	public void setOverflowY(Overflow value) {
		ClientDomStyleStatic.setOverflowY(this, value);
	}

	@Override
	public void setPadding(double value, Unit unit) {
		ClientDomStyleStatic.setPadding(this, value, unit);
	}

	@Override
	public void setPaddingBottom(double value, Unit unit) {
		ClientDomStyleStatic.setPaddingBottom(this, value, unit);
	}

	@Override
	public void setPaddingLeft(double value, Unit unit) {
		ClientDomStyleStatic.setPaddingLeft(this, value, unit);
	}

	@Override
	public void setPaddingRight(double value, Unit unit) {
		ClientDomStyleStatic.setPaddingRight(this, value, unit);
	}

	@Override
	public void setPaddingTop(double value, Unit unit) {
		ClientDomStyleStatic.setPaddingTop(this, value, unit);
	}

	@Override
	public void setPosition(Position value) {
		ClientDomStyleStatic.setPosition(this, value);
	}

	@Override
	public void setProperty(String name, double value, Unit unit) {
		ClientDomStyleStatic.setProperty(this, name, value, unit);
	}

	@Override
	public void setProperty(String name, String value) {
		ClientDomStyleStatic.setProperty(this, name, value);
	}

	@Override
	public void setPropertyImpl(String name, String value) {
		properties.put(name, value);
	}

	@Override
	public void setPropertyPx(String name, int value) {
		ClientDomStyleStatic.setPropertyPx(this, name, value);
	}

	@Override
	public void setRight(double value, Unit unit) {
		ClientDomStyleStatic.setRight(this, value, unit);
	}

	@Override
	public void setTableLayout(TableLayout value) {
		ClientDomStyleStatic.setTableLayout(this, value);
	}

	@Override
	public void setTextAlign(TextAlign value) {
		ClientDomStyleStatic.setTextAlign(this, value);
	}

	@Override
	public void setTextDecoration(TextDecoration value) {
		ClientDomStyleStatic.setTextDecoration(this, value);
	}

	@Override
	public void setTextIndent(double value, Unit unit) {
		ClientDomStyleStatic.setTextIndent(this, value, unit);
	}

	@Override
	public void setTextJustify(TextJustify value) {
		ClientDomStyleStatic.setTextJustify(this, value);
	}

	@Override
	public void setTextOverflow(TextOverflow value) {
		ClientDomStyleStatic.setTextOverflow(this, value);
	}

	@Override
	public void setTextTransform(TextTransform value) {
		ClientDomStyleStatic.setTextTransform(this, value);
	}

	@Override
	public void setTop(double value, Unit unit) {
		ClientDomStyleStatic.setTop(this, value, unit);
	}

	@Override
	public void setVerticalAlign(double value, Unit unit) {
		ClientDomStyleStatic.setVerticalAlign(this, value, unit);
	}

	@Override
	public void setVerticalAlign(VerticalAlign value) {
		ClientDomStyleStatic.setVerticalAlign(this, value);
	}

	@Override
	public void setVisibility(Visibility value) {
		ClientDomStyleStatic.setVisibility(this, value);
	}

	@Override
	public void setWhiteSpace(WhiteSpace value) {
		ClientDomStyleStatic.setWhiteSpace(this, value);
	}

	@Override
	public void setWidth(double value, Unit unit) {
		ClientDomStyleStatic.setWidth(this, value, unit);
	}

	@Override
	public void setZIndex(int value) {
		ClientDomStyleStatic.setZIndex(this, value);
	}

	@Override
	public Style styleObject() {
		return styleObject;
	}
}
