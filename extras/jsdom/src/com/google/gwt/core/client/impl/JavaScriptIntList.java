package com.google.gwt.core.client.impl;

import java.util.Arrays;

import com.google.gwt.core.client.JsArrayInteger;

/**
 * <p>
 * As per {@link JavaScriptObjectList}, but for ints. See that class's doc
 */
public final class JavaScriptIntList {
	public int[] javaArray = new int[0];

	public JsArrayInteger jsArray;

	/*
	 * js-side, ensures the existence of the jsarray (and initialises it with
	 * javaArray values if they don't exist). This code involves an array copy
	 * in GWT_SCRIPT mode - but there's no protocol round-tripping involved, so
	 * the cost is minimal
	 */
	public final native JsArrayInteger ensureJsArray()/*-{
	//devmode
		if(this.hasOwnProperty("__gwt_java_js_int_list")){
			return this.__gwt_java_js_int_list;
		}
		//this should only be called once, with a null jsArray
		var arr = this.@com.google.gwt.core.client.impl.JavaScriptIntList::jsArray;
		if(!!arr){
			throw Error("jsArray must be null");
		}
		arr = [];
		this.@com.google.gwt.core.client.impl.JavaScriptIntList::jsArray=arr;
		this.@com.google.gwt.core.client.impl.JavaScriptIntList::copyToArray()();
		return arr;

		
	}-*/;

	void copyToArray() {
		Arrays.stream(javaArray).forEach(jsArray::push);
	}
}
