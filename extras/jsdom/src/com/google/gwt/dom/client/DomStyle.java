package com.google.gwt.dom.client;

import com.google.gwt.core.client.SingleJsoImpl;
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
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

public interface DomStyle {
	/**
	 * Clear the background-color css property.
	 */
	default void clearBackgroundColor() {
		DomStyle_Static.clearBackgroundColor(this);
	}

	/**
	 * Clear the background-image css property.
	 */
	default void clearBackgroundImage() {
		DomStyle_Static.clearBackgroundImage(this);
	}

	/**
	 * Clear the border-color css property.
	 */
	default void clearBorderColor() {
		DomStyle_Static.clearBorderColor(this);
	}

	/**
	 * Clears the border-style CSS property.
	 */
	default void clearBorderStyle() {
		DomStyle_Static.clearBorderStyle(this);
	}

	/**
	 * Clear the border-width css property.
	 */
	default void clearBorderWidth() {
		DomStyle_Static.clearBorderWidth(this);
	}

	/**
	 * Clear the bottom css property.
	 */
	default void clearBottom() {
		DomStyle_Static.clearBottom(this);
	}

	/**
	 * Clear the 'clear' CSS property.
	 */
	default void clearClear() {
		DomStyle_Static.clearClear(this);
	}

	/**
	 * Clear the color css property.
	 */
	default void clearColor() {
		DomStyle_Static.clearColor(this);
	}

	/**
	 * Clears the cursor CSS property.
	 */
	default void clearCursor() {
		DomStyle_Static.clearCursor(this);
	}

	/**
	 * Clears the display CSS property.
	 */
	default void clearDisplay() {
		DomStyle_Static.clearDisplay(this);
	}

	/**
	 * Clear the float css property.
	 */
	default void clearFloat() {
		DomStyle_Static.clearFloat(this);
	}

	/**
	 * Clear the font-size css property.
	 */
	default void clearFontSize() {
		DomStyle_Static.clearFontSize(this);
	}

	/**
	 * Clears the font-style CSS property.
	 */
	default void clearFontStyle() {
		DomStyle_Static.clearFontStyle(this);
	}

	/**
	 * Clears the font-weight CSS property.
	 */
	default void clearFontWeight() {
		DomStyle_Static.clearFontWeight(this);
	}

	/**
	 * Clear the height css property.
	 */
	default void clearHeight() {
		DomStyle_Static.clearHeight(this);
	}

	/**
	 * Clear the left css property.
	 */
	default void clearLeft() {
		DomStyle_Static.clearLeft(this);
	}

	/**
	 * Clear the line-height css property.
	 */
	default void clearLineHeight() {
		DomStyle_Static.clearLineHeight(this);
	}

	/**
	 * Clears the list-style-type CSS property.
	 */
	default void clearListStyleType() {
		DomStyle_Static.clearListStyleType(this);
	}

	/**
	 * Clear the margin css property.
	 */
	default void clearMargin() {
		DomStyle_Static.clearMargin(this);
	}

	/**
	 * Clear the margin-bottom css property.
	 */
	default void clearMarginBottom() {
		DomStyle_Static.clearMarginBottom(this);
	}

	/**
	 * Clear the margin-left css property.
	 */
	default void clearMarginLeft() {
		DomStyle_Static.clearMarginLeft(this);
	}

	/**
	 * Clear the margin-right css property.
	 */
	default void clearMarginRight() {
		DomStyle_Static.clearMarginRight(this);
	}

	/**
	 * Clear the margin-top css property.
	 */
	default void clearMarginTop() {
		DomStyle_Static.clearMarginTop(this);
	}

	/**
	 * Clear the opacity css property.
	 */
	default void clearOpacity() {
		DomStyle_Static.clearOpacity(this);
	}

	Style styleObject();

	/**
	 * Clear the outline-color css property.
	 */
	default void clearOutlineColor() {
		DomStyle_Static.clearOutlineColor(this);
	}

	/**
	 * Clears the outline-style CSS property.
	 */
	default void clearOutlineStyle() {
		DomStyle_Static.clearOutlineStyle(this);
	}

