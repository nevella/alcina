package com.google.gwt.core.client;

/*
 * Either a javascript object, or has a permanent 1-1 correspondence with a js object
 * 
 */
public interface JavascriptObjectEquivalent {
	<T extends JavascriptObjectEquivalent> T cast();
}
