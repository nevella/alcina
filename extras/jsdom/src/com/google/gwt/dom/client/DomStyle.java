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
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

public interface DomStyle {
	/**
	 * Clear the background-color css property.
	 */
	void clearBackgroundColor();

	/**
	 * Clear the background-image css property.
	 */
	void clearBackgroundImage();

	/**
	 * Clear the border-color css property.
	 */
	void clearBorderColor();

	/**
	 * Clears the border-style CSS property.
	 */
	void clearBorderStyle();

	/**
	 * Clear the border-width css property.
	 */
	void clearBorderWidth();

	/**
	 * Clear the bottom css property.
	 */
	void clearBottom();

	/**
	 * Clear the 'clear' CSS property.
	 */
	void clearClear();

	/**
	 * Clear the color css property.
	 */
	void clearColor();

	/**
	 * Clears the cursor CSS property.
	 */
	void clearCursor();

	/**
	 * Clears the display CSS property.
	 */
	void clearDisplay();

	/**
	 * Clear the float css property.
	 */
	void clearFloat();

	/**
	 * Clear the font-size css property.
	 */
	void clearFontSize();

	/**
	 * Clears the font-style CSS property.
	 */
	void clearFontStyle();

	/**
	 * Clears the font-weight CSS property.
	 */
	void clearFontWeight();

	/**
	 * Clear the height css property.
	 */
	void clearHeight();

	/**
	 * Clear the left css property.
	 */
	void clearLeft();

	/**
	 * Clear the line-height css property.
	 */
	void clearLineHeight();

	/**
	 * Clears the list-style-type CSS property.
	 */
	void clearListStyleType();

	/**
	 * Clear the margin css property.
	 */
	void clearMargin();

	/**
	 * Clear the margin-bottom css property.
	 */
	void clearMarginBottom();

	/**
	 * Clear the margin-left css property.
	 */
	void clearMarginLeft();

	/**
	 * Clear the margin-right css property.
	 */
	void clearMarginRight();

	/**
	 * Clear the margin-top css property.
	 */
	void clearMarginTop();

	/**
	 * Clear the opacity css property.
	 */
	void clearOpacity();

	Style styleObject();

	/**
	 * Clear the outline-color css property.
	 */
	void clearOutlineColor();

	/**
	 * Clears the outline-style CSS property.
	 */
	void clearOutlineStyle();

	/**
	 * Clear the outline-width css property.
	 */
	void clearOutlineWidth();

	/**
	 * Clears the overflow CSS property.
	 */
	void clearOverflow();

	/**
	 * Clears the overflow-x CSS property.
	 */
	void clearOverflowX();

	/**
	 * Clears the overflow-y CSS property.
	 */
	void clearOverflowY();

	/**
	 * Clear the padding css property.
	 */
	void clearPadding();

	/**
	 * Clear the padding-bottom css property.
	 */
	void clearPaddingBottom();

	/**
	 * Clear the padding-left css property.
	 */
	void clearPaddingLeft();

	/**
	 * Clear the padding-right css property.
	 */
	void clearPaddingRight();

	/**
	 * Clear the padding-top css property.
	 */
	void clearPaddingTop();

	/**
	 * Clears the position CSS property.
	 */
	void clearPosition();

	/**
	 * Clears the value of a named property, causing it to revert to its
	 * default.
	 */
	void clearProperty(String name);

	/**
	 * Clear the right css property.
	 */
	void clearRight();

	/**
	 * Clear the table-layout css property.
	 */
	void clearTableLayout();

	/**
	 * Clear the 'text-align' CSS property.
	 */
	void clearTextAlign();

	/**
	 * Clears the text-decoration CSS property.
	 */
	void clearTextDecoration();

	/**
	 * Clear the 'text-indent' CSS property.
	 */
	void clearTextIndent();

	/**
	 * Clear the 'text-justify' CSS3 property.
	 */
	void clearTextJustify();

	/**
	 * Clear the 'text-overflow' CSS3 property.
	 */
	void clearTextOverflow();

	/**
	 * Clear the 'text-transform' CSS property.
	 */
	void clearTextTransform();

	/**
	 * Clear the top css property.
	 */
	void clearTop();

	/**
	 * Clears the visibility CSS property.
	 */
	void clearVisibility();

	/**
	 * Clear the 'white-space' CSS property.
	 */
	void clearWhiteSpace();

	/**
	 * Clear the width css property.
	 */
	void clearWidth();

	/**
	 * Clear the z-index css property.
	 */
	void clearZIndex();

	/**
	 * Get the background-color css property.
	 */
	String getBackgroundColor();

	/**
	 * Get the background-image css property.
	 */
	String getBackgroundImage();