	/**
	 * Clear the outline-width css property.
	 */
	default void clearOutlineWidth() {
		DomStyle_Static.clearOutlineWidth(this);
	}

	/**
	 * Clears the overflow CSS property.
	 */
	default void clearOverflow() {
		DomStyle_Static.clearOverflow(this);
	}

	/**
	 * Clears the overflow-x CSS property.
	 */
	default void clearOverflowX() {
		DomStyle_Static.clearOverflowX(this);
	}

	/**
	 * Clears the overflow-y CSS property.
	 */
	default void clearOverflowY() {
		DomStyle_Static.clearOverflowY(this);
	}

	/**
	 * Clear the padding css property.
	 */
	default void clearPadding() {
		DomStyle_Static.clearPadding(this);
	}

	/**
	 * Clear the padding-bottom css property.
	 */
	default void clearPaddingBottom() {
		DomStyle_Static.clearPaddingBottom(this);
	}

	/**
	 * Clear the padding-left css property.
	 */
	default void clearPaddingLeft() {
		DomStyle_Static.clearPaddingLeft(this);
	}

	/**
	 * Clear the padding-right css property.
	 */
	default void clearPaddingRight() {
		DomStyle_Static.clearPaddingRight(this);
	}

	/**
	 * Clear the padding-top css property.
	 */
	default void clearPaddingTop() {
		DomStyle_Static.clearPaddingTop(this);
	}

	/**
	 * Clears the position CSS property.
	 */
	default void clearPosition() {
		DomStyle_Static.clearPosition(this);
	}

	/**
	 * Clears the value of a named property, causing it to revert to its
	 * default.
	 */
	default void clearProperty(String name) {
		DomStyle_Static.clearProperty(this, name);
	}

	/**
	 * Clear the right css property.
	 */
	default void clearRight() {
		DomStyle_Static.clearRight(this);
	}

	/**
	 * Clear the table-layout css property.
	 */
	default void clearTableLayout() {
		DomStyle_Static.clearTableLayout(this);
	}

	/**
	 * Clear the 'text-align' CSS property.
	 */
	default void clearTextAlign() {
		DomStyle_Static.clearTextAlign(this);
	}

	/**
	 * Clears the text-decoration CSS property.
	 */
	default void clearTextDecoration() {
		DomStyle_Static.clearTextDecoration(this);
	}

	/**
	 * Clear the 'text-indent' CSS property.
	 */
	default void clearTextIndent() {
		DomStyle_Static.clearTextIndent(this);
	}

	/**
	 * Clear the 'text-justify' CSS3 property.
	 */
	default void clearTextJustify() {
		DomStyle_Static.clearTextJustify(this);
	}

	/**
	 * Clear the 'text-overflow' CSS3 property.
	 */
	default void clearTextOverflow() {
		DomStyle_Static.clearTextOverflow(this);
	}

	/**
	 * Clear the 'text-transform' CSS property.
	 */
	default void clearTextTransform() {
		DomStyle_Static.clearTextTransform(this);
	}

	/**
	 * Clear the top css property.
	 */
	default void clearTop() {
		DomStyle_Static.clearTop(this);
	}

	/**
	 * Clears the visibility CSS property.
	 */
	default void clearVisibility() {
		DomStyle_Static.clearVisibility(this);
	}

	/**
	 * Clear the 'white-space' CSS property.
	 */
	default void clearWhiteSpace() {
		DomStyle_Static.clearWhiteSpace(this);
	}

	/**
	 * Clear the width css property.
	 */
	default void clearWidth() {
		DomStyle_Static.clearWidth(this);
	}

	/**
	 * Clear the z-index css property.
	 */
	default void clearZIndex() {
		DomStyle_Static.clearZIndex(this);
	}

	/**
	 * Get the background-color css property.
	 */
	default String getBackgroundColor() {
		return DomStyle_Static.getBackgroundColor(this);
	}

	/**
	 * Get the background-image css property.
	 */
	default String getBackgroundImage() {
		return DomStyle_Static.getBackgroundImage(this);
	}

