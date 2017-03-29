/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dom.client;

import static com.google.gwt.dom.client.DomStyleConstants.*;

import java.util.Map;

/**
 * Provides programmatic access to properties of the style object.
 * 
 * <p>
 * Note that if a property or value is not explicitly enumerated in this class,
 * you can still access it via {@link #getProperty(String)}, and
 * {@link #setProperty(String, String)}.
 * </p>
 * 
 * @see Element#getStyle()
 */
public class Style implements DomStyle {
	/**
	 * Interface to be implemented by enumerated CSS values.
	 */
	public interface HasCssName {
		/**
		 * Gets the CSS name associated with this value.
		 */
		String getCssName();
	}

	/**
	 * CSS length units.
	 */
	public enum Unit {
		PX {
			@Override
			public String getType() {
				return UNIT_PX;
			}
		},
		PCT {
			@Override
			public String getType() {
				return UNIT_PCT;
			}
		},
		EM {
			@Override
			public String getType() {
				return UNIT_EM;
			}
		},
		EX {
			@Override
			public String getType() {
				return UNIT_EX;
			}
		},
		PT {
			@Override
			public String getType() {
				return UNIT_PT;
			}
		},
		PC {
			@Override
			public String getType() {
				return UNIT_PC;
			}
		},
		IN {
			@Override
			public String getType() {
				return UNIT_IN;
			}
		},
		CM {
			@Override
			public String getType() {
				return UNIT_CM;
			}
		},
		MM {
			@Override
			public String getType() {
				return UNIT_MM;
			}
		};
		public abstract String getType();
	}

