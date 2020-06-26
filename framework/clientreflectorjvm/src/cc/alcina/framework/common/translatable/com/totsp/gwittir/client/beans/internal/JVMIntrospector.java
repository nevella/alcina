/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.totsp.gwittir.client.beans.internal;

import com.google.gwt.core.client.GwtScriptOnly;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Introspector;

import cc.alcina.framework.gwt.client.service.BeanDescriptorProvider;

/**
 * never actually used, but means we don't have to do weird things for
 * hosted-mode version
 */
@GwtScriptOnly
public class JVMIntrospector implements Introspector, BeanDescriptorProvider {
    @Override
    public BeanDescriptor getDescriptor(Object object) {
        
        return null;
    }

    @Override
    public Class resolveClass(Object instance) {
        
        return null;
    }
}
