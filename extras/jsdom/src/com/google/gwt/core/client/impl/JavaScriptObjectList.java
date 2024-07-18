package com.google.gwt.core.client.impl;

import java.util.Arrays;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
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
 * <p>
 * Java code should only access the javaArray (to avoid round-tripping); js code
 * only the jsArray
 */
public final class JavaScriptObjectList {
	public JavaScriptObject[] javaArray = new JavaScriptObject[0];

	public JsArrayMixed jsArray;

	/*
	 * js-side, ensures the existence of the jsarray (and initialises it with
	 * javaArray values if they don't exist). This code involves an array copy
	 * in GWT_SCRIPT mode - but there's no protocol round-tripping involved, so
	 * the cost is minimal
	 */
	public final native JsArrayInteger ensureJsArray()/*-{
	//devmode
		if(this.hasOwnProperty("__gwt_java_js_object_list")){
			return this.__gwt_java_js_object_list;
		}
		//this should only be called once, with a null jsArray
		var arr = this.@com.google.gwt.core.client.impl.JavaScriptObjectList::jsArray;
		if(!!arr){
			throw Error("jsArray must be null");
		}
		arr = [];
		this.@com.google.gwt.core.client.impl.JavaScriptObjectList::jsArray=arr;
		this.@com.google.gwt.core.client.impl.JavaScriptObjectList::copyToArray()();
		return arr;

		
	}-*/;

	void copyToArray() {
		Arrays.stream(javaArray).forEach(jsArray::push);
	}
}