	/**
	 * Enum for the border-style property.
	 */
	public enum BorderStyle implements HasCssName {
		NONE {
			@Override
			public String getCssName() {
				return BORDER_STYLE_NONE;
			}
		},
		DOTTED {
			@Override
			public String getCssName() {
				return BORDER_STYLE_DOTTED;
			}
		},
		DASHED {
			@Override
			public String getCssName() {
				return BORDER_STYLE_DASHED;
			}
		},
		HIDDEN {
			@Override
			public String getCssName() {
				return BORDER_STYLE_HIDDEN;
			}
		},
		SOLID {
			@Override
			public String getCssName() {
				return BORDER_STYLE_SOLID;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the 'clear' CSS property.
	 */
	public enum Clear implements HasCssName {
		BOTH {
			@Override
			public String getCssName() {
				return CLEAR_BOTH;
			}
		},
		LEFT {
			@Override
			public String getCssName() {
				return CLEAR_LEFT;
			}
		},
		NONE {
			@Override
			public String getCssName() {
				return CLEAR_NONE;
			}
		},
		RIGHT {
			@Override
			public String getCssName() {
				return CLEAR_RIGHT;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the cursor property.
	 */
	public enum Cursor implements HasCssName {
		DEFAULT {
			@Override
			public String getCssName() {
				return CURSOR_DEFAULT;
			}
		},
		AUTO {
			@Override
			public String getCssName() {
				return CURSOR_AUTO;
			}
		},
		CROSSHAIR {
			@Override
			public String getCssName() {
				return CURSOR_CROSSHAIR;
			}
		},
		POINTER {
			@Override
			public String getCssName() {
				return CURSOR_POINTER;
			}
		},
		MOVE {
			@Override
			public String getCssName() {
				return CURSOR_MOVE;
			}
		},
		E_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_E_RESIZE;
			}
		},
		NE_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_NE_RESIZE;
			}
		},
		NW_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_NW_RESIZE;
			}
		},
		N_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_N_RESIZE;
			}
		},
		SE_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_SE_RESIZE;
			}
		},
		SW_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_SW_RESIZE;
			}
		},
		S_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_S_RESIZE;
			}
		},
		W_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_W_RESIZE;
			}
		},
		TEXT {
			@Override
			public String getCssName() {
				return CURSOR_TEXT;
			}
		},
		WAIT {
			@Override
			public String getCssName() {
				return CURSOR_WAIT;
			}
		},
		HELP {
			@Override
			public String getCssName() {
				return CURSOR_HELP;
			}
		},
		COL_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_COL_RESIZE;
			}
		},
		ROW_RESIZE {
			@Override
			public String getCssName() {
				return CURSOR_ROW_RESIZE;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the display property.
	 */
	public enum Display implements HasCssName {
		NONE {
			@Override
			public String getCssName() {
				return DISPLAY_NONE;
			}
		},
		BLOCK {
			@Override
			public String getCssName() {
				return DISPLAY_BLOCK;
			}
		},
		INLINE {
			@Override
			public String getCssName() {
				return DISPLAY_INLINE;
			}
		},
		INLINE_BLOCK {
			@Override
			public String getCssName() {
				return DISPLAY_INLINE_BLOCK;
			}
		},
		INLINE_TABLE {
			@Override
			public String getCssName() {
				return DISPLAY_INLINE_TABLE;
			}
		},
		LIST_ITEM {
			@Override
			public String getCssName() {
				return DISPLAY_LIST_ITEM;
			}
		},
		RUN_IN {
			@Override
			public String getCssName() {
				return DISPLAY_RUN_IN;
			}
		},
		TABLE {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE;
			}
		},
		TABLE_CAPTION {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE_CAPTION;
			}
		},
		TABLE_COLUMN_GROUP {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE_COLUMN_GROUP;
			}
		},
		TABLE_HEADER_GROUP {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE_HEADER_GROUP;
			}
		},
		TABLE_FOOTER_GROUP {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE_FOOTER_GROUP;
			}
		},
		TABLE_ROW_GROUP {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE_ROW_GROUP;
			}
		},
		TABLE_CELL {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE_CELL;
			}
		},
		TABLE_COLUMN {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE_COLUMN;
			}
		},
		TABLE_ROW {
			@Override
			public String getCssName() {
				return DISPLAY_TABLE_ROW;
			}
		},
		INITIAL {
			@Override
			public String getCssName() {
				return DISPLAY_INITIAL;
			}
		},
		FLEX {
			@Override
			public String getCssName() {
				return DISPLAY_FLEX;
			}
		},
		INLINE_FLEX {
			@Override
			public String getCssName() {
				return DISPLAY_INLINE_FLEX;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the float property.
	 */
	public enum Float implements HasCssName {
		LEFT {
			@Override
			public String getCssName() {
				return FLOAT_LEFT;
			}
		},
		RIGHT {
			@Override
			public String getCssName() {
				return FLOAT_RIGHT;
			}
		},
		NONE {
			@Override
			public String getCssName() {
				return FLOAT_NONE;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the font-style property.
	 */
	public enum FontStyle implements HasCssName {
		NORMAL {
			@Override
			public String getCssName() {
				return FONT_STYLE_NORMAL;
			}
		},
		ITALIC {
			@Override
			public String getCssName() {
				return FONT_STYLE_ITALIC;
			}
		},
		OBLIQUE {
			@Override
			public String getCssName() {
				return FONT_STYLE_OBLIQUE;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the font-weight property.
	 */
	public enum FontWeight implements HasCssName {
		NORMAL {
			@Override
			public String getCssName() {
				return FONT_WEIGHT_NORMAL;
			}
		},
		BOLD {
			@Override
			public String getCssName() {
				return FONT_WEIGHT_BOLD;
			}
		},
		BOLDER {
			@Override
			public String getCssName() {
				return FONT_WEIGHT_BOLDER;
			}
		},
		LIGHTER {
			@Override
			public String getCssName() {
				return FONT_WEIGHT_LIGHTER;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the list-style-type property.
	 */
	public enum ListStyleType implements HasCssName {
		NONE {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_NONE;
			}
		},
		DISC {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_DISC;
			}
		},
		CIRCLE {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_CIRCLE;
			}
		},
		SQUARE {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_SQUARE;
			}
		},
		DECIMAL {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_DECIMAL;
			}
		},
		LOWER_ALPHA {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_LOWER_ALPHA;
			}
		},
		UPPER_ALPHA {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_UPPER_ALPHA;
			}
		},
		LOWER_ROMAN {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_LOWER_ROMAN;
			}
		},
		UPPER_ROMAN {
			@Override
			public String getCssName() {
				return LIST_STYLE_TYPE_UPPER_ROMAN;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the outline-style property.
	 */
	public enum OutlineStyle implements HasCssName {
		NONE {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_NONE;
			}
		},
		DASHED {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_DASHED;
			}
		},
		DOTTED {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_DOTTED;
			}
		},
		DOUBLE {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_DOUBLE;
			}
		},
		GROOVE {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_GROOVE;
			}
		},
		INSET {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_INSET;
			}
		},
		OUTSET {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_OUTSET;
			}
		},
		RIDGE {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_RIDGE;
			}
		},
		SOLID {
			@Override
			public String getCssName() {
				return OUTLINE_STYLE_SOLID;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the overflow property.
	 */
	public enum Overflow implements HasCssName {
		VISIBLE {
			@Override
			public String getCssName() {
				return OVERFLOW_VISIBLE;
			}
		},
		HIDDEN {
			@Override
			public String getCssName() {
				return OVERFLOW_HIDDEN;
			}
		},
		SCROLL {
			@Override
			public String getCssName() {
				return OVERFLOW_SCROLL;
			}
		},
		AUTO {
			@Override
			public String getCssName() {
				return OVERFLOW_AUTO;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the position property.
	 */
	public enum Position implements HasCssName {
		STATIC {
			@Override
			public String getCssName() {
				return POSITION_STATIC;
			}
		},
		RELATIVE {
			@Override
			public String getCssName() {
				return POSITION_RELATIVE;
			}
		},
		ABSOLUTE {
			@Override
			public String getCssName() {
				return POSITION_ABSOLUTE;
			}
		},
		FIXED {
			@Override
			public String getCssName() {
				return POSITION_FIXED;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the table-layout property.
	 */
	public enum TableLayout implements HasCssName {
		AUTO {
			@Override
			public String getCssName() {
				return TABLE_LAYOUT_AUTO;
			}
		},
		FIXED {
			@Override
			public String getCssName() {
				return TABLE_LAYOUT_FIXED;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the text-align property.
	 */
	public enum TextAlign implements HasCssName {
		CENTER {
			@Override
			public String getCssName() {
				return TEXT_ALIGN_CENTER;
			}
		},
		JUSTIFY {
			@Override
			public String getCssName() {
				return TEXT_ALIGN_JUSTIFY;
			}
		},
		LEFT {
			@Override
			public String getCssName() {
				return TEXT_ALIGN_LEFT;
			}
		},
		RIGHT {
			@Override
			public String getCssName() {
				return TEXT_ALIGN_RIGHT;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the 'text-decoration' CSS property.
	 */
	public enum TextDecoration implements HasCssName {
		BLINK {
			@Override
			public String getCssName() {
				return TEXT_DECORATION_BLINK;
			}
		},
		LINE_THROUGH {
			@Override
			public String getCssName() {
				return TEXT_DECORATION_LINE_THROUGH;
			}
		},
		NONE {
			@Override
			public String getCssName() {
				return TEXT_DECORATION_NONE;
			}
		},
		OVERLINE {
			@Override
			public String getCssName() {
				return TEXT_DECORATION_OVERLINE;
			}
		},
		UNDERLINE {
			@Override
			public String getCssName() {
				return TEXT_DECORATION_UNDERLINE;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the 'text-justify' CSS3 property.
	 */
	public enum TextJustify implements HasCssName {
		AUTO {
			@Override
			public String getCssName() {
				return TEXT_JUSTIFY_AUTO;
			}
		},
		DISTRIBUTE {
			@Override
			public String getCssName() {
				return TEXT_JUSTIFY_DISTRIBUTE;
			}
		},
		INTER_CLUSTER {
			@Override
			public String getCssName() {
				return TEXT_JUSTIFY_INTER_CLUSTER;
			}
		},
		INTER_IDEOGRAPH {
			@Override
			public String getCssName() {
				return TEXT_JUSTIFY_INTER_IDEOGRAPH;
			}
		},
		INTER_WORD {
			@Override
			public String getCssName() {
				return TEXT_JUSTIFY_INTER_WORD;
			}
		},
		KASHIDA {
			@Override
			public String getCssName() {
				return TEXT_JUSTIFY_KASHIDA;
			}
		},
		NONE {
			@Override
			public String getCssName() {
				return TEXT_JUSTIFY_NONE;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the 'text-overflow' CSS3 property.
	 */
	public enum TextOverflow implements HasCssName {
		CLIP {
			@Override
			public String getCssName() {
				return TEXT_OVERFLOW_CLIP;
			}
		},
		ELLIPSIS {
			@Override
			public String getCssName() {
				return TEXT_OVERFLOW_ELLIPSIS;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the 'text-transform' CSS property.
	 */
	public enum TextTransform implements HasCssName {
		CAPITALIZE {
			@Override
			public String getCssName() {
				return TEXT_TRANSFORM_CAPITALIZE;
			}
		},
		NONE {
			@Override
			public String getCssName() {
				return TEXT_TRANSFORM_NONE;
			}
		},
		LOWERCASE {
			@Override
			public String getCssName() {
				return TEXT_TRANSFORM_LOWERCASE;
			}
		},
		UPPERCASE {
			@Override
			public String getCssName() {
				return TEXT_TRANSFORM_UPPERCASE;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the vertical-align property.
	 */
	public enum VerticalAlign implements HasCssName {
		BASELINE {
			@Override
			public String getCssName() {
				return VERTICAL_ALIGN_BASELINE;
			}
		},
		SUB {
			@Override
			public String getCssName() {
				return VERTICAL_ALIGN_SUB;
			}
		},
		SUPER {
			@Override
			public String getCssName() {
				return VERTICAL_ALIGN_SUPER;
			}
		},
		TOP {
			@Override
			public String getCssName() {
				return VERTICAL_ALIGN_TOP;
			}
		},
		TEXT_TOP {
			@Override
			public String getCssName() {
				return VERTICAL_ALIGN_TEXT_TOP;
			}
		},
		MIDDLE {
			@Override
			public String getCssName() {
				return VERTICAL_ALIGN_MIDDLE;
			}
		},
		BOTTOM {
			@Override
			public String getCssName() {
				return VERTICAL_ALIGN_BOTTOM;
			}
		},
		TEXT_BOTTOM {
			@Override
			public String getCssName() {
				return VERTICAL_ALIGN_TEXT_BOTTOM;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the visibility property.
	 */
	public enum Visibility implements HasCssName {
		VISIBLE {
			@Override
			public String getCssName() {
				return VISIBILITY_VISIBLE;
			}
		},
		HIDDEN {
			@Override
			public String getCssName() {
				return VISIBILITY_HIDDEN;
			}
		};
		@Override
		public abstract String getCssName();
	}

	/**
	 * Enum for the 'white-space' CSS property.
	 */
	public enum WhiteSpace implements HasCssName {
		NORMAL {
			@Override
			public String getCssName() {
				return WHITE_SPACE_NORMAL;
			}
		},
		NOWRAP {
			@Override
			public String getCssName() {
				return WHITE_SPACE_NOWRAP;
			}
		},
		PRE {
			@Override
			public String getCssName() {
				return WHITE_SPACE_PRE;
			}
		},
		PRE_LINE {
			@Override
			public String getCssName() {
				return WHITE_SPACE_PRE_LINE;
			}
		},
		PRE_WRAP {
			@Override
			public String getCssName() {
				return WHITE_SPACE_PRE_WRAP;
			}
		};
		@Override
		public abstract String getCssName();
	}

	protected Style() {
	}

	DomStyle impl;

	public void clearBackgroundColor() {
		this.impl.clearBackgroundColor();
	}

	public void clearBackgroundImage() {
		this.impl.clearBackgroundImage();
	}

	public void clearBorderColor() {
		this.impl.clearBorderColor();
	}

	public void clearBorderStyle() {
		this.impl.clearBorderStyle();
	}

	public void clearBorderWidth() {
		this.impl.clearBorderWidth();
	}

	public void clearBottom() {
		this.impl.clearBottom();
	}

	public void clearClear() {
		this.impl.clearClear();
	}

	public void clearColor() {
		this.impl.clearColor();
	}

	public void clearCursor() {
		this.impl.clearCursor();
	}

	public void clearDisplay() {
		this.impl.clearDisplay();
	}

	public void clearFloat() {
		this.impl.clearFloat();
	}

	public void clearFontSize() {
		this.impl.clearFontSize();
	}

	public void clearFontStyle() {
		this.impl.clearFontStyle();
	}

	public void clearFontWeight() {
		this.impl.clearFontWeight();
	}

	public void clearHeight() {
		this.impl.clearHeight();
	}

	public void clearLeft() {
		this.impl.clearLeft();
	}

	public void clearLineHeight() {
		this.impl.clearLineHeight();
	}

	public void clearListStyleType() {
		this.impl.clearListStyleType();
	}

	public void clearMargin() {
		this.impl.clearMargin();
	}

	public void clearMarginBottom() {
		this.impl.clearMarginBottom();
	}

	public void clearMarginLeft() {
		this.impl.clearMarginLeft();
	}

	public void clearMarginRight() {
		this.impl.clearMarginRight();
	}

	public void clearMarginTop() {
		this.impl.clearMarginTop();
	}

	public void clearOpacity() {
		this.impl.clearOpacity();
	}

	public Style styleObject() {
		return this.impl.styleObject();
	}

	public void clearOutlineColor() {
		this.impl.clearOutlineColor();
	}

	public void clearOutlineStyle() {
		this.impl.clearOutlineStyle();
	}

	public void clearOutlineWidth() {
		this.impl.clearOutlineWidth();
	}

	public void clearOverflow() {
		this.impl.clearOverflow();
	}

	public void clearOverflowX() {
		this.impl.clearOverflowX();
	}

	public void clearOverflowY() {
		this.impl.clearOverflowY();
	}

	public void clearPadding() {
		this.impl.clearPadding();
	}

	public void clearPaddingBottom() {
		this.impl.clearPaddingBottom();
	}

	public void clearPaddingLeft() {
		this.impl.clearPaddingLeft();
	}

	public void clearPaddingRight() {
		this.impl.clearPaddingRight();
	}

	public void clearPaddingTop() {
		this.impl.clearPaddingTop();
	}

	public void clearPosition() {
		this.impl.clearPosition();
	}

	public void clearProperty(String name) {
		this.impl.clearProperty(name);
	}

	public void clearRight() {
		this.impl.clearRight();
	}

	public void clearTableLayout() {
		this.impl.clearTableLayout();
	}

	public void clearTextAlign() {
		this.impl.clearTextAlign();
	}

	public void clearTextDecoration() {
		this.impl.clearTextDecoration();
	}

	public void clearTextIndent() {
		this.impl.clearTextIndent();
	}

	public void clearTextJustify() {
		this.impl.clearTextJustify();
	}

	public void clearTextOverflow() {
		this.impl.clearTextOverflow();
	}

	public void clearTextTransform() {
		this.impl.clearTextTransform();
	}

	public void clearTop() {
		this.impl.clearTop();
	}

	public void clearVisibility() {
		this.impl.clearVisibility();
	}

	public void clearWhiteSpace() {
		this.impl.clearWhiteSpace();
	}

	public void clearWidth() {
		this.impl.clearWidth();
	}

	public void clearZIndex() {
		this.impl.clearZIndex();
	}

	public String getBackgroundColor() {
		return this.impl.getBackgroundColor();
	}

	public String getBackgroundImage() {
		return this.impl.getBackgroundImage();
	}

	public String getBorderColor() {
		return this.impl.getBorderColor();
	}

	public String getBorderStyle() {
		return this.impl.getBorderStyle();
	}

	public String getBorderWidth() {
		return this.impl.getBorderWidth();
	}

	public String getBottom() {
		return this.impl.getBottom();
	}

	public String getClear() {
		return this.impl.getClear();
	}

	public String getColor() {
		return this.impl.getColor();
	}

	public String getCursor() {
		return this.impl.getCursor();
	}

	public String getDisplay() {
		return this.impl.getDisplay();
	}

	public String getFontSize() {
		return this.impl.getFontSize();
	}

	public String getFontStyle() {
		return this.impl.getFontStyle();
	}

	public String getFontWeight() {
		return this.impl.getFontWeight();
	}

	public String getHeight() {
		return this.impl.getHeight();
	}

	public String getLeft() {
		return this.impl.getLeft();
	}

	public String getLineHeight() {
		return this.impl.getLineHeight();
	}

	public String getListStyleType() {
		return this.impl.getListStyleType();
	}

	public String getMargin() {
		return this.impl.getMargin();
	}

	public String getMarginBottom() {
		return this.impl.getMarginBottom();
	}

	public String getMarginLeft() {
		return this.impl.getMarginLeft();
	}

	public String getMarginRight() {
		return this.impl.getMarginRight();
	}

	public String getMarginTop() {
		return this.impl.getMarginTop();
	}

	public String getOpacity() {
		return this.impl.getOpacity();
	}

	public String getOverflow() {
		return this.impl.getOverflow();
	}

	public String getOverflowX() {
		return this.impl.getOverflowX();
	}

	public String getOverflowY() {
		return this.impl.getOverflowY();
	}

	public String getPadding() {
		return this.impl.getPadding();
	}

	public String getPaddingBottom() {
		return this.impl.getPaddingBottom();
	}

	public String getPaddingLeft() {
		return this.impl.getPaddingLeft();
	}

	public String getPaddingRight() {
		return this.impl.getPaddingRight();
	}

	public String getPaddingTop() {
		return this.impl.getPaddingTop();
	}

	public String getPosition() {
		return this.impl.getPosition();
	}

	public String getProperty(String name) {
		return this.impl.getProperty(name);
	}

	public String getRight() {
		return this.impl.getRight();
	}

	public String getTableLayout() {
		return this.impl.getTableLayout();
	}

	public String getTextAlign() {
		return this.impl.getTextAlign();
	}

	public String getTextDecoration() {
		return this.impl.getTextDecoration();
	}

	public String getTextIndent() {
		return this.impl.getTextIndent();
	}

	public String getTextJustify() {
		return this.impl.getTextJustify();
	}

	public String getTextOverflow() {
		return this.impl.getTextOverflow();
	}

	public String getTextTransform() {
		return this.impl.getTextTransform();
	}

	public String getTop() {
		return this.impl.getTop();
	}

	public String getVerticalAlign() {
		return this.impl.getVerticalAlign();
	}

	public String getVisibility() {
		return this.impl.getVisibility();
	}

	public String getWhiteSpace() {
		return this.impl.getWhiteSpace();
	}

	public String getWidth() {
		return this.impl.getWidth();
	}

	public String getZIndex() {
		return this.impl.getZIndex();
	}

	public void setBackgroundColor(String value) {
		this.impl.setBackgroundColor(value);
	}

	public void setBackgroundImage(String value) {
		this.impl.setBackgroundImage(value);
	}

	public void setBorderColor(String value) {
		this.impl.setBorderColor(value);
	}

	public void setBorderStyle(BorderStyle value) {
		this.impl.setBorderStyle(value);
	}

	public void setBorderWidth(double value, Unit unit) {
		this.impl.setBorderWidth(value, unit);
	}

	public void setBottom(double value, Unit unit) {
		this.impl.setBottom(value, unit);
	}

	public void setClear(Clear value) {
		this.impl.setClear(value);
	}

	public void setColor(String value) {
		this.impl.setColor(value);
	}

	public void setCursor(Cursor value) {
		this.impl.setCursor(value);
	}

	public void setDisplay(Display value) {
		this.impl.setDisplay(value);
	}

	public void setFloat(Float value) {
		this.impl.setFloat(value);
	}

	public void setFontSize(double value, Unit unit) {
		this.impl.setFontSize(value, unit);
	}

	public void setFontStyle(FontStyle value) {
		this.impl.setFontStyle(value);
	}

	public void setFontWeight(FontWeight value) {
		this.impl.setFontWeight(value);
	}

	public void setHeight(double value, Unit unit) {
		this.impl.setHeight(value, unit);
	}

	public void setLeft(double value, Unit unit) {
		this.impl.setLeft(value, unit);
	}

	public void setLineHeight(double value, Unit unit) {
		this.impl.setLineHeight(value, unit);
	}

	public void setListStyleType(ListStyleType value) {
		this.impl.setListStyleType(value);
	}

	public void setMargin(double value, Unit unit) {
		this.impl.setMargin(value, unit);
	}

	public void setMarginBottom(double value, Unit unit) {
		this.impl.setMarginBottom(value, unit);
	}

	public void setMarginLeft(double value, Unit unit) {
		this.impl.setMarginLeft(value, unit);
	}

	public void setMarginRight(double value, Unit unit) {
		this.impl.setMarginRight(value, unit);
	}

	public void setMarginTop(double value, Unit unit) {
		this.impl.setMarginTop(value, unit);
	}

	public void setOpacity(double value) {
		this.impl.setOpacity(value);
	}

	public void setOutlineColor(String value) {
		this.impl.setOutlineColor(value);
	}

	public void setOutlineStyle(OutlineStyle value) {
		this.impl.setOutlineStyle(value);
	}

	public void setOutlineWidth(double value, Unit unit) {
		this.impl.setOutlineWidth(value, unit);
	}

	public void setOverflow(Overflow value) {
		this.impl.setOverflow(value);
	}

	public void setOverflowX(Overflow value) {
		this.impl.setOverflowX(value);
	}

	public void setOverflowY(Overflow value) {
		this.impl.setOverflowY(value);
	}

	public void setPadding(double value, Unit unit) {
		this.impl.setPadding(value, unit);
	}

	public void setPaddingBottom(double value, Unit unit) {
		this.impl.setPaddingBottom(value, unit);
	}

	public void setPaddingLeft(double value, Unit unit) {
		this.impl.setPaddingLeft(value, unit);
	}

	public void setPaddingRight(double value, Unit unit) {
		this.impl.setPaddingRight(value, unit);
	}

	public void setPaddingTop(double value, Unit unit) {
		this.impl.setPaddingTop(value, unit);
	}

	public void setPosition(Position value) {
		this.impl.setPosition(value);
	}

	public void setProperty(String name, String value) {
		this.impl.setProperty(name, value);
	}

	public void setProperty(String name, double value, Unit unit) {
		this.impl.setProperty(name, value, unit);
	}

	public void setPropertyPx(String name, int value) {
		this.impl.setPropertyPx(name, value);
	}

	public void setRight(double value, Unit unit) {
		this.impl.setRight(value, unit);
	}

	public void setTableLayout(TableLayout value) {
		this.impl.setTableLayout(value);
	}

	public void setTextAlign(TextAlign value) {
		this.impl.setTextAlign(value);
	}

	public void setTextDecoration(TextDecoration value) {
		this.impl.setTextDecoration(value);
	}

	public void setTextIndent(double value, Unit unit) {
		this.impl.setTextIndent(value, unit);
	}

	public void setTextJustify(TextJustify value) {
		this.impl.setTextJustify(value);
	}

	public void setTextOverflow(TextOverflow value) {
		this.impl.setTextOverflow(value);
	}

	public void setTextTransform(TextTransform value) {
		this.impl.setTextTransform(value);
	}

	public void setTop(double value, Unit unit) {
		this.impl.setTop(value, unit);
	}

	public void setVerticalAlign(VerticalAlign value) {
		this.impl.setVerticalAlign(value);
	}

	public void setVerticalAlign(double value, Unit unit) {
		this.impl.setVerticalAlign(value, unit);
	}

	public void setVisibility(Visibility value) {
		this.impl.setVisibility(value);
	}

	public void setWhiteSpace(WhiteSpace value) {
		this.impl.setWhiteSpace(value);
	}

	public void setWidth(double value, Unit unit) {
		this.impl.setWidth(value, unit);
	}

	public void setZIndex(int value) {
		this.impl.setZIndex(value);
	}

	public String getPropertyImpl(String name) {
		return this.impl.getPropertyImpl(name);
	}

	public void setPropertyImpl(String name, String value) {
		this.impl.setPropertyImpl(name, value);
	}

	boolean resolved;

	public Style_Jso domImpl() {
		return (Style_Jso) impl;
	}

	 boolean provideIsLocal() {
		return !resolved;
	}

	public Map<String,String> getProperties() {
		return this.impl.getProperties();
	}
	
}
