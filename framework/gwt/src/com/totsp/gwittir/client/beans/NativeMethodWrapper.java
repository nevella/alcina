package com.totsp.gwittir.client.beans;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

public class NativeMethodWrapper implements Method {
	private String name;

	private Class declaringClass;

	private JavaScriptObject nativeMethod;

	private Class methodReturnType;

	public NativeMethodWrapper(Class declaringClass, String name,
			Class methodReturnType, TreeIntrospector introspector) {
		this.declaringClass = declaringClass;
		this.name = name;
		this.methodReturnType = methodReturnType;
		this.nativeMethod = introspector.getNativeMethod(declaringClass, name);
		if (this.nativeMethod == null) {
			throw new RuntimeException("native method is null");
		}
	}

	@Override
	public Class getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object invoke(Object target, Object[] args) throws Exception {
		if (GWT.isScript()) {
			return invoke0(target,
					args == null || args.length == 0 ? null : args[0]);
		} else {
			if (methodReturnType == boolean.class) {
				return invoke0RetBoolean(target, null);
			} else if (methodReturnType == double.class) {
				return invoke0RetDouble(target, null);
			} else {
				if (args == null || args.length == 0) {
					return invoke0(target, null);
				} else {
					Object arg = args[0];
					if (arg != null && (arg.getClass() == boolean.class
							|| arg.getClass() == Boolean.class)) {
						return invoke0ArgBoolean(target, (boolean) arg);
					} else if (arg != null && (arg.getClass() == double.class
							|| arg.getClass() == Double.class)) {
						return invoke0ArgDouble(target, (double) arg);
					} else {
						return invoke0(target, arg);
					}
				}
			}
		}
	}

	private native Object invoke0(Object target, Object arg) throws Exception /*-{
    var nativeMethod = this.@com.totsp.gwittir.client.beans.NativeMethodWrapper::nativeMethod;
    return nativeMethod(target, arg);
	}-*/;

	private native Object invoke0ArgBoolean(Object target, boolean arg)
			throws Exception /*-{
    var nativeMethod = this.@com.totsp.gwittir.client.beans.NativeMethodWrapper::nativeMethod;
    return nativeMethod(target, arg);
	}-*/;

	private native Object invoke0ArgDouble(Object target, double arg)
			throws Exception /*-{
    var nativeMethod = this.@com.totsp.gwittir.client.beans.NativeMethodWrapper::nativeMethod;
    return nativeMethod(target, arg);
	}-*/;

	private native int invoke0Int(Object target, Object arg) throws Exception /*-{
    var nativeMethod = this.@com.totsp.gwittir.client.beans.NativeMethodWrapper::nativeMethod;
    return nativeMethod(target, arg);
	}-*/;

	private native boolean invoke0RetBoolean(Object target, Object arg)
			throws Exception /*-{
    var nativeMethod = this.@com.totsp.gwittir.client.beans.NativeMethodWrapper::nativeMethod;
    return nativeMethod(target, arg);
	}-*/;

	private native double invoke0RetDouble(Object target, Object arg)
			throws Exception /*-{
    var nativeMethod = this.@com.totsp.gwittir.client.beans.NativeMethodWrapper::nativeMethod;
    return nativeMethod(target, arg);
	}-*/;
}
