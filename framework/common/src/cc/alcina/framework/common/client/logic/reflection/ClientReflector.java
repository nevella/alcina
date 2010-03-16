/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

/**
 *
 * @author Nick Reddel
 */

 public class ClientReflector implements ClassLookup {
	private static ClientReflector domainReflector;

	public static boolean defined() {
		return domainReflector != null;
	}

	public static ClientReflector get() {
		if (domainReflector == null) {
			throw new RuntimeException("Domain reflector not defined");
		}
		return domainReflector;
	}

	public static void register(ClientReflector r) {
		domainReflector = r;
	}

	protected Map<Class, ClientBeanReflector> gwbiMap = new HashMap<Class, ClientBeanReflector>();

	protected ClassLookup child = null;

	private Map<Class, Object> templateInstances = new HashMap<Class, Object>();

	public ClientReflector() {
	}

	public ClientBeanReflector beanInfoForClass(Class clazz) {
		throw new RuntimeException("Operation not implemented");
	}

	public String displayNameForObject(Object o) {
		ClientBeanReflector bi = beanInfoForClass(o.getClass());
		return (bi == null) ? null : bi.getObjectName(o);
	}

	public List<String> getAnnotatedPropertyNames(Class clazz) {
		ClientBeanReflector bi = beanInfoForClass(clazz);
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		ArrayList<String> result = new ArrayList<String>();
		for (ClientPropertyReflector pr : prs) {
			if (pr.getGwPropertyInfo() != null) {
				result.add(pr.getPropertyName());
			}
		}
		return result;
	}

	public <A extends Annotation> A getAnnotationForClass(Class targetClass,
			Class<A> annotationClass) {
		ClientBeanReflector beanInfo = ClientReflector.get().beanInfoForClass(
				targetClass);
		return beanInfo == null ? null : beanInfo
				.getAnnotation(annotationClass);
	}

	public Class getClassForName(String fqn) {
		Class clazz = forNameMap.get(fqn);
		if (clazz!=null){
			return clazz;
		}
		throw new RuntimeException("Class "+fqn+" not reflect-instantiable");
	}

	public Class getPropertyType(Class clazz, String propertyName) {
		return GwittirBridge.get().getPropertyForClass(clazz, propertyName)
				.getType();
	}
	@SuppressWarnings("unchecked")
	public <T> T getTemplateInstance(Class<T> clazz) {
		if (!templateInstances.containsKey(clazz)) {
			templateInstances.put(clazz, CommonLocator.get().classLookup()
					.newInstance(clazz, 0));
		}
		return (T) templateInstances.get(clazz);
	}

	public boolean isInstantiableClass(Class clazz) {
		try {
			if (stdAndPrimitives.contains(clazz)){
				return false;
			}
			getClassForName(clazz.getName());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean isStandardJavaClass(Class clazz) {
		return stdAndPrimitivesMap.containsValue(clazz);
	}

	public <T> T newInstance(Class<T> clazz) {
		return newInstance(clazz, 0);
	}

	public <T> T newInstance(Class<T> clazz, long localId) {
		throw new RuntimeException("Operation not implemented");
	}

	public void registerWithParent(ClientReflector parent) {
		parent.child = this;
		for (Class c : gwbiMap.keySet()) {
			parent.gwbiMap.put(c, gwbiMap.get(c));
		}
	}

	protected Map<String,Class> stdClassMap = new HashMap<String, Class>();
	{
		Class[] stds = {Long.class,Double.class,Float.class,Short.class,Byte.class,Integer.class,Boolean.class,Character.class,Date.class,String.class};
		for (Class std : stds) {
			stdClassMap.put(std.getName(), std);
		}
	}
	protected Map<String,Class> primitiveClassMap = new HashMap<String, Class>();
	{
		Class[] prims = {long.class,int.class,short.class,char.class,byte.class,boolean.class,double.class,float.class};
		for (Class prim : prims) {
			primitiveClassMap.put(prim.getName(), prim);
		}
	}
	protected Map<String,Class> stdAndPrimitivesMap = new HashMap<String, Class>();
	{
		stdAndPrimitivesMap.putAll(stdClassMap);
		stdAndPrimitivesMap.putAll(primitiveClassMap);
	}
	protected Set<Class> stdAndPrimitives = new HashSet< Class>(stdAndPrimitivesMap.values());
	protected Map<String,Class> forNameMap = new HashMap<String, Class>();
	{
		forNameMap.putAll(stdAndPrimitivesMap);
	}

	public List<PropertyInfoLite> getWritableProperties(Class clazz) {
		BeanDescriptor descriptor = GwittirBridge.get().getDescriptorForClass(
				clazz);
		List<PropertyInfoLite> infos = new ArrayList<PropertyInfoLite>();
		for (Property p : descriptor.getProperties()) {
			if (p.getMutatorMethod()==null){
				continue;
			}
			infos.add(new PropertyInfoLite(p.getType(), p.getName(), p
					.getAccessorMethod(),clazz));
		}
		return infos;
	}
}
