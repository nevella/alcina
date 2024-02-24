package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

public class DomRectJso extends JavaScriptObject {
	protected DomRectJso() {
	}

	public final native double getBottom() /*-{
return this.bottom;
}-*/;

	public final native double getHeight() /*-{
return this.height;
}-*/;

	public final native double getLeft() /*-{
return this.left;
}-*/;

	public final native double getRight() /*-{
return this.right;
}-*/;

	public final native double getTop() /*-{
return this.top;
}-*/;

	public final native double getWidth() /*-{
return this.width;
}-*/;

	public final native double getX() /*-{
return this.x;
}-*/;

	public final native double getY() /*-{
return this.y;
}-*/;
}