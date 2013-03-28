package cc.alcina.framework.common.client.logic.reflection.jvm;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.AnnotationUtils;
import cc.alcina.framework.entity.util.ClasspathScanner;

import com.totsp.gwittir.client.beans.annotations.Omit;
import com.totsp.gwittir.client.beans.internal.JVMIntrospector.MethodWrapper;

/**
 *
 */
public class ClientReflectorJvm extends ClientReflector {
	Map<Class, ClientBeanReflector> reflectors = new HashMap<Class, ClientBeanReflector>();

	public static final String PROP_FILTER_CLASSNAME = "ClientReflectorJvm.filterClassName";

	public ClientReflectorJvm() {
		
	}

	public ClientBeanReflector beanInfoForClass(Class clazz) {
		return null;
	}

	

	public Class getClassForName(String fqn) {
		return null;
	}

	@Override
	public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
		return  null;
	}

	public List<PropertyInfoLite> getWritableProperties(Class clazz) {
		return null;
	}

	@Override
	protected <T> T newInstance0(Class<T> clazz, long objectId, long localId) {
		// not called
		return null;
	}
}