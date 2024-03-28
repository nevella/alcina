package com.google.gwt.dom.client;

import static com.google.gwt.dom.client.DomStyleConstants.*;

import java.util.Arrays;

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

import cc.alcina.framework.common.client.util.Ax;

class ClientDomStyleStatic {
	/**
	 * Assert that the specified property does not contain a hyphen.
	 *
	 * @param name
	 *            the property name
	 */
	static void assertCamelCase(ClientDomStyle domStyle, String name) {
		assert !name.contains("-") : "The style name '" + name
				+ "' should be in camelCase format";
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
	 * Clear the background-color css property.
	 */
	static void clearBackgroundColor(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_BACKGROUND_COLOR);
	}

	/**
	 * Clear the background-image css property.
	 */
	static void clearBackgroundImage(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_BACKGROUND_IMAGE);
	}

	/**
	 * Clear the border-color css property.
	 */
	static void clearBorderColor(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_BORDER_COLOR);
	}

	/**
	 * Clears the border-style CSS property.
	 */
	static void clearBorderStyle(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_BORDER_STYLE);
	}

	/**
	 * Clear the border-width css property.
	 */
	static void clearBorderWidth(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_BORDER_WIDTH);
	}

	/**
	 * Clear the bottom css property.
	 */
	static void clearBottom(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_BOTTOM);
	}

	/**
	 * Clear the 'clear' CSS property.
	 */
	static void clearClear(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_CLEAR);
	}

	/**
	 * Clear the color css property.
	 */
	static void clearColor(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_COLOR);
	}

	/**
	 * Clears the cursor CSS property.
	 */
	static void clearCursor(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_CURSOR);
	}

	/**
	 * Clears the display CSS property.
	 */
	static void clearDisplay(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_DISPLAY);
	}

	/**
	 * Clear the float css property.
	 */
	static void clearFloat(ClientDomStyle domStyle) {
		domStyle.clearProperty(DOMImpl.impl.cssFloatPropertyName());
	}

	/**
	 * Clear the font-size css property.
	 */
	static void clearFontSize(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_FONT_SIZE);
	}

	/**
	 * Clears the font-style CSS property.
	 */
	static void clearFontStyle(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_FONT_STYLE);
	}

	/**
	 * Clears the font-weight CSS property.
	 */
	static void clearFontWeight(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_FONT_WEIGHT);
	}

	/**
	 * Clear the height css property.
	 */
	static void clearHeight(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_HEIGHT);
	}

	/**
	 * Clear the left css property.
	 */
	static void clearLeft(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_LEFT);
	}

	/**
	 * Clear the line-height css property.
	 */
	static void clearLineHeight(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_LINE_HEIGHT);
	}

	/**
	 * Clears the list-style-type CSS property.
	 */
	static void clearListStyleType(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_LIST_STYLE_TYPE);
	}

	/**
	 * Clear the margin css property.
	 */
	static void clearMargin(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN);
	}

	/**
	 * Clear the margin-bottom css property.
	 */
	static void clearMarginBottom(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN_BOTTOM);
	}

	/**
	 * Clear the margin-left css property.
	 */
	static void clearMarginLeft(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN_LEFT);
	}

	/**
	 * Clear the margin-right css property.
	 */
	static void clearMarginRight(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN_RIGHT);
	}

	/**
	 * Clear the margin-top css property.
	 */
	static void clearMarginTop(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_MARGIN_TOP);
	}

	/**
	 * Clear the opacity css property.
	 */
	static void clearOpacity(ClientDomStyle domStyle) {
		DOMImpl.impl.cssClearOpacity(domStyle.styleObject());
	}

	/**
	 * Clear the outline-color css property.
	 */
	static void clearOutlineColor(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_OUTLINE_COLOR);
	}

	/**
	 * Clears the outline-style CSS property.
	 */
	static void clearOutlineStyle(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_OUTLINE_STYLE);
	}

	/**
	 * Clear the outline-width css property.
	 */
	static void clearOutlineWidth(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_OUTLINE_WIDTH);
	}

	/**
	 * Clears the overflow CSS property.
	 */
	static void clearOverflow(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_OVERFLOW);
	}

	/**
	 * Clears the overflow-x CSS property.
	 */
	static void clearOverflowX(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_OVERFLOW_X);
	}

	/**
	 * Clears the overflow-y CSS property.
	 */
	static void clearOverflowY(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_OVERFLOW_Y);
	}

	/**
	 * Clear the padding css property.
	 */
	static void clearPadding(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING);
	}

	/**
	 * Clear the padding-bottom css property.
	 */
	static void clearPaddingBottom(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING_BOTTOM);
	}

	/**
	 * Clear the padding-left css property.
	 */
	static void clearPaddingLeft(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING_LEFT);
	}

	/**
	 * Clear the padding-right css property.
	 */
	static void clearPaddingRight(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING_RIGHT);
	}

	/**
	 * Clear the padding-top css property.
	 */
	static void clearPaddingTop(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_PADDING_TOP);
	}

	/**
	 * Clears the position CSS property.
	 */
	static void clearPosition(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_POSITION);
	}

	public static void clearProperty(ClientDomStyle domStyle, String name) {
		domStyle.setProperty(name, "");
	}

	/**
	 * Clear the right css property.
	 */
	static void clearRight(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_RIGHT);
	}

	/**
	 * Clear the table-layout css property.
	 */
	static void clearTableLayout(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_TABLE_LAYOUT);
	}

	/**
	 * Clear the 'text-align' CSS property.
	 */
	static void clearTextAlign(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_ALIGN);
	}

	/**
	 * Clears the text-decoration CSS property.
	 */
	static void clearTextDecoration(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_DECORATION);
	}

	/**
	 * Clear the 'text-indent' CSS property.
	 */
	static void clearTextIndent(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_INDENT);
	}

	/**
	 * Clear the 'text-justify' CSS3 property.
	 */
	static void clearTextJustify(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_JUSTIFY);
	}

	/**
	 * Clear the 'text-overflow' CSS3 property.
	 */
	static void clearTextOverflow(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_OVERFLOW);
	}

	/**
	 * Clear the 'text-transform' CSS property.
	 */
	static void clearTextTransform(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_TEXT_TRANSFORM);
	}

	/**
	 * Clear the top css property.
	 */
	static void clearTop(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_TOP);
	}

	/**
	 * Clears the visibility CSS property.
	 */
	static void clearVisibility(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_VISIBILITY);
	}

	/**
	 * Clear the 'white-space' CSS property.
	 */
	static void clearWhiteSpace(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_WHITE_SPACE);
	}

	/**
	 * Clear the width css property.
	 */
	static void clearWidth(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_WIDTH);
	}

	/**
	 * Clear the z-index css property.
	 */
	static void clearZIndex(ClientDomStyle domStyle) {
		domStyle.clearProperty(STYLE_Z_INDEX);
	}

	static <E extends Enum> E enumeratedValue(Class<E> enumClass,
			String value) {
		if (Ax.isBlank(value)) {
			return null;
		} else {
			String test = value.toUpperCase();
			return Arrays.stream(enumClass.getEnumConstants())
					.filter(e -> e.toString().equals(test)).findFirst().get();
		}
	}

	/**
	 * Get the background-color css property.
	 */
	static String getBackgroundColor(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_BACKGROUND_COLOR);
	}

	/**
	 * Get the background-image css property.
	 */
	static String getBackgroundImage(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_BACKGROUND_IMAGE);
	}

	/**
	 * Get the border-color css property.
	 */
	static String getBorderColor(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_BORDER_COLOR);
	}

	/**
	 * Gets the border-style CSS property.
	 */
	static String getBorderStyle(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_BORDER_STYLE);
	}

	/**
	 * Get the border-width css property.
	 */
	static String getBorderWidth(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_BORDER_WIDTH);
	}

	/**
	 * Get the bottom css property.
	 */
	static String getBottom(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_BOTTOM);
	}

	/**
	 * Get the 'clear' CSS property.
	 */
	static String getClear(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_CLEAR);
	}

	/**
	 * Get the color css property.
	 */
	static String getColor(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_COLOR);
	}

	/**
	 * Gets the cursor CSS property.
	 */
	static String getCursor(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_CURSOR);
	}

	/**
	 * Gets the display CSS property.
	 */
	static String getDisplay(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_DISPLAY);
	}

	static Display getDisplayTyped(ClientDomStyle domStyle) {
		return enumeratedValue(Display.class, getDisplay(domStyle));
	}

	/**
	 * Get the font-size css property.
	 */
	static String getFontSize(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_FONT_SIZE);
	}

	/**
	 * Gets the font-style CSS property.
	 */
	static String getFontStyle(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_FONT_STYLE);
	}

	/**
	 * Gets the font-weight CSS property.
	 */
	static String getFontWeight(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_FONT_WEIGHT);
	}

	/**
	 * Get the height css property.
	 */
	static String getHeight(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_HEIGHT);
	}

	/**
	 * Get the left css property.
	 */
	static String getLeft(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_LEFT);
	}

	/**
	 * Get the line-height css property.
	 */
	static String getLineHeight(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_LINE_HEIGHT);
	}

	/**
	 * Gets the list-style-type CSS property.
	 */
	static String getListStyleType(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_LIST_STYLE_TYPE);
	}

	/**
	 * Get the margin css property.
	 */
	static String getMargin(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN);
	}

	/**
	 * Get the margin-bottom css property.
	 */
	static String getMarginBottom(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN_BOTTOM);
	}

	/**
	 * Get the margin-left css property.
	 */
	static String getMarginLeft(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN_LEFT);
	}

	/**
	 * Get the margin-right css property.
	 */
	static String getMarginRight(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN_RIGHT);
	}

	/**
	 * Get the margin-top css property.
	 */
	static String getMarginTop(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_MARGIN_TOP);
	}

	/**
	 * Get the opacity css property.
	 */
	static String getOpacity(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_OPACITY);
	}

	/**
	 * Gets the overflow CSS property.
	 */
	static String getOverflow(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_OVERFLOW);
	}

	/**
	 * Gets the overflow-x CSS property.
	 */
	static String getOverflowX(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_OVERFLOW_X);
	}

	/**
	 * Gets the overflow-y CSS property.
	 */
	static String getOverflowY(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_OVERFLOW_Y);
	}

	/**
	 * Get the padding css property.
	 */
	static String getPadding(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING);
	}

	/**
	 * Get the padding-bottom css property.
	 */
	static String getPaddingBottom(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING_BOTTOM);
	}

	/**
	 * Get the padding-left css property.
	 */
	static String getPaddingLeft(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING_LEFT);
	}

	/**
	 * Get the padding-right css property.
	 */
	static String getPaddingRight(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING_RIGHT);
	}

	/**
	 * Get the padding-top css property.
	 */
	static String getPaddingTop(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_PADDING_TOP);
	}

	/**
	 * Gets the position CSS property.
	 */
	static String getPosition(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_POSITION);
	}

	static Position getPositionTyped(ClientDomStyle domStyle) {
		return enumeratedValue(Position.class, getDisplay(domStyle));
	}

	static String getProperty(ClientDomStyle domStyle, String name) {
		assertCamelCase(name);
		return domStyle.getPropertyImpl(name);
	}

	/**
	 * Get the right css property.
	 */
	static String getRight(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_RIGHT);
	}

	/**
	 * Gets the table-layout property.
	 */
	static String getTableLayout(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_TABLE_LAYOUT);
	}

	/**
	 * Get the 'text-align' CSS property.
	 */
	static String getTextAlign(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_ALIGN);
	}

	/**
	 * Gets the text-decoration CSS property.
	 */
	static String getTextDecoration(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_DECORATION);
	}

	/**
	 * Get the 'text-indent' CSS property.
	 */
	static String getTextIndent(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_INDENT);
	}

	/**
	 * Get the 'text-justify' CSS3 property.
	 */
	static String getTextJustify(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_JUSTIFY);
	}

	/**
	 * Get the 'text-overflow' CSS3 property.
	 */
	static String getTextOverflow(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_OVERFLOW);
	}

	/**
	 * Get the 'text-transform' CSS property.
	 */
	static String getTextTransform(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_TEXT_TRANSFORM);
	}

	/**
	 * Get the top css property.
	 */
	static String getTop(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_TOP);
	}

	/**
	 * Gets the vertical-align CSS property.
	 */
	static String getVerticalAlign(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_VERTICAL_ALIGN);
	}

	/**
	 * Gets the visibility CSS property.
	 */
	static String getVisibility(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_VISIBILITY);
	}

	/**
	 * Get the 'white-space' CSS property.
	 */
	static String getWhiteSpace(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_WHITE_SPACE);
	}

	/**
	 * Get the width css property.
	 */
	static String getWidth(ClientDomStyle domStyle) {
		return domStyle.getProperty(STYLE_WIDTH);
	}

	/**
	 * Get the z-index css property.
	 */
	static String getZIndex(ClientDomStyle domStyle) {
		return DOMImpl.impl.getNumericStyleProperty(domStyle.styleObject(),
				STYLE_Z_INDEX);
	}

	/**
	 * Set the background-color css property.
	 */
	static void setBackgroundColor(ClientDomStyle domStyle, String value) {
		domStyle.setProperty(STYLE_BACKGROUND_COLOR, value);
	}

	/**
	 * Set the background-image css property.
	 */
	static void setBackgroundImage(ClientDomStyle domStyle, @IsSafeUri
	String value) {
		domStyle.setProperty(STYLE_BACKGROUND_IMAGE, value);
	}

	/**
	 * Set the border-color css property.
	 */
	static void setBorderColor(ClientDomStyle domStyle, String value) {
		domStyle.setProperty(STYLE_BORDER_COLOR, value);
	}

	/**
	 * Sets the border-style CSS property.
	 */
	static void setBorderStyle(ClientDomStyle domStyle, BorderStyle value) {
		domStyle.setProperty(STYLE_BORDER_STYLE, value.getCssName());
	}

	/**
	 * Set the border-width css property.
	 */
	static void setBorderWidth(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_BORDER_WIDTH, value, unit);
	}

	/**
	 * Set the bottom css property.
	 */
	static void setBottom(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_BOTTOM, value, unit);
	}

	/**
	 * Sets the 'clear' CSS property.
	 */
	static void setClear(ClientDomStyle domStyle, Clear value) {
		domStyle.setProperty(STYLE_CLEAR, value.getCssName());
	}

	/**
	 * Sets the color CSS property.
	 */
	static void setColor(ClientDomStyle domStyle, String value) {
		domStyle.setProperty(STYLE_COLOR, value);
	}

	/**
	 * Sets the cursor CSS property.
	 */
	static void setCursor(ClientDomStyle domStyle, Cursor value) {
		domStyle.setProperty(STYLE_CURSOR, value.getCssName());
	}

	/**
	 * Sets the display CSS property.
	 */
	static void setDisplay(ClientDomStyle domStyle, Display value) {
		domStyle.setProperty(STYLE_DISPLAY, value.getCssName());
	}

	/**
	 * Set the float css property.
	 */
	static void setFloat(ClientDomStyle domStyle, Float value) {
		domStyle.setProperty(DOMImpl.impl.cssFloatPropertyName(),
				value.getCssName());
	}

	/**
	 * Set the font-size css property.
	 */
	static void setFontSize(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_FONT_SIZE, value, unit);
	}

	/**
	 * Sets the font-style CSS property.
	 */
	static void setFontStyle(ClientDomStyle domStyle, FontStyle value) {
		domStyle.setProperty(STYLE_FONT_STYLE, value.getCssName());
	}

	/**
	 * Sets the font-weight CSS property.
	 */
	static void setFontWeight(ClientDomStyle domStyle, FontWeight value) {
		domStyle.setProperty(STYLE_FONT_WEIGHT, value.getCssName());
	}

	/**
	 * Set the height css property.
	 */
	static void setHeight(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_HEIGHT, value, unit);
	}

	/**
	 * Set the left css property.
	 */
	static void setLeft(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_LEFT, value, unit);
	}

	/**
	 * Set the line-height css property.
	 */
	static void setLineHeight(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_LINE_HEIGHT, value, unit);
	}

	/**
	 * Sets the list-style-type CSS property.
	 */
	static void setListStyleType(ClientDomStyle domStyle, ListStyleType value) {
		domStyle.setProperty(STYLE_LIST_STYLE_TYPE, value.getCssName());
	}

	/**
	 * Set the margin css property.
	 */
	static void setMargin(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_MARGIN, value, unit);
	}

	/**
	 * Set the margin-bottom css property.
	 */
	static void setMarginBottom(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_MARGIN_BOTTOM, value, unit);
	}

	/**
	 * Set the margin-left css property.
	 */
	static void setMarginLeft(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_MARGIN_LEFT, value, unit);
	}

	/**
	 * Set the margin-right css property.
	 */
	static void setMarginRight(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_MARGIN_RIGHT, value, unit);
	}

	/**
	 * Set the margin-top css property.
	 */
	static void setMarginTop(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_MARGIN_TOP, value, unit);
	}

	/**
	 * Set the opacity css property.
	 */
	static void setOpacity(ClientDomStyle domStyle, String value) {
		DOMImpl.impl.cssSetOpacity(domStyle.styleObject(), value);
	}

	/**
	 * Set the outline-color css property.
	 */
	static void setOutlineColor(ClientDomStyle domStyle, String value) {
		domStyle.setProperty(STYLE_OUTLINE_COLOR, value);
	}

	/**
	 * Sets the outline-style CSS property.
	 */
	static void setOutlineStyle(ClientDomStyle domStyle, OutlineStyle value) {
		domStyle.setProperty(STYLE_OUTLINE_STYLE, value.getCssName());
	}

	/**
	 * Set the outline-width css property.
	 */
	static void setOutlineWidth(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_OUTLINE_WIDTH, value, unit);
	}

	/**
	 * Sets the overflow CSS property.
	 */
	static void setOverflow(ClientDomStyle domStyle, Overflow value) {
		domStyle.setProperty(STYLE_OVERFLOW, value.getCssName());
	}

	/**
	 * Sets the overflow-x CSS property.
	 */
	static void setOverflowX(ClientDomStyle domStyle, Overflow value) {
		domStyle.setProperty(STYLE_OVERFLOW_X, value.getCssName());
	}

	/**
	 * Sets the overflow-y CSS property.
	 */
	static void setOverflowY(ClientDomStyle domStyle, Overflow value) {
		domStyle.setProperty(STYLE_OVERFLOW_Y, value.getCssName());
	}

	/**
	 * Set the padding css property.
	 */
	static void setPadding(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_PADDING, value, unit);
	}

	/**
	 * Set the padding-bottom css property.
	 */
	static void setPaddingBottom(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_PADDING_BOTTOM, value, unit);
	}

	/**
	 * Set the padding-left css property.
	 */
	static void setPaddingLeft(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_PADDING_LEFT, value, unit);
	}

	/**
	 * Set the padding-right css property.
	 */
	static void setPaddingRight(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_PADDING_RIGHT, value, unit);
	}

	/**
	 * Set the padding-top css property.
	 */
	static void setPaddingTop(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_PADDING_TOP, value, unit);
	}

	/**
	 * Sets the position CSS property.
	 */
	static void setPosition(ClientDomStyle domStyle, Position value) {
		domStyle.setProperty(STYLE_POSITION, value.getCssName());
	}

	/**
	 * Sets the value of a named property in the specified units.
	 */
	static void setProperty(ClientDomStyle domStyle, String name, double value,
			Unit unit) {
		assertCamelCase(name);
		domStyle.setPropertyImpl(name, value + unit.getType());
	}

	/**
	 * Sets the value of a named property.
	 */
	static void setProperty(ClientDomStyle domStyle, String name,
			String value) {
		assertCamelCase(name);
		domStyle.setPropertyImpl(name, value);
	}

	/**
	 * Sets the value of a named property, in pixels.
	 *
	 * This is shorthand for <code>value + "px"</code>.
	 */
	static void setPropertyPx(ClientDomStyle domStyle, String name, int value) {
		domStyle.setProperty(name, value, Unit.PX);
	}

	/**
	 * Set the right css property.
	 */
	static void setRight(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_RIGHT, value, unit);
	}

	/**
	 * Set the table-layout CSS property.
	 */
	static void setTableLayout(ClientDomStyle domStyle, TableLayout value) {
		domStyle.setProperty(STYLE_TABLE_LAYOUT, value.getCssName());
	}

	/**
	 * Set the 'text-align' CSS property.
	 */
	static void setTextAlign(ClientDomStyle domStyle, TextAlign value) {
		domStyle.setProperty(STYLE_TEXT_ALIGN, value.getCssName());
	}

	/**
	 * Sets the text-decoration CSS property.
	 */
	static void setTextDecoration(ClientDomStyle domStyle,
			TextDecoration value) {
		domStyle.setProperty(STYLE_TEXT_DECORATION, value.getCssName());
	}

	/**
	 * Set the 'text-indent' CSS property.
	 */
	static void setTextIndent(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_TEXT_INDENT, value, unit);
	}

	/**
	 * Set the 'text-justify' CSS3 property.
	 */
	static void setTextJustify(ClientDomStyle domStyle, TextJustify value) {
		domStyle.setProperty(STYLE_TEXT_JUSTIFY, value.getCssName());
	}

	/**
	 * Set the 'text-overflow' CSS3 property.
	 */
	static void setTextOverflow(ClientDomStyle domStyle, TextOverflow value) {
		domStyle.setProperty(STYLE_TEXT_OVERFLOW, value.getCssName());
	}

	/**
	 * Set the 'text-transform' CSS property.
	 */
	static void setTextTransform(ClientDomStyle domStyle, TextTransform value) {
		domStyle.setProperty(STYLE_TEXT_TRANSFORM, value.getCssName());
	}

	/**
	 * Set the top css property.
	 */
	static void setTop(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_TOP, value, unit);
	}

	/**
	 * Sets the vertical-align CSS property.
	 */
	static void setVerticalAlign(ClientDomStyle domStyle, double value,
			Unit unit) {
		domStyle.setProperty(STYLE_VERTICAL_ALIGN, value, unit);
	}

	/**
	 * Sets the vertical-align CSS property.
	 */
	static void setVerticalAlign(ClientDomStyle domStyle, VerticalAlign value) {
		domStyle.setProperty(STYLE_VERTICAL_ALIGN, value.getCssName());
	}

	/**
	 * Sets the visibility CSS property.
	 */
	static void setVisibility(ClientDomStyle domStyle, Visibility value) {
		domStyle.setProperty(STYLE_VISIBILITY, value.getCssName());
	}

	/**
	 * Set the 'white-space' CSS property.
	 */
	static void setWhiteSpace(ClientDomStyle domStyle, WhiteSpace value) {
		domStyle.setProperty(STYLE_WHITE_SPACE, value.getCssName());
	}

	/**
	 * Set the width css property.
	 */
	static void setWidth(ClientDomStyle domStyle, double value, Unit unit) {
		domStyle.setProperty(STYLE_WIDTH, value, unit);
	}

	/**
	 * Set the z-index css property.
	 */
	static void setZIndex(ClientDomStyle domStyle, int value) {
		domStyle.setProperty(STYLE_Z_INDEX, value + "");
	}
}
