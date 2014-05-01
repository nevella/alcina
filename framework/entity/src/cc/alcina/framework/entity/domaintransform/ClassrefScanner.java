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
package cc.alcina.framework.entity.domaintransform;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.registry.CachingScanner;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class ClassrefScanner extends CachingScanner {
	private LinkedHashSet<Class> persistableClasses;

	public void scan(Map<String, Date> classes) throws Exception {
		String cachePath = getHomeDir().getPath() + File.separator
				+ getClass().getSimpleName() + "-cache.ser";
		persistableClasses = new LinkedHashSet<Class>();
		persistableClasses.addAll(Arrays.asList(new Class[] { Long.class,
				Double.class, Float.class, Integer.class, Short.class,
				String.class, Date.class, Boolean.class }));
		scan(classes, cachePath);
		finish();
	}

	private void finish() throws Exception {
		if (!persistent) { 
			CommonPersistenceLocal cp = Registry.impl(
					CommonPersistenceProvider.class)
					.getCommonPersistenceExTransaction();
			Class<? extends ClassRef> crimpl = cp
					.getImplementation(ClassRef.class);
			long idCtr=0;
			for (Class clazz : persistableClasses) {
				ClassRef ref = ClassRef.forClass(clazz);
				ref = crimpl.newInstance();
				ref.setRefClass(clazz);
				ref.setId(++idCtr);
				ClassRef.add(CommonUtils.wrapInCollection(ref));
			}
			return;
		}
		CommonPersistenceLocal cp = Registry.impl(
				CommonPersistenceProvider.class).getCommonPersistence();
		Class<? extends ClassRef> crimpl = cp.getImplementation(ClassRef.class);
		Set<? extends ClassRef> classrefs = cp.getAll(crimpl);
		Set<? extends ClassRef> deleteClassrefs = new HashSet<ClassRef>();
		ClassRef.add(classrefs);
		((Set) deleteClassrefs).addAll(classrefs);
		classrefs.clear();
		boolean delta = false;
		for (Class clazz : persistableClasses) {
			ClassRef ref = ClassRef.forClass(clazz);
			if (ref == null) {
				delta = true;
				ref = crimpl.newInstance();
				ref.setRefClass(clazz);
				long id = cp.merge(ref);
				ref.setId(id);
				ClassRef.add(CommonUtils.wrapInCollection(ref));
				System.out.format("adding classref - %s %s\n", ref.getId(),
						ref.getRefClassName());
			} else {
				deleteClassrefs.remove(ref);
			}
		}
		for (ClassRef ref : deleteClassrefs) {
			delta = true;
			System.out.format("removing classref - %s %s\n", ref.getId(),
					ref.getRefClassName());
			cp.remove(ref);
			ClassRef.remove(ref);
		}
	}

	@Override
	protected void process(Class c, String className, Date modDate,
			Map<String, Date> outgoingIgnoreMap) {
		if ((!Modifier.isPublic(c.getModifiers()))
				|| (Modifier.isAbstract(c.getModifiers()) && !c.isEnum())) {
			outgoingIgnoreMap.put(className, modDate);
			return;
		}
		/*
		 * why are these required? (answer: makes sure they're in the gwt code -
		 * otherwise could be sending classes not compiled into client code)
		 */
		boolean bi = c.isAnnotationPresent(BeanInfo.class);
		boolean in = c.isAnnotationPresent(ClientInstantiable.class);
		boolean dtp = c.isAnnotationPresent(DomainTransformPersistable.class);
		if ((HasIdAndLocalId.class.isAssignableFrom(c) && (in || bi || dtp))
				|| (c.isEnum() && (in || dtp))) {
			persistableClasses.add(c);
		} else {
			outgoingIgnoreMap.put(className, modDate);
		}
	}

	// persistableClasses.addAll(Arrays.asList(new Class[] { long.class,
	// double.class, float.class, int.class, short.class,
	// boolean.class }));
	public void fixEntities(Class entityClass, String strPropName,
			String crPropName, EntityManager em,
			CommonPersistenceLocal persister) {
		Set all = persister.getAll(entityClass);
		for (Object o : all) {
			String cName = (String) SEUtilities
					.getPropertyValue(o, strPropName);
			if (cName == null) {
				continue;
			}
			ClassRef ref = ClassRef.forName(cName);
			if (ref == null) {
				throw new WrappedRuntimeException(
						"Classref not found:" + cName,
						SuggestedAction.NOTIFY_WARNING);
			}
			SEUtilities.setPropertyValue(o, crPropName, ref);
		}
		em.flush();
	}

	boolean persistent = true;

	public ClassrefScanner noPersistence() {
		persistent = false;
		return this;
	}
}