	/**
	 * Get the border-color css property.
	 */
	String getBorderColor();

	/**
	 * Gets the border-style CSS property.
	 */
	String getBorderStyle();

	/**
	 * Get the border-width css property.
	 */
	String getBorderWidth();

	/**
	 * Get the bottom css property.
	 */
	String getBottom();

	/**
	 * Get the 'clear' CSS property.
	 */
	String getClear();

	/**
	 * Get the color css property.
	 */
	String getColor();

	/**
	 * Gets the cursor CSS property.
	 */
	String getCursor();

	/**
	 * Gets the display CSS property.
	 */
	String getDisplay();

	/**
	 * Get the font-size css property.
	 */
	String getFontSize();

	/**
	 * Gets the font-style CSS property.
	 */
	String getFontStyle();

	/**
	 * Gets the font-weight CSS property.
	 */
	String getFontWeight();

	/**
	 * Get the height css property.
	 */
	String getHeight();

	/**
	 * Get the left css property.
	 */
	String getLeft();

	/**
	 * Get the line-height css property.
	 */
	String getLineHeight();

	/**
	 * Gets the list-style-type CSS property.
	 */
	String getListStyleType();

	/**
	 * Get the margin css property.
	 */
	String getMargin();

	/**
	 * Get the margin-bottom css property.
	 */
	String getMarginBottom();

	/**
	 * Get the margin-left css property.
	 */
	String getMarginLeft();

	/**
	 * Get the margin-right css property.
	 */
	String getMarginRight();

	/**
	 * Get the margin-top css property.
	 */
	String getMarginTop();

	/**
	 * Get the opacity css property.
	 */
	String getOpacity();

	/**
	 * Gets the overflow CSS property.
	 */
	String getOverflow();

	/**
	 * Gets the overflow-x CSS property.
	 */
	String getOverflowX();

	/**
	 * Gets the overflow-y CSS property.
	 */
	String getOverflowY();

	/**
	 * Get the padding css property.
	 */
	String getPadding();

	/**
	 * Get the padding-bottom css property.
	 */
	String getPaddingBottom();

	/**
	 * Get the padding-left css property.
	 */
	String getPaddingLeft();

	/**
	 * Get the padding-right css property.
	 */
	String getPaddingRight();

	/**
	 * Get the padding-top css property.
	 */
	String getPaddingTop();

	/**
	 * Gets the position CSS property.
	 */
	String getPosition();

	/**
	 * Gets the value of a named property.
	 */
	String getProperty(String name);

	/**
	 * Get the right css property.
	 */
	String getRight();

	/**
	 * Gets the table-layout property.
	 */
	String getTableLayout();

	/**
	 * Get the 'text-align' CSS property.
	 */
	String getTextAlign();

	/**
	 * Gets the text-decoration CSS property.
	 */
	String getTextDecoration();

	/**
	 * Get the 'text-indent' CSS property.
	 */
	String getTextIndent();

	/**
	 * Get the 'text-justify' CSS3 property.
	 */
	String getTextJustify();

	/**
	 * Get the 'text-overflow' CSS3 property.
	 */
	String getTextOverflow();

	/**
	 * Get the 'text-transform' CSS property.
	 */
	String getTextTransform();

	/**
	 * Get the top css property.
	 */
	String getTop();

	/**
	 * Gets the vertical-align CSS property.
	 */
	String getVerticalAlign();

	/**
	 * Gets the visibility CSS property.
	 */
	String getVisibility();

	/**
	 * Get the 'white-space' CSS property.
	 */
	String getWhiteSpace();

	/**
	 * Get the width css property.
	 */
	String getWidth();

	/**
	 * Get the z-index css property.
	 */
	String getZIndex();

	/**
	 * Set the background-color css property.
	 */
	void setBackgroundColor(String value);

	/**
	 * Set the background-image css property.
	 */
	void setBackgroundImage(@IsSafeUri String value);

	/**
	 * Set the border-color css property.
	 */
	void setBorderColor(String value);

	/**
	 * Sets the border-style CSS property.
	 */
	void setBorderStyle(BorderStyle value);

	/**
	 * Set the border-width css property.
	 */
	void setBorderWidth(double value, Unit unit);

	/**
	 * Set the bottom css property.
	 */
	void setBottom(double value, Unit unit);

	/**
	 * Sets the 'clear' CSS property.
	 */
	void setClear(Clear value);

	/**
	 * Sets the color CSS property.
	 */
	void setColor(String value);

	/**
	 * Sets the cursor CSS property.
	 */
	void setCursor(Cursor value);

	/**
	 * Sets the display CSS property.
	 */
	void setDisplay(Display value);

	/**
	 * Set the float css property.
	 */
	void setFloat(Float value);

