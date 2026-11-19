package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

public class Html5File extends JavaScriptObject {
	protected Html5File() {
	}

	public final native String getFileName() /*-{
    if (this.name) {
      return this.name;
    } else {
      return this.fileName;
    }
	}-*/;

	public final native int getFileSize() /*-{
    return this.fileSize;
	}-*/;
}