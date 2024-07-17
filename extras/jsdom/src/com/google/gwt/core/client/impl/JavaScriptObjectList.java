package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;

/**
 * <p>
 * This class acts as a container for NodeJso, to provide performant syncing of
 * a Local Dom to a Browser Dom tree
 * 
 * <p>
 * In scripted mode, the iterator is backed by the jsArray, in hosted mode by
 * the javaArray field
 * <p>
 * This class must be passed by reference (from Java to Js) - and then has
 * particular devmode protocol types to support bulk serialization
 */
public final class JavaScriptObjectList {
	public JavaScriptObject[] javaArray = new JavaScriptObject[0];

	public JsArrayMixed jsArray;
}