	/**
	 * Set the font-size css property.
	 */
	void setFontSize(double value, Unit unit);

	/**
	 * Sets the font-style CSS property.
	 */
	void setFontStyle(FontStyle value);

	/**
	 * Sets the font-weight CSS property.
	 */
	void setFontWeight(FontWeight value);

	/**
	 * Set the height css property.
	 */
	void setHeight(double value, Unit unit);

	/**
	 * Set the left css property.
	 */
	void setLeft(double value, Unit unit);

	/**
	 * Set the line-height css property.
	 */
	void setLineHeight(double value, Unit unit);

	/**
	 * Sets the list-style-type CSS property.
	 */
	void setListStyleType(ListStyleType value);

	/**
	 * Set the margin css property.
	 */
	void setMargin(double value, Unit unit);

	/**
	 * Set the margin-bottom css property.
	 */
	void setMarginBottom(double value, Unit unit);

	/**
	 * Set the margin-left css property.
	 */
	void setMarginLeft(double value, Unit unit);

	/**
	 * Set the margin-right css property.
	 */
	void setMarginRight(double value, Unit unit);

	/**
	 * Set the margin-top css property.
	 */
	void setMarginTop(double value, Unit unit);

	/**
	 * Set the opacity css property.
	 */
	void setOpacity(double value);

	/**
	 * Set the outline-color css property.
	 */
	void setOutlineColor(String value);

	/**
	 * Sets the outline-style CSS property.
	 */
	void setOutlineStyle(OutlineStyle value);

	/**
	 * Set the outline-width css property.
	 */
	void setOutlineWidth(double value, Unit unit);

	/**
	 * Sets the overflow CSS property.
	 */
	void setOverflow(Overflow value);

	/**
	 * Sets the overflow-x CSS property.
	 */
	void setOverflowX(Overflow value);

	/**
	 * Sets the overflow-y CSS property.
	 */
	void setOverflowY(Overflow value);

	/**
	 * Set the padding css property.
	 */
	void setPadding(double value, Unit unit);

	/**
	 * Set the padding-bottom css property.
	 */
	void setPaddingBottom(double value, Unit unit);

	/**
	 * Set the padding-left css property.
	 */
	void setPaddingLeft(double value, Unit unit);

	/**
	 * Set the padding-right css property.
	 */
	void setPaddingRight(double value, Unit unit);

	/**
	 * Set the padding-top css property.
	 */
	void setPaddingTop(double value, Unit unit);

	/**
	 * Sets the position CSS property.
	 */
	void setPosition(Position value);

	/**
	 * Sets the value of a named property.
	 */
	void setProperty(String name, String value);

	/**
	 * Sets the value of a named property in the specified units.
	 */
	void setProperty(String name, double value, Unit unit);

	/**
	 * Sets the value of a named property, in pixels.
	 * 
	 * This is shorthand for <code>value + "px"</code>.
	 */
	void setPropertyPx(String name, int value);

	/**
	 * Set the right css property.
	 */
	void setRight(double value, Unit unit);

	/**
	 * Set the table-layout CSS property.
	 */
	void setTableLayout(TableLayout value);

	/**
	 * Set the 'text-align' CSS property.
	 */
	void setTextAlign(TextAlign value);

	/**
	 * Sets the text-decoration CSS property.
	 */
	void setTextDecoration(TextDecoration value);

	/**
	 * Set the 'text-indent' CSS property.
	 */
	void setTextIndent(double value, Unit unit);

	/**
	 * Set the 'text-justify' CSS3 property.
	 */
	void setTextJustify(TextJustify value);

	/**
	 * Set the 'text-overflow' CSS3 property.
	 */
	void setTextOverflow(TextOverflow value);

	/**
	 * Set the 'text-transform' CSS property.
	 */
	void setTextTransform(TextTransform value);

	/**
	 * Set the top css property.
	 */
	void setTop(double value, Unit unit);

	/**
	 * Sets the vertical-align CSS property.
	 */
	void setVerticalAlign(VerticalAlign value);

	/**
	 * Sets the vertical-align CSS property.
	 */
	void setVerticalAlign(double value, Unit unit);

	/**
	 * Sets the visibility CSS property.
	 */
	void setVisibility(Visibility value);

	/**
	 * Set the 'white-space' CSS property.
	 */
	void setWhiteSpace(WhiteSpace value);

	/**
	 * Set the width css property.
	 */
	void setWidth(double value, Unit unit);

	/**
	 * Set the z-index css property.
	 */
	void setZIndex(int value);

	/**
	 * Gets the value of a named property.
	 */
	String getPropertyImpl(String name);

	void setPropertyImpl(String name, String value);

	Map<String, String> getProperties();


	

}
