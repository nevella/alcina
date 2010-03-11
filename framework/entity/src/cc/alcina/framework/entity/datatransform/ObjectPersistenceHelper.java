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

package cc.alcina.framework.entity.datatransform;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.util.CurrentUtcDateProvider;
import cc.alcina.framework.gwt.client.gwittir.HasGeneratedDisplayName;


/**
 * a fair bit of overlap with tltm - should clean up
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public class ObjectPersistenceHelper implements ClassLookup, ObjectLookup,
		PropertyAccessor, CurrentUtcDateProvider {
	// Initialises this. Note - not a thread-specific singleton,
	// any thread (client) specific work delegated to tltm
	private ObjectPersistenceHelper() {
		super();
		TransformManager.register(ThreadlocalTransformManager.ttmInstance());
		CommonLocator.get().registerClassLookup(this);
		CommonLocator.get().registerObjectLookup(this);
		CommonLocator.get().registerPropertyAccessor(this);
	}

	private static ObjectPersistenceHelper theInstance;

	public static ObjectPersistenceHelper get() {
		if (theInstance == null) {
			theInstance = new ObjectPersistenceHelper();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public Class getClassForName(String fqn) {
		try {
			return Class.forName(fqn);
		} catch (ClassNotFoundException e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		// uses thread-local instance
		return TransformManager.get().getObject(c, id, localId);
	}

	public void setPropertyValue(Object bean, String propertyName, Object value) {
		((ThreadlocalTransformManager) TransformManager.get())
				.setPropertyValue(bean, propertyName, value);
	}

	public <T extends HasIdAndLocalId> T  getObject(T bean) {
		return (T) TransformManager.get().getObject(
				bean.getClass(), bean.getId(), bean.getLocalId());
	}

	public void deregisterObject(HasIdAndLocalId bean) {
		TransformManager.get().deregisterObject(bean);
	}

	public Object getPropertyValue(Object bean, String propertyName) {
		return ((ThreadlocalTransformManager) TransformManager.get())
				.getPropertyValue(bean, propertyName);
	}

	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(targetClass)
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getName().equals(propertyName)) {
					return pd.getReadMethod().getAnnotation(annotationClass);
				}
			}
			return null;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <A extends Annotation> A getAnnotationForClass(Class targetClass,
			Class<A> annotationClass) {
		return (A) targetClass.getAnnotation(annotationClass);
	}

	public String displayNameForObject(Object o) {
		if (o instanceof HasGeneratedDisplayName) {
			return ((HasGeneratedDisplayName) o).generatedDisplayName();
		}
		String dnpn = "id";
		BeanInfo info = o.getClass().getAnnotation(BeanInfo.class);
		if (info != null) {
			dnpn = info.displayNamePropertyName();
		}
		Object pv = CommonLocator.get().propertyAccessor().getPropertyValue(
				o, dnpn);
		return (pv == null) ? "---" : pv.toString();
	}

	public List<String> getAnnotatedPropertyNames(Class clazz) {
		try {
			List<String> result = new ArrayList<String>();
			PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null
						&& pd.getWriteMethod() != null
						&& pd.getReadMethod().getAnnotation(
								VisualiserInfo.class) != null) {
					result.add(pd.getName());
				}
			}
			return result;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public Class getPropertyType(Class clazz, String propertyName) {
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getName().equals(propertyName)) {
					return pd.getPropertyType();
				}
			}
			return null;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <T> T newInstance(Class<T> clazz, long localId) {
		return ((ThreadlocalTransformManager) TransformManager.get())
				.newInstance(clazz, localId);
	}

	protected Enum getTargetEnumValue(DataTransformEvent evt) {
		if (Enum.class.isAssignableFrom(evt.getValueClass())) {
			return Enum.valueOf(evt.getValueClass(), evt.getNewStringValue());
		}
		return null;
	}

	public <T> T newInstance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <T> T getTemplateInstance(Class<T> clazz) {
		return newInstance(clazz);
	}

	@SuppressWarnings("deprecation")
	// it works...yeah, calendar shmalendar
	public Date currentUtcDate() {
		Date d = new Date();
		return new Date(d.getTime() + d.getTimezoneOffset() * 60 * 1000);
	}

	public List<PropertyInfoLite> getWritableProperties(Class clazz) {
		try {
			List<PropertyInfoLite> infos = new ArrayList<PropertyInfoLite>();
			java.beans.BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				Class<?> propertyType = pd.getPropertyType();
				if (pd.getWriteMethod()==null){
					continue;
				}
				if (propertyType.isInterface()&&propertyType!=Set.class) {
					// this seems to vary (unnecessary on 1.5, necessary on
					// 1.6)-propertydescriptor change probly
					propertyType = CommonLocator.get()
							.implementationLookup().getImplementation(
									propertyType);
				}
				infos.add(new PropertyInfoLite(propertyType, pd
						.getName(),null,clazz));
			}
			return infos;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		
	}
}