	/**
	 * Get the border-color css property.
	 */
	default String getBorderColor() {
		return DomStyle_Static.getBorderColor(this);
	}

	/**
	 * Gets the border-style CSS property.
	 */
	default String getBorderStyle() {
		return DomStyle_Static.getBorderStyle(this);
	}

	/**
	 * Get the border-width css property.
	 */
	default String getBorderWidth() {
		return DomStyle_Static.getBorderWidth(this);
	}

	/**
	 * Get the bottom css property.
	 */
	default String getBottom() {
		return DomStyle_Static.getBottom(this);
	}

	/**
	 * Get the 'clear' CSS property.
	 */
	default String getClear() {
		return DomStyle_Static.getClear(this);
	}

	/**
	 * Get the color css property.
	 */
	default String getColor() {
		return DomStyle_Static.getColor(this);
	}

	/**
	 * Gets the cursor CSS property.
	 */
	default String getCursor() {
		return DomStyle_Static.getCursor(this);
	}

	/**
	 * Gets the display CSS property.
	 */
	default String getDisplay() {
		return DomStyle_Static.getDisplay(this);
	}

	/**
	 * Get the font-size css property.
	 */
	default String getFontSize() {
		return DomStyle_Static.getFontSize(this);
	}

	/**
	 * Gets the font-style CSS property.
	 */
	default String getFontStyle() {
		return DomStyle_Static.getFontStyle(this);
	}

	/**
	 * Gets the font-weight CSS property.
	 */
	default String getFontWeight() {
		return DomStyle_Static.getFontWeight(this);
	}

	/**
	 * Get the height css property.
	 */
	default String getHeight() {
		return DomStyle_Static.getHeight(this);
	}

	/**
	 * Get the left css property.
	 */
	default String getLeft() {
		return DomStyle_Static.getLeft(this);
	}

	/**
	 * Get the line-height css property.
	 */
	default String getLineHeight() {
		return DomStyle_Static.getLineHeight(this);
	}

	/**
	 * Gets the list-style-type CSS property.
	 */
	default String getListStyleType() {
		return DomStyle_Static.getListStyleType(this);
	}

	/**
	 * Get the margin css property.
	 */
	default String getMargin() {
		return DomStyle_Static.getMargin(this);
	}

	/**
	 * Get the margin-bottom css property.
	 */
	default String getMarginBottom() {
		return DomStyle_Static.getMarginBottom(this);
	}

	/**
	 * Get the margin-left css property.
	 */
	default String getMarginLeft() {
		return DomStyle_Static.getMarginLeft(this);
	}

	/**
	 * Get the margin-right css property.
	 */
	default String getMarginRight() {
		return DomStyle_Static.getMarginRight(this);
	}

	/**
	 * Get the margin-top css property.
	 */
	default String getMarginTop() {
		return DomStyle_Static.getMarginTop(this);
	}

	/**
	 * Get the opacity css property.
	 */
	default String getOpacity() {
		return DomStyle_Static.getOpacity(this);
	}

	/**
	 * Gets the overflow CSS property.
	 */
	default String getOverflow() {
		return DomStyle_Static.getOverflow(this);
	}

	/**
	 * Gets the overflow-x CSS property.
	 */
	default String getOverflowX() {
		return DomStyle_Static.getOverflowX(this);
	}

	/**
	 * Gets the overflow-y CSS property.
	 */
	default String getOverflowY() {
		return DomStyle_Static.getOverflowY(this);
	}

	/**
	 * Get the padding css property.
	 */
	default String getPadding() {
		return DomStyle_Static.getPadding(this);
	}

	/**
	 * Get the padding-bottom css property.
	 */
	default String getPaddingBottom() {
		return DomStyle_Static.getPaddingBottom(this);
	}

	/**
	 * Get the padding-left css property.
	 */
	default String getPaddingLeft() {
		return DomStyle_Static.getPaddingLeft(this);
	}

	/**
	 * Get the padding-right css property.
	 */
	default String getPaddingRight() {
		return DomStyle_Static.getPaddingRight(this);
	}

