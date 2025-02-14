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
package cc.alcina.framework.entity.transform;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.NonDomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.domain.DomainStoreDbColumn;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.registry.CachingScanner;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.transform.ClassrefScanner.ClassrefScannerMetadata;

/**
 * @author Nick Reddel
 */
public class ClassrefScanner extends CachingScanner<ClassrefScannerMetadata> {
	private LinkedHashSet<Class> persistableClasses;

	private long roIdCounter = 0;

	boolean persistent = true;

	boolean reachabilityCheck = true;

	Logger logger = LoggerFactory.getLogger(getClass());

	private void checkReachability() {
		Set<ClassRef> all = ClassRef.all();
		Set<String> errClassRef = new TreeSet<String>();
		Set<String> errAssociation = new TreeSet<String>();
		Set<Class> reffed = new LinkedHashSet<Class>();
		for (ClassRef ref : all) {
			reffed.add(ref.getRefClass());
		}
		for (Class ref : new ArrayList<Class>(reffed)) {
			if (!Entity.class.isAssignableFrom(ref)) {
				continue;
			}
			Class c = ref;
			while (c != Object.class) {
				Field[] fields = c.getDeclaredFields();
				for (Field field : fields) {
					int m = field.getModifiers();
					if (Modifier.isFinal(m) || Modifier.isStatic(m)
							|| Modifier.isTransient(m)) {
						continue;
					}
					Class type = field.getType();
					Class checkType = null;
					if (GraphProjection.isPrimitiveOrDataClass(type)) {
						if (ClassUtil.isEnumOrEnumSubclass(type)) {
							checkType = type;
						}
					} else if (Entity.class.isAssignableFrom(type)) {
						checkType = type;
					} else if (GraphProjection.isGenericEntityType(field)) {
						Type pt = GraphProjection.getGenericType(field);
						if (pt instanceof ParameterizedType) {
							Type genericType = ((ParameterizedType) pt)
									.getActualTypeArguments()[0];
							if (genericType instanceof Class) {
								checkType = (Class) genericType;
							}
						}
					}
					if (checkType != null && !reffed.contains(checkType)) {
						if (Modifier.isAbstract(checkType.getModifiers())) {
							for (Class clazz : new ArrayList<Class>(reffed)) {
								if (checkType.isAssignableFrom(clazz)) {
									reffed.add(checkType);
								}
							}
						}
					}
					if (checkType != null && !reffed.contains(checkType)) {
						errClassRef.add(String.format("%-30s: %s.%s",
								checkType.getSimpleName(), ref.getSimpleName(),
								field.getName()));
					}
					PropertyDescriptor leftPd = SEUtilities
							.getPropertyDescriptorByName(ref, field.getName());
					String methodName = String.format("%s.%s",
							ref.getSimpleName(), field.getName());
					if (leftPd != null && leftPd.getReadMethod() != null
							&& leftPd.getReadMethod()
									.getAnnotation(Association.class) != null) {
						Association left = leftPd.getReadMethod()
								.getAnnotation(Association.class);
						PropertyDescriptor rightPd = SEUtilities
								.getPropertyDescriptorByName(
										left.implementationClass(),
										left.propertyName());
						Association right = null;
						DomainStoreDbColumn rightMcc = null;
						if (rightPd != null && rightPd.getReadMethod() != null
								&& rightPd.getReadMethod().getAnnotation(
										Association.class) != null) {
							right = rightPd.getReadMethod()
									.getAnnotation(Association.class);
							if (right.implementationClass() != ref || !right
									.propertyName().equals(field.getName())) {
								right = null;
							}
						}
						if (rightPd != null && rightPd.getReadMethod() != null
								&& rightPd.getReadMethod().getAnnotation(
										DomainStoreDbColumn.class) != null) {
							rightMcc = rightPd.getReadMethod()
									.getAnnotation(DomainStoreDbColumn.class);
							if (rightMcc.targetEntity() != ref || !rightMcc
									.mappedBy().equals(field.getName())) {
								rightMcc = null;
							}
						}
						if (right == null && rightMcc == null) {
							errAssociation.add(methodName);
						}
					}
				}
				c = c.getSuperclass();
			}
		}
		if (!errClassRef.isEmpty()) {
			System.out.println(
					"Problems with classref reachability:\n-------------------");
			System.out.println(CommonUtils.join(errClassRef, "\n"));
			throw new RuntimeException("Cancelling startup");
		}
		if (!errAssociation.isEmpty()) {
			System.out.println(
					"Problems with inverse associations:\n-------------------");
			System.out.println(CommonUtils.join(errAssociation, "\n"));
			throw new RuntimeException("Cancelling startup");
		}
	}

