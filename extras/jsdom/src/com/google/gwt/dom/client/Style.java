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
import java.util.Objects;
import java.util.Optional;

import com.google.gwt.user.client.LocalDomDebug;

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
    private Element element;

    StyleLocal local;

    private DomStyle remote = StyleNull.INSTANCE;

    protected Style(Element element) {
        this.element = element;
        local = new StyleLocal(this);
    }

    @Override
    public void clearBackgroundColor() {
        local().clearBackgroundColor();
        remote().clearBackgroundColor();
    }

    @Override
    public void clearBackgroundImage() {
        local().clearBackgroundImage();
        remote().clearBackgroundImage();
    }

    @Override
    public void clearBorderColor() {
        local().clearBorderColor();
        remote().clearBorderColor();
    }

    @Override
    public void clearBorderStyle() {
        local().clearBorderStyle();
        remote().clearBorderStyle();
    }

    @Override
    public void clearBorderWidth() {
        local().clearBorderWidth();
        remote().clearBorderWidth();
    }

    @Override
    public void clearBottom() {
        local().clearBottom();
        remote().clearBottom();
    }

    @Override
    public void clearClear() {
        local().clearClear();
        remote().clearClear();
    }

    @Override
    public void clearColor() {
        local().clearColor();
        remote().clearColor();
    }

    @Override
    public void clearCursor() {
        local().clearCursor();
        remote().clearCursor();
    }

    @Override
    public void clearDisplay() {
        local().clearDisplay();
        remote().clearDisplay();
    }

    @Override
    public void clearFloat() {
        local().clearFloat();
        remote().clearFloat();
    }

    @Override
    public void clearFontSize() {
        local().clearFontSize();
        remote().clearFontSize();
    }

    @Override
    public void clearFontStyle() {
        local().clearFontStyle();
        remote().clearFontStyle();
    }

    @Override
    public void clearFontWeight() {
        local().clearFontWeight();
        remote().clearFontWeight();
    }

    @Override
    public void clearHeight() {
        local().clearHeight();
        remote().clearHeight();
    }

    @Override
    public void clearLeft() {
        local().clearLeft();
        remote().clearLeft();
    }

    @Override
    public void clearLineHeight() {
        local().clearLineHeight();
        remote().clearLineHeight();
    }

    @Override
    public void clearListStyleType() {
        local().clearListStyleType();
        remote().clearListStyleType();
    }

    @Override
    public void clearMargin() {
        local().clearMargin();
        remote().clearMargin();
    }

    @Override
    public void clearMarginBottom() {
        local().clearMarginBottom();
        remote().clearMarginBottom();
    }

    @Override
    public void clearMarginLeft() {
        local().clearMarginLeft();
        remote().clearMarginLeft();
    }

    @Override
    public void clearMarginRight() {
        local().clearMarginRight();
        remote().clearMarginRight();
    }

    @Override
    public void clearMarginTop() {
        local().clearMarginTop();
        remote().clearMarginTop();
    }

    @Override
    public void clearOpacity() {
        local().clearOpacity();
        remote().clearOpacity();
    }

    @Override
    public void clearOutlineColor() {
        local().clearOutlineColor();
        remote().clearOutlineColor();
    }

    @Override
    public void clearOutlineStyle() {
        local().clearOutlineStyle();
        remote().clearOutlineStyle();
    }

    @Override
    public void clearOutlineWidth() {
        local().clearOutlineWidth();
        remote().clearOutlineWidth();
    }

    @Override
    public void clearOverflow() {
        local().clearOverflow();
        remote().clearOverflow();
    }

    @Override
    public void clearOverflowX() {
        local().clearOverflowX();
        remote().clearOverflowX();
    }

    @Override
    public void clearOverflowY() {
        local().clearOverflowY();
        remote().clearOverflowY();
    }

    @Override
    public void clearPadding() {
        local().clearPadding();
        remote().clearPadding();
    }

    @Override
    public void clearPaddingBottom() {
        local().clearPaddingBottom();
        remote().clearPaddingBottom();
    }

    @Override
    public void clearPaddingLeft() {
        local().clearPaddingLeft();
        remote().clearPaddingLeft();
    }

    @Override
    public void clearPaddingRight() {
        local().clearPaddingRight();
        remote().clearPaddingRight();
    }

    @Override
    public void clearPaddingTop() {
        local().clearPaddingTop();
        remote().clearPaddingTop();
    }

    @Override
    public void clearPosition() {
        local().clearPosition();
        remote().clearPosition();
    }

    @Override
    public void clearProperty(String name) {
        local().clearProperty(name);
        remote().clearProperty(name);
    }

    @Override
    public void clearRight() {
        local().clearRight();
        remote().clearRight();
    }

    @Override
    public void clearTableLayout() {
        local().clearTableLayout();
        remote().clearTableLayout();
    }

    @Override
    public void clearTextAlign() {
        local().clearTextAlign();
        remote().clearTextAlign();
    }

    @Override
    public void clearTextDecoration() {
        local().clearTextDecoration();
        remote().clearTextDecoration();
    }

    @Override
    public void clearTextIndent() {
        local().clearTextIndent();
        remote().clearTextIndent();
    }

    @Override
    public void clearTextJustify() {
        local().clearTextJustify();
        remote().clearTextJustify();
    }

    @Override
    public void clearTextOverflow() {
        local().clearTextOverflow();
        remote().clearTextOverflow();
    }

    @Override
    public void clearTextTransform() {
        local().clearTextTransform();
        remote().clearTextTransform();
    }

    @Override
    public void clearTop() {
        local().clearTop();
        remote().clearTop();
    }

    @Override
    public void clearVisibility() {
        local().clearVisibility();
        remote().clearVisibility();
    }

    @Override
    public void clearWhiteSpace() {
        local().clearWhiteSpace();
        remote().clearWhiteSpace();
    }

    @Override
    public void clearWidth() {
        local().clearWidth();
        remote().clearWidth();
    }

    @Override
    public void clearZIndex() {
        local().clearZIndex();
        remote().clearZIndex();
    }

    public void cloneStyleFrom(DomStyle domStyle) {
        Style style = (Style) domStyle;
        local().cloneStyleFrom(style.local(), this);
    }

    @Override
    public String getBackgroundColor() {
        return local().getBackgroundColor();
    }

    @Override
    public String getBackgroundImage() {
        return local().getBackgroundImage();
    }

    @Override
    public String getBorderColor() {
        return local().getBorderColor();
    }

    @Override
    public String getBorderStyle() {
        return local().getBorderStyle();
    }

    @Override
    public String getBorderWidth() {
        return local().getBorderWidth();
    }

    @Override
    public String getBottom() {
        return local().getBottom();
    }

    @Override
    public String getClear() {
        return local().getClear();
    }

    @Override
    public String getColor() {
        return local().getColor();
    }

    @Override
    public String getCursor() {
        return local().getCursor();
    }

    @Override
    public String getDisplay() {
        return local().getDisplay();
    }

    @Override
    public String getFontSize() {
        return local().getFontSize();
    }

    @Override
    public String getFontStyle() {
        return local().getFontStyle();
    }

    @Override
    public String getFontWeight() {
        return local().getFontWeight();
    }

    @Override
    public String getHeight() {
        return local().getHeight();
    }

    @Override
    public String getLeft() {
        return local().getLeft();
    }

    @Override
    public String getLineHeight() {
        return local().getLineHeight();
    }

    @Override
    public String getListStyleType() {
        return local().getListStyleType();
    }

    @Override
    public String getMargin() {
        return local().getMargin();
    }

    @Override
    public String getMarginBottom() {
        return local().getMarginBottom();
    }

    @Override
    public String getMarginLeft() {
        return local().getMarginLeft();
    }

    @Override
    public String getMarginRight() {
        return local().getMarginRight();
    }

    @Override
    public String getMarginTop() {
        return local().getMarginTop();
    }

    @Override
    public String getOpacity() {
        return local().getOpacity();
    }

    @Override
    public String getOverflow() {
        return local().getOverflow();
    }

    @Override
    public String getOverflowX() {
        return local().getOverflowX();
    }

    @Override
    public String getOverflowY() {
        return local().getOverflowY();
    }

    @Override
    public String getPadding() {
        return local().getPadding();
    }

    @Override
    public String getPaddingBottom() {
        return local().getPaddingBottom();
    }

    @Override
    public String getPaddingLeft() {
        return local().getPaddingLeft();
    }

    @Override
    public String getPaddingRight() {
        return local().getPaddingRight();
    }

    @Override
    public String getPaddingTop() {
        return local().getPaddingTop();
    }

    @Override
    public String getPosition() {
        return local().getPosition();
    }

    @Override
    public Map<String, String> getProperties() {
        return local().getProperties();
    }

    @Override
    public String getProperty(String name) {
        return local().getProperty(name);
    }

    @Override
    public String getPropertyImpl(String name) {
        return local().getPropertyImpl(name);
    }

    @Override
    public String getRight() {
        return local().getRight();
    }

    @Override
    public String getTableLayout() {
        return local().getTableLayout();
    }

    @Override
    public String getTextAlign() {
        return local().getTextAlign();
    }

    @Override
    public String getTextDecoration() {
        return local().getTextDecoration();
    }

    @Override
    public String getTextIndent() {
        return local().getTextIndent();
    }

    @Override
    public String getTextJustify() {
        return local().getTextJustify();
    }

    @Override
    public String getTextOverflow() {
        return local().getTextOverflow();
    }

    @Override
    public String getTextTransform() {
        return local().getTextTransform();
    }

    @Override
    public String getTop() {
        return local().getTop();
    }

    @Override
    public String getVerticalAlign() {
        return local().getVerticalAlign();
    }

    @Override
    public String getVisibility() {
        return local().getVisibility();
    }

    @Override
    public String getWhiteSpace() {
        return local().getWhiteSpace();
    }

    @Override
    public String getWidth() {
        return local().getWidth();
    }

    @Override
    public String getZIndex() {
        return local().getZIndex();
    }

    public void removePropertyImpl(String name) {
        local().setProperty(name, "");
        remote().setProperty(name, "");
    }

    @Override
    public void setBackgroundColor(String value) {
        local().setBackgroundColor(value);
        remote().setBackgroundColor(value);
    }

    @Override
    public void setBackgroundImage(String value) {
        local().setBackgroundImage(value);
        remote().setBackgroundImage(value);
    }

    @Override
    public void setBorderColor(String value) {
        local().setBorderColor(value);
        remote().setBorderColor(value);
    }

    @Override
    public void setBorderStyle(BorderStyle value) {
        local().setBorderStyle(value);
        remote().setBorderStyle(value);
    }

    @Override
    public void setBorderWidth(double value, Unit unit) {
        local().setBorderWidth(value, unit);
        remote().setBorderWidth(value, unit);
    }

    @Override
    public void setBottom(double value, Unit unit) {
        local().setBottom(value, unit);
        remote().setBottom(value, unit);
    }

    @Override
    public void setClear(Clear value) {
        local().setClear(value);
        remote().setClear(value);
    }

    @Override
    public void setColor(String value) {
        local().setColor(value);
        remote().setColor(value);
    }

    @Override
    public void setCursor(Cursor value) {
        local().setCursor(value);
        remote().setCursor(value);
    }

    @Override
    public void setDisplay(Display value) {
        local().setDisplay(value);
        remote().setDisplay(value);
    }

    @Override
    public void setFloat(Float value) {
        local().setFloat(value);
        remote().setFloat(value);
    }

    @Override
    public void setFontSize(double value, Unit unit) {
        local().setFontSize(value, unit);
        remote().setFontSize(value, unit);
    }

    @Override
    public void setFontStyle(FontStyle value) {
        local().setFontStyle(value);
        remote().setFontStyle(value);
    }

    @Override
    public void setFontWeight(FontWeight value) {
        local().setFontWeight(value);
        remote().setFontWeight(value);
    }

    @Override
    public void setHeight(double value, Unit unit) {
        local().setHeight(value, unit);
        remote().setHeight(value, unit);
    }

    @Override
    public void setLeft(double value, Unit unit) {
        local().setLeft(value, unit);
        remote().setLeft(value, unit);
    }

    @Override
    public void setLineHeight(double value, Unit unit) {
        local().setLineHeight(value, unit);
        remote().setLineHeight(value, unit);
    }

    @Override
    public void setListStyleType(ListStyleType value) {
        local().setListStyleType(value);
        remote().setListStyleType(value);
    }

    @Override
    public void setMargin(double value, Unit unit) {
        local().setMargin(value, unit);
        remote().setMargin(value, unit);
    }

    @Override
    public void setMarginBottom(double value, Unit unit) {
        local().setMarginBottom(value, unit);
        remote().setMarginBottom(value, unit);
    }

    @Override
    public void setMarginLeft(double value, Unit unit) {
        local().setMarginLeft(value, unit);
        remote().setMarginLeft(value, unit);
    }

    @Override
    public void setMarginRight(double value, Unit unit) {
        local().setMarginRight(value, unit);
        remote().setMarginRight(value, unit);
    }

    @Override
    public void setMarginTop(double value, Unit unit) {
        local().setMarginTop(value, unit);
        remote().setMarginTop(value, unit);
    }

    @Override
    public void setOpacity(double value) {
        local().setOpacity(value);
        remote().setOpacity(value);
    }

    @Override
    public void setOutlineColor(String value) {
        local().setOutlineColor(value);
        remote().setOutlineColor(value);
    }

    @Override
    public void setOutlineStyle(OutlineStyle value) {
        local().setOutlineStyle(value);
        remote().setOutlineStyle(value);
    }

    @Override
    public void setOutlineWidth(double value, Unit unit) {
        local().setOutlineWidth(value, unit);
        remote().setOutlineWidth(value, unit);
    }

    @Override
    public void setOverflow(Overflow value) {
        local().setOverflow(value);
        remote().setOverflow(value);
    }

    @Override
    public void setOverflowX(Overflow value) {
        local().setOverflowX(value);
        remote().setOverflowX(value);
    }

    @Override
    public void setOverflowY(Overflow value) {
        local().setOverflowY(value);
        remote().setOverflowY(value);
    }

    @Override
    public void setPadding(double value, Unit unit) {
        local().setPadding(value, unit);
        remote().setPadding(value, unit);
    }

    @Override
    public void setPaddingBottom(double value, Unit unit) {
        local().setPaddingBottom(value, unit);
        remote().setPaddingBottom(value, unit);
    }

    @Override
    public void setPaddingLeft(double value, Unit unit) {
        local().setPaddingLeft(value, unit);
        remote().setPaddingLeft(value, unit);
    }

    @Override
    public void setPaddingRight(double value, Unit unit) {
        local().setPaddingRight(value, unit);
        remote().setPaddingRight(value, unit);
    }

    @Override
    public void setPaddingTop(double value, Unit unit) {
        local().setPaddingTop(value, unit);
        remote().setPaddingTop(value, unit);
    }

    @Override
    public void setPosition(Position value) {
        local().setPosition(value);
        remote().setPosition(value);
    }

    @Override
    public void setProperty(String name, double value, Unit unit) {
        local().setProperty(name, value, unit);
        remote().setProperty(name, value, unit);
    }

    @Override
    public void setProperty(String name, String value) {
        if (name.equals("display") && element.linkedToRemote()) {
            LocalDom.log(LocalDomDebug.STYLE, "%s %s : %s", element,
                    Optional.ofNullable(element.uiObject).map(
                            uiObject -> uiObject.getClass().getSimpleName())
                            .orElse("(null)"),
                    value);
        }
        String current = local().getProperty(name);
        if (Objects.equals(current, value)) {
        } else {
            local().setProperty(name, value);
            remote().setProperty(name, value);
        }
    }

    @Override
    public void setPropertyImpl(String name, String value) {
        String current = local().getProperty(name);
        if (Objects.equals(current, value)) {
        } else {
            local().setPropertyImpl(name, value);
            remote().setPropertyImpl(name, value);
        }
    }

    @Override
    public void setPropertyPx(String name, int value) {
        String current = local().getProperty(name);
        if (Objects.equals(current, String.valueOf(value))) {
        } else {
            local().setPropertyPx(name, value);
            remote().setPropertyPx(name, value);
        }
        local().setPropertyPx(name, value);
        remote().setPropertyPx(name, value);
    }

    @Override
    public void setRight(double value, Unit unit) {
        local().setRight(value, unit);
        remote().setRight(value, unit);
    }

    @Override
    public void setTableLayout(TableLayout value) {
        local().setTableLayout(value);
        remote().setTableLayout(value);
    }

    @Override
    public void setTextAlign(TextAlign value) {
        local().setTextAlign(value);
        remote().setTextAlign(value);
    }

    @Override
    public void setTextDecoration(TextDecoration value) {
        local().setTextDecoration(value);
        remote().setTextDecoration(value);
    }

    @Override
    public void setTextIndent(double value, Unit unit) {
        local().setTextIndent(value, unit);
        remote().setTextIndent(value, unit);
    }

    @Override
    public void setTextJustify(TextJustify value) {
        local().setTextJustify(value);
        remote().setTextJustify(value);
    }

    @Override
    public void setTextOverflow(TextOverflow value) {
        local().setTextOverflow(value);
        remote().setTextOverflow(value);
    }

    @Override
    public void setTextTransform(TextTransform value) {
        local().setTextTransform(value);
        remote().setTextTransform(value);
    }

    @Override
    public void setTop(double value, Unit unit) {
        local().setTop(value, unit);
        remote().setTop(value, unit);
    }

    @Override
    public void setVerticalAlign(double value, Unit unit) {
        local().setVerticalAlign(value, unit);
        remote().setVerticalAlign(value, unit);
    }

    @Override
    public void setVerticalAlign(VerticalAlign value) {
        local().setVerticalAlign(value);
        remote().setVerticalAlign(value);
    }

    @Override
    public void setVisibility(Visibility value) {
        local().setVisibility(value);
        remote().setVisibility(value);
    }

    @Override
    public void setWhiteSpace(WhiteSpace value) {
        local().setWhiteSpace(value);
        remote().setWhiteSpace(value);
    }

    @Override
    public void setWidth(double value, Unit unit) {
        local().setWidth(value, unit);
        remote().setWidth(value, unit);
    }

    @Override
    public void setZIndex(int value) {
        local().setZIndex(value);
        remote().setZIndex(value);
    }

    @Override
    public Style styleObject() {
        // return local().styleObject();
        throw new UnsupportedOperationException();// do we need this?
    }

    protected StyleLocal local() {
        return local;
    }

    protected DomStyle remote() {
        element.ensureRemoteCheck();
        if (!linkedToRemote() && element.linkedToRemote()) {
            remote = element.typedRemote().getStyleRemote();
        }
        return remote;
    }

    void detachFromRemote() {
        remote = StyleNull.INSTANCE;
    }

    boolean linkedToRemote() {
        return remote != StyleNull.INSTANCE;
    }

    void resetRemote() {
        remote = StyleNull.INSTANCE;
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
     * Interface to be implemented by enumerated CSS values.
     */
    public interface HasCssName {
        /**
         * Gets the CSS name associated with this value.
         */
        String getCssName();
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
}
