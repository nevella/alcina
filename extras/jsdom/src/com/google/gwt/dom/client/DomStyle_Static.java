package com.google.gwt.dom.client;

import static com.google.gwt.dom.client.DomStyleConstants.*;

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

class DomStyle_Static {
	/**
	 * Clear the background-color css property.
	 */
	static void clearBackgroundColor(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_BACKGROUND_COLOR);
	}

	/**
	 * Clear the background-image css property.
	 */
	static void clearBackgroundImage(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_BACKGROUND_IMAGE);
	}

	/**
	 * Clear the border-color css property.
	 */
	static void clearBorderColor(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_BORDER_COLOR);
	}

	/**
	 * Clears the border-style CSS property.
	 */
	static void clearBorderStyle(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_BORDER_STYLE);
	}

	/**
	 * Clear the border-width css property.
	 */
	static void clearBorderWidth(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_BORDER_WIDTH);
	}

	/**
	 * Clear the bottom css property.
	 */
	static void clearBottom(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_BOTTOM);
	}

	/**
	 * Clear the 'clear' CSS property.
	 */
	static void clearClear(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_CLEAR);
	}

	/**
	 * Clear the color css property.
	 */
	static void clearColor(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_COLOR);
	}

	/**
	 * Clears the cursor CSS property.
	 */
	static void clearCursor(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_CURSOR);
	}

	/**
	 * Clears the display CSS property.
	 */
	static void clearDisplay(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_DISPLAY);
	}

	/**
	 * Clear the float css property.
	 */
	static void clearFloat(DomStyle domStyle) {
		domStyle.clearProperty(DOMImpl.impl.cssFloatPropertyName());
	}

	/**
	 * Clear the font-size css property.
	 */
	static void clearFontSize(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_FONT_SIZE);
	}

	/**
	 * Clears the font-style CSS property.
	 */
	static void clearFontStyle(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_FONT_STYLE);
	}

	/**
	 * Clears the font-weight CSS property.
	 */
	static void clearFontWeight(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_FONT_WEIGHT);
	}

	/**
	 * Clear the height css property.
	 */
	static void clearHeight(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_HEIGHT);
	}

	/**
	 * Clear the left css property.
	 */
	static void clearLeft(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_LEFT);
	}

	/**
	 * Clear the line-height css property.
	 */
	static void clearLineHeight(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_LINE_HEIGHT);
	}

	/**
	 * Clears the list-style-type CSS property.
	 */
	static void clearListStyleType(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_LIST_STYLE_TYPE);
	}

	/**
	 * Clear the margin css property.
	 */
	static void clearMargin(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN);
	}

	/**
	 * Clear the margin-bottom css property.
	 */
	static void clearMarginBottom(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN_BOTTOM);
	}

	/**
	 * Clear the margin-left css property.
	 */
	static void clearMarginLeft(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN_LEFT);
	}

	/**
	 * Clear the margin-right css property.
	 */
	static void clearMarginRight(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN_RIGHT);
	}

	/**
	 * Clear the margin-top css property.
	 */
	static void clearMarginTop(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN_TOP);
	}

	/**
	 * Clear the opacity css property.
	 */
	static void clearOpacity(DomStyle domStyle) {
		DOMImpl.impl.cssClearOpacity(domStyle.styleObject());
	}

	/**
	 * Clear the outline-color css property.
	 */
	static void clearOutlineColor(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_OUTLINE_COLOR);
	}

	/**
	 * Clears the outline-style CSS property.
	 */
	static void clearOutlineStyle(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_OUTLINE_STYLE);
	}

	/**
	 * Clear the outline-width css property.
	 */
	static void clearOutlineWidth(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_OUTLINE_WIDTH);
	}

	/**
	 * Clears the overflow CSS property.
	 */
	static void clearOverflow(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_OVERFLOW);
	}

	/**
	 * Clears the overflow-x CSS property.
	 */
	static void clearOverflowX(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_OVERFLOW_X);
	}

	/**
	 * Clears the overflow-y CSS property.
	 */
	static void clearOverflowY(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_OVERFLOW_Y);
	}

	/**
	 * Clear the padding css property.
	 */
	static void clearPadding(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING);
	}

	/**
	 * Clear the padding-bottom css property.
	 */
	static void clearPaddingBottom(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING_BOTTOM);
	}

	/**
	 * Clear the padding-left css property.
	 */
	static void clearPaddingLeft(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING_LEFT);
	}

	/**
	 * Clear the padding-right css property.
	 */
	static void clearPaddingRight(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING_RIGHT);
	}

	/**
	 * Clear the padding-top css property.
	 */
	static void clearPaddingTop(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING_TOP);
	}

	/**
	 * Clears the position CSS property.
	 */
	static void clearPosition(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_POSITION);
	}

	/**
	 * Clear the right css property.
	 */
	static void clearRight(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_RIGHT);
	}

	/**
	 * Clear the table-layout css property.
	 */
	static void clearTableLayout(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_TABLE_LAYOUT);
	}

	/**
	 * Clear the 'text-align' CSS property.
	 */
	static void clearTextAlign(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_ALIGN);
	}

	/**
	 * Clears the text-decoration CSS property.
	 */
	static void clearTextDecoration(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_DECORATION);
	}

	/**
	 * Clear the 'text-indent' CSS property.
	 */
	static void clearTextIndent(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_INDENT);
	}

	/**
	 * Clear the 'text-justify' CSS3 property.
	 */
	static void clearTextJustify(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_JUSTIFY);
	}

	/**
	 * Clear the 'text-overflow' CSS3 property.
	 */
	static void clearTextOverflow(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_OVERFLOW);
	}

	/**
	 * Clear the 'text-transform' CSS property.
	 */
	static void clearTextTransform(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_TRANSFORM);
	}

	/**
	 * Clear the top css property.
	 */
	static void clearTop(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_TOP);
	}

	/**
	 * Clears the visibility CSS property.
	 */
	static void clearVisibility(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_VISIBILITY);
	}

	/**
	 * Clear the 'white-space' CSS property.
	 */
	static void clearWhiteSpace(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_WHITE_SPACE);
	}

	/**
	 * Clear the width css property.
	 */
	static void clearWidth(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_WIDTH);
	}

	/**
	 * Clear the z-index css property.
	 */
	static void clearZIndex(DomStyle domStyle) {
		domStyle.clearProperty(STYLE_Z_INDEX);
	}

	/**
	 * Get the background-color css property.
	 */
	static String getBackgroundColor(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_BACKGROUND_COLOR);
	}

	/**
	 * Get the background-image css property.
	 */
	static String getBackgroundImage(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_BACKGROUND_IMAGE);
	}

	/**
	 * Get the border-color css property.
	 */
	static String getBorderColor(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_BORDER_COLOR);
	}

	/**
	 * Gets the border-style CSS property.
	 */
	static String getBorderStyle(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_BORDER_STYLE);
	}

	/**
	 * Get the border-width css property.
	 */
	static String getBorderWidth(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_BORDER_WIDTH);
	}

	/**
	 * Get the bottom css property.
	 */
	static String getBottom(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_BOTTOM);
	}

	/**
	 * Get the 'clear' CSS property.
	 */
	static String getClear(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_CLEAR);
	}

	/**
	 * Get the color css property.
	 */
	static String getColor(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_COLOR);
	}

	/**
	 * Gets the cursor CSS property.
	 */
	static String getCursor(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_CURSOR);
	}

	/**
	 * Gets the display CSS property.
	 */
	static String getDisplay(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_DISPLAY);
	}

	/**
	 * Get the font-size css property.
	 */
	static String getFontSize(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_FONT_SIZE);
	}

	/**
	 * Gets the font-style CSS property.
	 */
	static String getFontStyle(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_FONT_STYLE);
	}

	/**
	 * Gets the font-weight CSS property.
	 */
	static String getFontWeight(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_FONT_WEIGHT);
	}

	/**
	 * Get the height css property.
	 */
	static String getHeight(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_HEIGHT);
	}

	/**
	 * Get the left css property.
	 */
	static String getLeft(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_LEFT);
	}

	/**
	 * Get the line-height css property.
	 */
	static String getLineHeight(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_LINE_HEIGHT);
	}

	/**
	 * Gets the list-style-type CSS property.
	 */
	static String getListStyleType(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_LIST_STYLE_TYPE);
	}

	/**
	 * Get the margin css property.
	 */
	static String getMargin(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN);
	}

	/**
	 * Get the margin-bottom css property.
	 */
	static String getMarginBottom(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN_BOTTOM);
	}

	/**
	 * Get the margin-left css property.
	 */
	static String getMarginLeft(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN_LEFT);
	}

	/**
	 * Get the margin-right css property.
	 */
	static String getMarginRight(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN_RIGHT);
	}

	/**
	 * Get the margin-top css property.
	 */
	static String getMarginTop(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN_TOP);
	}

	/**
	 * Get the opacity css property.
	 */
	static String getOpacity(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_OPACITY);
	}

	/**
	 * Gets the overflow CSS property.
	 */
	static String getOverflow(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_OVERFLOW);
	}

	/**
	 * Gets the overflow-x CSS property.
	 */
	static String getOverflowX(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_OVERFLOW_X);
	}

	/**
	 * Gets the overflow-y CSS property.
	 */
	static String getOverflowY(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_OVERFLOW_Y);
	}

	/**
	 * Get the padding css property.
	 */
	static String getPadding(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING);
	}

	/**
	 * Get the padding-bottom css property.
	 */
	static String getPaddingBottom(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING_BOTTOM);
	}

	/**
	 * Get the padding-left css property.
	 */
	static String getPaddingLeft(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING_LEFT);
	}

	/**
	 * Get the padding-right css property.
	 */
	static String getPaddingRight(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING_RIGHT);
	}

	/**
	 * Get the padding-top css property.
	 */
	static String getPaddingTop(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING_TOP);
	}

	/**
	 * Gets the position CSS property.
	 */
	static String getPosition(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_POSITION);
	}

	/**
	 * Get the right css property.
	 */
	static String getRight(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_RIGHT);
	}

	/**
	 * Gets the table-layout property.
	 */
	static String getTableLayout(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_TABLE_LAYOUT);
	}

	/**
	 * Get the 'text-align' CSS property.
	 */
	static String getTextAlign(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_ALIGN);
	}

	/**
	 * Gets the text-decoration CSS property.
	 */
	static String getTextDecoration(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_DECORATION);
	}

	/**
	 * Get the 'text-indent' CSS property.
	 */
	static String getTextIndent(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_INDENT);
	}

	/**
	 * Get the 'text-justify' CSS3 property.
	 */
	static String getTextJustify(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_JUSTIFY);
	}

	/**
	 * Get the 'text-overflow' CSS3 property.
	 */
	static String getTextOverflow(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_OVERFLOW);
	}

	/**
	 * Get the 'text-transform' CSS property.
	 */
	static String getTextTransform(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_TRANSFORM);
	}

	/**
	 * Get the top css property.
	 */
	static String getTop(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_TOP);
	}

	/**
	 * Gets the vertical-align CSS property.
	 */
	static String getVerticalAlign(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_VERTICAL_ALIGN);
	}

	/**
	 * Gets the visibility CSS property.
	 */
	static String getVisibility(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_VISIBILITY);
	}

	/**
	 * Get the 'white-space' CSS property.
	 */
	static String getWhiteSpace(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_WHITE_SPACE);
	}

	/**
	 * Get the width css property.
	 */
	static String getWidth(DomStyle domStyle) {
		return domStyle.getProperty(STYLE_WIDTH);
	}

	/**
	 * Get the z-index css property.
	 */
	static String getZIndex(DomStyle domStyle) {
		return DOMImpl.impl.getNumericStyleProperty(domStyle.styleObject(),
				STYLE_Z_INDEX);
	}

	/**
	 * Set the background-color css property.
	 */
	static void setBackgroundColor(DomStyle domStyle, String value) {
		domStyle.setProperty(STYLE_BACKGROUND_COLOR, value);
	}

	/**
	 * Set the background-image css property.
	 */
	static void setBackgroundImage(DomStyle domStyle, @IsSafeUri String value) {
		domStyle.setProperty(STYLE_BACKGROUND_IMAGE, value);
	}

	/**
	 * Set the border-color css property.
	 */
	static void setBorderColor(DomStyle domStyle, String value) {
		domStyle.setProperty(STYLE_BORDER_COLOR, value);
	}

	/**
	 * Sets the border-style CSS property.
	 */
	static void setBorderStyle(DomStyle domStyle, BorderStyle value) {
		domStyle.setProperty(STYLE_BORDER_STYLE, value.getCssName());
	}

	/**
	 * Set the border-width css property.
	 */
	static void setBorderWidth(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_BORDER_WIDTH, value, unit);
	}

	/**
	 * Set the bottom css property.
	 */
	static void setBottom(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_BOTTOM, value, unit);
	}

	/**
	 * Sets the 'clear' CSS property.
	 */
	static void setClear(DomStyle domStyle, Clear value) {
		domStyle.setProperty(STYLE_CLEAR, value.getCssName());
	}

	/**
	 * Sets the color CSS property.
	 */
	static void setColor(DomStyle domStyle, String value) {
		domStyle.setProperty(STYLE_COLOR, value);
	}

	/**
	 * Sets the cursor CSS property.
	 */
	static void setCursor(DomStyle domStyle, Cursor value) {
		domStyle.setProperty(STYLE_CURSOR, value.getCssName());
	}

	/**
	 * Sets the display CSS property.
	 */
	static void setDisplay(DomStyle domStyle, Display value) {
		domStyle.setProperty(STYLE_DISPLAY, value.getCssName());
	}

	/**
	 * Set the float css property.
	 */
	static void setFloat(DomStyle domStyle, Float value) {
		domStyle.setProperty(DOMImpl.impl.cssFloatPropertyName(),
				value.getCssName());
	}

	/**
	 * Set the font-size css property.
	 */
	static void setFontSize(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_FONT_SIZE, value, unit);
	}

	/**
	 * Sets the font-style CSS property.
	 */
	static void setFontStyle(DomStyle domStyle, FontStyle value) {
		domStyle.setProperty(STYLE_FONT_STYLE, value.getCssName());
	}

	/**
	 * Sets the font-weight CSS property.
	 */
	static void setFontWeight(DomStyle domStyle, FontWeight value) {
		domStyle.setProperty(STYLE_FONT_WEIGHT, value.getCssName());
	}

	/**
	 * Set the height css property.
	 */
	static void setHeight(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_HEIGHT, value, unit);
	}

	/**
	 * Set the left css property.
	 */
	static void setLeft(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_LEFT, value, unit);
	}

	/**
	 * Set the line-height css property.
	 */
	static void setLineHeight(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_LINE_HEIGHT, value, unit);
	}

	/**
	 * Sets the list-style-type CSS property.
	 */
	static void setListStyleType(DomStyle domStyle, ListStyleType value) {
		domStyle.setProperty(STYLE_LIST_STYLE_TYPE, value.getCssName());
	}

	/**
	 * Set the margin css property.
	 */
	static void setMargin(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_MARGIN, value, unit);
	}

	/**
	 * Set the margin-bottom css property.
	 */
	static void setMarginBottom(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_MARGIN_BOTTOM, value, unit);
	}

	/**
	 * Set the margin-left css property.
	 */
	static void setMarginLeft(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_MARGIN_LEFT, value, unit);
	}

	/**
	 * Set the margin-right css property.
	 */
	static void setMarginRight(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_MARGIN_RIGHT, value, unit);
	}

	/**
	 * Set the margin-top css property.
	 */
	static void setMarginTop(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_MARGIN_TOP, value, unit);
	}

	/**
	 * Set the opacity css property.
	 */
	static void setOpacity(DomStyle domStyle, double value) {
		DOMImpl.impl.cssSetOpacity(domStyle.styleObject(), value);
	}

	/**
	 * Set the outline-color css property.
	 */
	static void setOutlineColor(DomStyle domStyle, String value) {
		domStyle.setProperty(STYLE_OUTLINE_COLOR, value);
	}

	/**
	 * Sets the outline-style CSS property.
	 */
	static void setOutlineStyle(DomStyle domStyle, OutlineStyle value) {
		domStyle.setProperty(STYLE_OUTLINE_STYLE, value.getCssName());
	}

	/**
	 * Set the outline-width css property.
	 */
	static void setOutlineWidth(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_OUTLINE_WIDTH, value, unit);
	}

	/**
	 * Sets the overflow CSS property.
	 */
	static void setOverflow(DomStyle domStyle, Overflow value) {
		domStyle.setProperty(STYLE_OVERFLOW, value.getCssName());
	}

	/**
	 * Sets the overflow-x CSS property.
	 */
	static void setOverflowX(DomStyle domStyle, Overflow value) {
		domStyle.setProperty(STYLE_OVERFLOW_X, value.getCssName());
	}

	/**
	 * Sets the overflow-y CSS property.
	 */
	static void setOverflowY(DomStyle domStyle, Overflow value) {
		domStyle.setProperty(STYLE_OVERFLOW_Y, value.getCssName());
	}

	/**
	 * Set the padding css property.
	 */
	static void setPadding(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_PADDING, value, unit);
	}

	/**
	 * Set the padding-bottom css property.
	 */
	static void setPaddingBottom(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_PADDING_BOTTOM, value, unit);
	}

	/**
	 * Set the padding-left css property.
	 */
	static void setPaddingLeft(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_PADDING_LEFT, value, unit);
	}

	/**
	 * Set the padding-right css property.
	 */
	static void setPaddingRight(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_PADDING_RIGHT, value, unit);
	}

	/**
	 * Set the padding-top css property.
	 */
	static void setPaddingTop(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_PADDING_TOP, value, unit);
	}

	/**
	 * Sets the position CSS property.
	 */
	static void setPosition(DomStyle domStyle, Position value) {
		domStyle.setProperty(STYLE_POSITION, value.getCssName());
	}

	/**
	 * Sets the value of a named property.
	 */
	static void setProperty(DomStyle domStyle, String name, String value) {
		assertCamelCase(name);
		domStyle.setPropertyImpl(name, value);
	}

	/**
	 * Sets the value of a named property in the specified units.
	 */
	static void setProperty(DomStyle domStyle, String name, double value,
			Unit unit) {
		assertCamelCase(name);
		domStyle.setPropertyImpl(name, value + unit.getType());
	}

	/**
	 * Assert that the specified property does not contain a hyphen.
	 * 
	 * @param name
	 *            the property name
	 */
	static void assertCamelCase(String name) {
		assert !name.contains("-") : "The style name '" + name
				+ "' should be in camelCase format";
	}

	/**
	 * Sets the value of a named property, in pixels.
	 * 
	 * This is shorthand for <code>value + "px"</code>.
	 */
	static void setPropertyPx(DomStyle domStyle, String name, int value) {
		domStyle.setProperty(name, value, Unit.PX);
	}

	/**
	 * Set the right css property.
	 */
	static void setRight(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_RIGHT, value, unit);
	}

	/**
	 * Set the table-layout CSS property.
	 */
	static void setTableLayout(DomStyle domStyle, TableLayout value) {
		domStyle.setProperty(STYLE_TABLE_LAYOUT, value.getCssName());
	}

	/**
	 * Set the 'text-align' CSS property.
	 */
	static void setTextAlign(DomStyle domStyle, TextAlign value) {
		domStyle.setProperty(STYLE_TEXT_ALIGN, value.getCssName());
	}

	/**
	 * Sets the text-decoration CSS property.
	 */
	static void setTextDecoration(DomStyle domStyle, TextDecoration value) {
		domStyle.setProperty(STYLE_TEXT_DECORATION, value.getCssName());
	}

	/**
	 * Set the 'text-indent' CSS property.
	 */
	static void setTextIndent(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_TEXT_INDENT, value, unit);
	}

	/**
	 * Set the 'text-justify' CSS3 property.
	 */
	static void setTextJustify(DomStyle domStyle, TextJustify value) {
		domStyle.setProperty(STYLE_TEXT_JUSTIFY, value.getCssName());
	}

	/**
	 * Set the 'text-overflow' CSS3 property.
	 */
	static void setTextOverflow(DomStyle domStyle, TextOverflow value) {
		domStyle.setProperty(STYLE_TEXT_OVERFLOW, value.getCssName());
	}

	/**
	 * Set the 'text-transform' CSS property.
	 */
	static void setTextTransform(DomStyle domStyle, TextTransform value) {
		domStyle.setProperty(STYLE_TEXT_TRANSFORM, value.getCssName());
	}

	/**
	 * Set the top css property.
	 */
	static void setTop(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_TOP, value, unit);
	}

	/**
	 * Sets the vertical-align CSS property.
	 */
	static void setVerticalAlign(DomStyle domStyle, VerticalAlign value) {
		domStyle.setProperty(STYLE_VERTICAL_ALIGN, value.getCssName());
	}

	/**
	 * Sets the vertical-align CSS property.
	 */
	static void setVerticalAlign(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_VERTICAL_ALIGN, value, unit);
	}

	/**
	 * Sets the visibility CSS property.
	 */
	static void setVisibility(DomStyle domStyle, Visibility value) {
		domStyle.setProperty(STYLE_VISIBILITY, value.getCssName());
	}

	/**
	 * Set the 'white-space' CSS property.
	 */
	static void setWhiteSpace(DomStyle domStyle, WhiteSpace value) {
		domStyle.setProperty(STYLE_WHITE_SPACE, value.getCssName());
	}

	/**
	 * Set the width css property.
	 */
	static void setWidth(DomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_WIDTH, value, unit);
	}

	/**
	 * Set the z-index css property.
	 */
	static void setZIndex(DomStyle domStyle, int value) {
		domStyle.setProperty(STYLE_Z_INDEX, value + "");
	}

	/**
	 * Assert that the specified property does not contain a hyphen.
	 * 
	 * @param name
	 *            the property name
	 */
	static void assertCamelCase(DomStyle domStyle, String name) {
		assert !name.contains("-") : "The style name '" + name
				+ "' should be in camelCase format";
	}

	static String getProperty(DomStyle domStyle, String name) {
		assertCamelCase(name);
		return domStyle.getPropertyImpl(name);
	}

	static String getPropertyImpl(Style_Jso style_Dom, String name) {
		// FIXME - more direct call maybe
		return DOMImpl.impl.getStyleProperty(
				LocalDomBridge.styleObjectFor(style_Dom), name);
	}

	public static void clearProperty(DomStyle domStyle, String name) {
		domStyle.setProperty(name, "");
	}

	
}