	/**
	 * Get the padding-top css property.
	 */
	default String getPaddingTop() {
		return DomStyle_Static.getPaddingTop(this);
	}

	/**
	 * Gets the position CSS property.
	 */
	default String getPosition() {
		return DomStyle_Static.getPosition(this);
	}

	/**
	 * Gets the value of a named property.
	 */
	default String getProperty(String name) {
		return DomStyle_Static.getProperty(this, name);
	}

	/**
	 * Get the right css property.
	 */
	default String getRight() {
		return DomStyle_Static.getRight(this);
	}

	/**
	 * Gets the table-layout property.
	 */
	default String getTableLayout() {
		return DomStyle_Static.getTableLayout(this);
	}

	/**
	 * Get the 'text-align' CSS property.
	 */
	default String getTextAlign() {
		return DomStyle_Static.getTextAlign(this);
	}

	/**
	 * Gets the text-decoration CSS property.
	 */
	default String getTextDecoration() {
		return DomStyle_Static.getTextDecoration(this);
	}

	/**
	 * Get the 'text-indent' CSS property.
	 */
	default String getTextIndent() {
		return DomStyle_Static.getTextIndent(this);
	}

	/**
	 * Get the 'text-justify' CSS3 property.
	 */
	default String getTextJustify() {
		return DomStyle_Static.getTextJustify(this);
	}

	/**
	 * Get the 'text-overflow' CSS3 property.
	 */
	default String getTextOverflow() {
		return DomStyle_Static.getTextOverflow(this);
	}

	/**
	 * Get the 'text-transform' CSS property.
	 */
	default String getTextTransform() {
		return DomStyle_Static.getTextTransform(this);
	}

	/**
	 * Get the top css property.
	 */
	default String getTop() {
		return DomStyle_Static.getTop(this);
	}

	/**
	 * Gets the vertical-align CSS property.
	 */
	default String getVerticalAlign() {
		return DomStyle_Static.getVerticalAlign(this);
	}

	/**
	 * Gets the visibility CSS property.
	 */
	default String getVisibility() {
		return DomStyle_Static.getVisibility(this);
	}

	/**
	 * Get the 'white-space' CSS property.
	 */
	default String getWhiteSpace() {
		return DomStyle_Static.getWhiteSpace(this);
	}

	/**
	 * Get the width css property.
	 */
	default String getWidth() {
		return DomStyle_Static.getWidth(this);
	}

	/**
	 * Get the z-index css property.
	 */
	default String getZIndex() {
		return DomStyle_Static.getZIndex(this);
	}

	/**
	 * Set the background-color css property.
	 */
	default void setBackgroundColor(String value) {
		DomStyle_Static.setBackgroundColor(this, value);
	}

	/**
	 * Set the background-image css property.
	 */
	default void setBackgroundImage(@IsSafeUri String value) {
		DomStyle_Static.setBackgroundImage(this, value);
	}

	/**
	 * Set the border-color css property.
	 */
	default void setBorderColor(String value) {
		DomStyle_Static.setBorderColor(this, value);
	}

	/**
	 * Sets the border-style CSS property.
	 */
	default void setBorderStyle(BorderStyle value) {
		DomStyle_Static.setBorderStyle(this, value);
	}

	/**
	 * Set the border-width css property.
	 */
	default void setBorderWidth(double value, Unit unit) {
		DomStyle_Static.setBorderWidth(this, value, unit);
	}

	/**
	 * Set the bottom css property.
	 */
	default void setBottom(double value, Unit unit) {
		DomStyle_Static.setBottom(this, value, unit);
	}

	/**
	 * Sets the 'clear' CSS property.
	 */
	default void setClear(Clear value) {
		DomStyle_Static.setClear(this, value);
	}

	/**
	 * Sets the color CSS property.
	 */
	default void setColor(String value) {
		DomStyle_Static.setColor(this, value);
	}

	/**
	 * Sets the cursor CSS property.
	 */
	default void setCursor(Cursor value) {
		DomStyle_Static.setCursor(this, value);
	}

