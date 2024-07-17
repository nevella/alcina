package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.JsArrayInteger;

/**
 * <p>
 * This class acts as a container for ints, to provide performant syncing of a
 * Local Dom to a Browser Dom tree. This *could* also be done via json
 * serialization - but since we're doing JavascriptObjectList anywaysss....
 * 
 * <p>
 * In scripted mode, the iterator is backed by the jsArray, in hosted mode by
 * the javaArray field
 * <p>
 * This class must be passed by reference (from Java to Js) - and then has
 * particular devmode protocol types to support bulk serialization
 */
public final class JavaScriptIntList {
	public int[] javaArray = new int[0];

	public JsArrayInteger jsArray;
}
