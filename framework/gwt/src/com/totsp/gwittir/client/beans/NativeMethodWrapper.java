package com.totsp.gwittir.client.beans;

import com.google.gwt.core.client.JavaScriptObject;
import com.totsp.gwittir.client.beans.Method;
import com.totsp.gwittir.client.beans.TreeIntrospector;

public class NativeMethodWrapper implements Method {
	private String name;

	private Class declaringClass;

	private JavaScriptObject nativeMethod;

	public NativeMethodWrapper(Class declaringClass, String name,
			TreeIntrospector introspector) {
		this.declaringClass = declaringClass;
		this.name = name;
		this.nativeMethod = introspector.getNativeMethod(declaringClass, name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object invoke(Object target, Object[] args) throws Exception {
		return invoke0(target, args == null || args.length == 0 ? null
				: args[0]);
	}

	private native Object invoke0(Object target, Object arg) throws Exception /*-{
		var nativeMethod = this.@com.totsp.gwittir.client.beans.NativeMethodWrapper::nativeMethod;
		return nativeMethod(target, arg);
	}-*/;
}
