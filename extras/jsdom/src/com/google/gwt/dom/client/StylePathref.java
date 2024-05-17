package com.google.gwt.dom.client;

import java.util.List;
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

/*
 * TODO - romcom - style setters should really be batched, collated and flushed
 * as a multi-invoke
 */
public class StylePathref implements ClientDomStyle {
	ElementPathref element;

	StylePathref(ElementPathref element) {
		this.element = element;
	}

	@Override
	public void clearBackgroundColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearBackgroundImage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearBorderColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearBorderStyle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearBorderWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearBottom() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearClear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearCursor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearDisplay() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearFloat() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearFontSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearFontStyle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearFontWeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearLineHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearListStyleType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearMargin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearMarginBottom() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearMarginLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearMarginRight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearMarginTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearOpacity() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearOutlineColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearOutlineStyle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearOutlineWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearOverflow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearOverflowX() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearOverflowY() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearPadding() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearPaddingBottom() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearPaddingLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearPaddingRight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearPaddingTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearPosition() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearProperty(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearRight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTableLayout() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTextAlign() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTextDecoration() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTextIndent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTextJustify() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTextOverflow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTextTransform() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearVisibility() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearWhiteSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearZIndex() {
		throw new UnsupportedOperationException();
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
	public Display getDisplayTyped() {
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
	public Position getPositionTyped() {
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBackgroundImage(String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBorderColor(String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBorderStyle(BorderStyle value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBorderWidth(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBottom(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setClear(Clear value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setColor(String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCursor(Cursor value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDisplay(Display value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFloat(Float value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFontSize(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFontStyle(FontStyle value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFontWeight(FontWeight value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setHeight(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLeft(double value, Unit unit) {
		element.invokeStyle("setLeft", List.of(double.class, Unit.class),
				List.of(value, unit));
	}

	@Override
	public void setLineHeight(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setListStyleType(ListStyleType value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMargin(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMarginBottom(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMarginLeft(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMarginRight(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMarginTop(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOpacity(String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOutlineColor(String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOutlineStyle(OutlineStyle value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOutlineWidth(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOverflow(Overflow value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOverflowX(Overflow value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOverflowY(Overflow value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPadding(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPaddingBottom(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPaddingLeft(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPaddingRight(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPaddingTop(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPosition(Position value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setProperty(String name, double value, Unit unit) {
		element.invokeStyle("setProperty",
				List.of(String.class, double.class, Unit.class),
				List.of(name, value, unit));
	}

	@Override
	public void setProperty(String name, String value) {
		element.invokeStyle("setProperty", List.of(String.class, double.class),
				List.of(name, value));
	}

	@Override
	public void setPropertyImpl(String name, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPropertyPx(String name, int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRight(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTableLayout(TableLayout value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTextAlign(TextAlign value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTextDecoration(TextDecoration value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTextIndent(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTextJustify(TextJustify value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTextOverflow(TextOverflow value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTextTransform(TextTransform value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTop(double value, Unit unit) {
		element.invokeStyle("setTop", List.of(double.class, Unit.class),
				List.of(value, unit));
	}

	@Override
	public void setVerticalAlign(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setVerticalAlign(VerticalAlign value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setVisibility(Visibility value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setWhiteSpace(WhiteSpace value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setWidth(double value, Unit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setZIndex(int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Style styleObject() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeProperty(String key) {
		element.invokeStyle("removeProperty", List.of(String.class),
				List.of(key));
	}
}