	/**
	 * Sets the display CSS property.
	 */
	default void setDisplay(Display value) {
		DomStyle_Static.setDisplay(this, value);
	}

	/**
	 * Set the float css property.
	 */
	default void setFloat(Float value) {
		DomStyle_Static.setFloat(this, value);
	}

	/**
	 * Set the font-size css property.
	 */
	default void setFontSize(double value, Unit unit) {
		DomStyle_Static.setFontSize(this, value, unit);
	}

	/**
	 * Sets the font-style CSS property.
	 */
	default void setFontStyle(FontStyle value) {
		DomStyle_Static.setFontStyle(this, value);
	}

	/**
	 * Sets the font-weight CSS property.
	 */
	default void setFontWeight(FontWeight value) {
		DomStyle_Static.setFontWeight(this, value);
	}

	/**
	 * Set the height css property.
	 */
	default void setHeight(double value, Unit unit) {
		DomStyle_Static.setHeight(this, value, unit);
	}

	/**
	 * Set the left css property.
	 */
	default void setLeft(double value, Unit unit) {
		DomStyle_Static.setLeft(this, value, unit);
	}

	/**
	 * Set the line-height css property.
	 */
	default void setLineHeight(double value, Unit unit) {
		DomStyle_Static.setLineHeight(this, value, unit);
	}

	/**
	 * Sets the list-style-type CSS property.
	 */
	default void setListStyleType(ListStyleType value) {
		DomStyle_Static.setListStyleType(this, value);
	}

	/**
	 * Set the margin css property.
	 */
	default void setMargin(double value, Unit unit) {
		DomStyle_Static.setMargin(this, value, unit);
	}

	/**
	 * Set the margin-bottom css property.
	 */
	default void setMarginBottom(double value, Unit unit) {
		DomStyle_Static.setMarginBottom(this, value, unit);
	}

	/**
	 * Set the margin-left css property.
	 */
	default void setMarginLeft(double value, Unit unit) {
		DomStyle_Static.setMarginLeft(this, value, unit);
	}

	/**
	 * Set the margin-right css property.
	 */
	default void setMarginRight(double value, Unit unit) {
		DomStyle_Static.setMarginRight(this, value, unit);
	}

	/**
	 * Set the margin-top css property.
	 */
	default void setMarginTop(double value, Unit unit) {
		DomStyle_Static.setMarginTop(this, value, unit);
	}

	/**
	 * Set the opacity css property.
	 */
	default void setOpacity(double value) {
		DomStyle_Static.setOpacity(this, value);
	}

	/**
	 * Set the outline-color css property.
	 */
	default void setOutlineColor(String value) {
		DomStyle_Static.setOutlineColor(this, value);
	}

	/**
	 * Sets the outline-style CSS property.
	 */
	default void setOutlineStyle(OutlineStyle value) {
		DomStyle_Static.setOutlineStyle(this, value);
	}

	/**
	 * Set the outline-width css property.
	 */
	default void setOutlineWidth(double value, Unit unit) {
		DomStyle_Static.setOutlineWidth(this, value, unit);
	}

	/**
	 * Sets the overflow CSS property.
	 */
	default void setOverflow(Overflow value) {
		DomStyle_Static.setOverflow(this, value);
	}

	/**
	 * Sets the overflow-x CSS property.
	 */
	default void setOverflowX(Overflow value) {
		DomStyle_Static.setOverflowX(this, value);
	}

	/**
	 * Sets the overflow-y CSS property.
	 */
	default void setOverflowY(Overflow value) {
		DomStyle_Static.setOverflowY(this, value);
	}

	/**
	 * Set the padding css property.
	 */
	default void setPadding(double value, Unit unit) {
		DomStyle_Static.setPadding(this, value, unit);
	}

	/**
	 * Set the padding-bottom css property.
	 */
	default void setPaddingBottom(double value, Unit unit) {
		DomStyle_Static.setPaddingBottom(this, value, unit);
	}

	/**
	 * Set the padding-left css property.
	 */
	default void setPaddingLeft(double value, Unit unit) {
		DomStyle_Static.setPaddingLeft(this, value, unit);
	}

