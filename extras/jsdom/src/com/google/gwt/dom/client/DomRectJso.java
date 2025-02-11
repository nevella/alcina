package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

public final class DomRectJso extends JavaScriptObject {
	protected DomRectJso() {
	}

	public native double getBottom() /*-{
return this.bottom;
}-*/;

	public native double getHeight() /*-{
return this.height;
}-*/;

	public native double getLeft() /*-{
return this.left;
}-*/;

	public native double getRight() /*-{
return this.right;
}-*/;

	public native double getTop() /*-{
return this.top;
}-*/;

	public native double getWidth() /*-{
return this.width;
}-*/;

	public native double getX() /*-{
return this.x;
}-*/;

	public native double getY() /*-{
return this.y;
}-*/;
}