package com.google.gwt.core.client;

public interface CastableFromJavascriptObject {
	public <T extends CastableFromJavascriptObject> T cast();
}
