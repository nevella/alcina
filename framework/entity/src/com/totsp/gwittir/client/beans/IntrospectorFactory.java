/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.totsp.gwittir.client.beans;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.internal.JVMIntrospector;

/**
 *
 * @author kebernet
 */
public class IntrospectorFactory {
	public static Introspector create() {
		if (GWT.isScript()) {
			GWT.log("Using generated introspector.", null);
			System.out.println("Using generated introspector");
			return GWT.create(Introspector.class);
		} else {
			GWT.log("Using jvm introspector", null);
			System.out.println("Using jvm introspector");
			return new JVMIntrospector();
		}
	};

	private IntrospectorFactory() {
	}
}
