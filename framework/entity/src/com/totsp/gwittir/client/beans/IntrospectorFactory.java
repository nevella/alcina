/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.totsp.gwittir.client.beans;

import cc.alcina.framework.common.client.logic.reflection.ReflectionConstants;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.internal.JVMIntrospector;

/**
 *
 * @author kebernet
 */
public class IntrospectorFactory {


    private IntrospectorFactory(){

    };


    public static Introspector create(){
        if(GWT.isScript()){
            GWT.log("Using generated introspector.", null);
            System.out.println("Using generated introspector");
            return GWT.create(Introspector.class);
        } else {
        	GWT.log("Using JVM introspector", null);
            System.out.println("Using JVMIntrospector");
            return new JVMIntrospector();
        }
    }
}