	private void commit(EntityManager entityManager) throws Exception {
		if (Configuration.is("commitDisabled")) {
			return;
		}
		for (ClassrefScannerMetadata metadata : outgoingCache.classData
				.values()) {
			if (metadata.isClassRef) {
				persistableClasses.add(Reflections.forName(metadata.className));
			}
		}
		if (!persistent) {
			Class<? extends ClassRef> crimpl = PersistentImpl
					.getImplementation(ClassRef.class);
			long idCtr = 0;
			for (Class clazz : persistableClasses) {
				ClassRef ref = ClassRef.forClass(clazz);
				ref = crimpl.getDeclaredConstructor().newInstance();
				ref.setRefClass(clazz);
				ref.setId(++idCtr);
				ClassRef.add(CommonUtils.wrapInCollection(ref));
			}
		} else {
			Class<? extends ClassRef> classRefImplClass = PersistentImpl
					.getImplementation(ClassRef.class);
			Query query = entityManager.createQuery(String.format("from %s ",
					classRefImplClass.getSimpleName()));
			List<? extends ClassRef> classrefs = query.getResultList();
			Set<? extends ClassRef> deleteClassrefs = new HashSet<ClassRef>();
			ClassRef.add(classrefs);
			((Set) deleteClassrefs).addAll(classrefs);
			classrefs.clear();
			boolean delta = false;
			for (Class clazz : persistableClasses) {
				ClassRef ref = ClassRef.forClass(clazz);
				if (ref == null) {
					delta = true;
					ref = classRefImplClass.getDeclaredConstructor()
							.newInstance();
					ref.setRefClass(clazz);
					long id = 0;
					if (AppPersistenceBase.isInstanceReadOnly()) {
						id = --roIdCounter;
					} else {
						id = entityManager.merge(ref).getId();
					}
					ref.setId(id);
					ClassRef.add(CommonUtils.wrapInCollection(ref));
					logger.info("adding classref - {} {}\n", ref.getId(),
							ref.getRefClassName());
				} else {
					deleteClassrefs.remove(ref);
				}
			}
			if (AppPersistenceBase.isInstanceReadOnly()) {
			} else {
				for (ClassRef ref : deleteClassrefs) {
					delta = true;
					logger.trace("removing classref - {} {}\n", ref.getId(),
							ref.getRefClassName());
					if (Configuration.is("removePersistentClassrefs")) {
						entityManager.remove(ref);
					}
					ClassRef.remove(ref);
				}
			}
		}
	}

	@Override
	protected ClassrefScannerMetadata createMetadata(String className,
			ClassMetadata found) {
		return new ClassrefScannerMetadata(className).fromUrl(found);
	}

	public ClassrefScanner noPersistence() {
		persistent = false;
		return this;
	}

	public ClassrefScanner noReachabilityCheck() {
		reachabilityCheck = false;
		return this;
	}

	@Override
	protected ClassrefScannerMetadata process(Class clazz, String className,
			ClassMetadata found) {
		ClassrefScannerMetadata out = createMetadata(className, found);
		if ((!Modifier.isPublic(clazz.getModifiers()))
				|| (Modifier.isAbstract(clazz.getModifiers())
						&& !clazz.isEnum())) {
		} else {
			/*
			 * why are these required? (answer: makes sure they're in the gwt
			 * code - otherwise could be sending classes not compiled into
			 * client code)
			 */
			boolean bi = new AnnotationLocation(clazz, null)
					.hasAnnotation(Bean.class);
			boolean refl = new AnnotationLocation(clazz, null)
					.hasAnnotation(Reflected.class);
			boolean dtp = clazz
					.isAnnotationPresent(DomainTransformPersistable.class);
			boolean nonPersistent = clazz
					.isAnnotationPresent(NonDomainTransformPersistable.class);
			if (!nonPersistent
					&& (Entity.class.isAssignableFrom(clazz)
							&& (refl || bi || dtp))
					|| (clazz.isEnum() && (refl || dtp))) {
				out.isClassRef = true;
			} else {
			}
		}
		return out;
	}

	public void scan(ClassMetadataCache cache, EntityManager entityManager)
			throws Exception {
		String cachePath = getHomeDir().getPath() + File.separator
				+ "classref-scanner-cache.ser";
		persistableClasses = new LinkedHashSet<Class>();
		persistableClasses.addAll(Arrays.asList(new Class[] { Long.class,
				Double.class, Float.class, Integer.class, Short.class,
				String.class, Date.class, Boolean.class }));
		scan(cache, cachePath);
		commit(entityManager);
		if (reachabilityCheck) {
			checkReachability();
		}
	}

	public static class ClassrefScannerMetadata
			extends ClassMetadata<ClassrefScannerMetadata> {
		public boolean isClassRef;

		public ClassrefScannerMetadata() {
		}

		public ClassrefScannerMetadata(String className) {
			super(className);
		}
	}
}