	/**
	 * Set the padding-right css property.
	 */
	default void setPaddingRight(double value, Unit unit) {
		DomStyle_Static.setPaddingRight(this, value, unit);
	}

	/**
	 * Set the padding-top css property.
	 */
	default void setPaddingTop(double value, Unit unit) {
		DomStyle_Static.setPaddingTop(this, value, unit);
	}

	/**
	 * Sets the position CSS property.
	 */
	default void setPosition(Position value) {
		DomStyle_Static.setPosition(this, value);
	}

	/**
	 * Sets the value of a named property.
	 */
	default void setProperty(String name, String value) {
		DomStyle_Static.setProperty(this, name, value);
	}

	/**
	 * Sets the value of a named property in the specified units.
	 */
	default void setProperty(String name, double value, Unit unit) {
		DomStyle_Static.setProperty(this, name, value, unit);
	}

	/**
	 * Sets the value of a named property, in pixels.
	 * 
	 * This is shorthand for <code>value + "px"</code>.
	 */
	default void setPropertyPx(String name, int value) {
		DomStyle_Static.setPropertyPx(this, name, value);
	}

	/**
	 * Set the right css property.
	 */
	default void setRight(double value, Unit unit) {
		DomStyle_Static.setRight(this, value, unit);
	}

	/**
	 * Set the table-layout CSS property.
	 */
	default void setTableLayout(TableLayout value) {
		DomStyle_Static.setTableLayout(this, value);
	}

	/**
	 * Set the 'text-align' CSS property.
	 */
	default void setTextAlign(TextAlign value) {
		DomStyle_Static.setTextAlign(this, value);
	}

	/**
	 * Sets the text-decoration CSS property.
	 */
	default void setTextDecoration(TextDecoration value) {
		DomStyle_Static.setTextDecoration(this, value);
	}

	/**
	 * Set the 'text-indent' CSS property.
	 */
	default void setTextIndent(double value, Unit unit) {
		DomStyle_Static.setTextIndent(this, value, unit);
	}

	/**
	 * Set the 'text-justify' CSS3 property.
	 */
	default void setTextJustify(TextJustify value) {
		DomStyle_Static.setTextJustify(this, value);
	}

	/**
	 * Set the 'text-overflow' CSS3 property.
	 */
	default void setTextOverflow(TextOverflow value) {
		DomStyle_Static.setTextOverflow(this, value);
	}

	/**
	 * Set the 'text-transform' CSS property.
	 */
	default void setTextTransform(TextTransform value) {
		DomStyle_Static.setTextTransform(this, value);
	}

	/**
	 * Set the top css property.
	 */
	default void setTop(double value, Unit unit) {
		DomStyle_Static.setTop(this, value, unit);
	}

	/**
	 * Sets the vertical-align CSS property.
	 */
	default void setVerticalAlign(VerticalAlign value) {
		DomStyle_Static.setVerticalAlign(this, value);
	}

	/**
	 * Sets the vertical-align CSS property.
	 */
	default void setVerticalAlign(double value, Unit unit) {
		DomStyle_Static.setVerticalAlign(this, value, unit);
	}

	/**
	 * Sets the visibility CSS property.
	 */
	default void setVisibility(Visibility value) {
		DomStyle_Static.setVisibility(this, value);
	}

	/**
	 * Set the 'white-space' CSS property.
	 */
	default void setWhiteSpace(WhiteSpace value) {
		DomStyle_Static.setWhiteSpace(this, value);
	}

	/**
	 * Set the width css property.
	 */
	default void setWidth(double value, Unit unit) {
		DomStyle_Static.setWidth(this, value, unit);
	}

	/**
	 * Set the z-index css property.
	 */
	default void setZIndex(int value) {
		DomStyle_Static.setZIndex(this, value);
	}

	/**
	 * Gets the value of a named property.
	 */
	default String getPropertyImpl(String name) {
		return DOMImpl.impl.getStyleProperty(styleObject(), name);
	}

	void setPropertyImpl(String name, String value);
	
}
