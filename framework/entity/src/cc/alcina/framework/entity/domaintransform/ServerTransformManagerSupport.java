package cc.alcina.framework.entity.domaintransform;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.HasAnnotationCallback;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.SyntheticGetter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainProxy;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

public class ServerTransformManagerSupport {
	public static final String CONTEXT_NO_ASSOCIATION_CHECK = ServerTransformManagerSupport.class
			.getName() + ".CONTEXT_NO_ASSOCIATION_CHECK";

	public void removeAssociations(Entity entity) {
		if (LooseContext.is(CONTEXT_NO_ASSOCIATION_CHECK)) {
			return;
		}
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(entity.getClass())
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (Set.class.isAssignableFrom(pd.getPropertyType())) {
					Association info = pd.getReadMethod()
							.getAnnotation(Association.class);
					Set set = (Set) pd.getReadMethod().invoke(entity,
							CommonUtils.EMPTY_OBJECT_ARRAY);
					if (info != null && set != null) {
						for (Object o2 : set) {
							String accessorName = "get" + CommonUtils
									.capitaliseFirst(info.propertyName());
							Object o3 = o2.getClass()
									.getMethod(accessorName, new Class[0])
									.invoke(o2, CommonUtils.EMPTY_OBJECT_ARRAY);
							if (o3 instanceof Set) {
								Set assocSet = (Set) o3;
								assocSet.remove(entity);
							}
							if (info.dereferenceOnDelete()) {
								if (!ThreadlocalTransformManager
										.isInEntityManagerTransaction()) {
									TransformManager.get().registerDomainObject(
											(Entity) o2);
									Reflections.propertyAccessor()
											.setPropertyValue(o2,
													info.propertyName(), null);
								}
							}
							/*
							 * direct references (parent/one-one) are not
							 * removed, throw a referential integrity exception
							 * instead i.e. these *must* be handled explicity in
							 * code three years later, not sure why I opted for
							 * the above but I guess, if it's server layer, the
							 * programmer has more control and there may be a
							 * decent reason hmm - for domainStore, we need
							 * auto-removal of parent/child relations if
							 * transforming from servlet layer. possibly the
							 * "no-auto" was answer.getsurveyresponse
							 * performance in cosa? ..ahah - permissions
							 * reasons. if we null user/group (and are not
							 * root), may cause unwanted permissions probs - so
							 * only do in removeParentAssociations(), which is
							 * effectively root-only
							 */
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void removeParentAssociations(Entity entity) {
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(entity.getClass())
					.getPropertyDescriptors();
			Entity domainVersion = null;
			if (entity instanceof Entity
					&& DomainStore.writableStore().isCached(entity.getClass())) {
				domainVersion = entity.domain().domainVersion();
				if (domainVersion instanceof DomainProxy) {
					// don't run against proxies, it's a little tricky - keep
					// these manual for the moment
					domainVersion = null;
				}
			}
			for (PropertyDescriptor pd : pds) {
				if (Entity.class.isAssignableFrom(pd.getPropertyType())
						&& pd.getWriteMethod() != null && pd.getReadMethod()
								.getAnnotation(SyntheticGetter.class) == null) {
					Entity hiliTarget = (Entity) pd
							.getReadMethod()
							.invoke(entity, CommonUtils.EMPTY_OBJECT_ARRAY);
					if (hiliTarget == null) {
						if (domainVersion != null) {
							hiliTarget = (Entity) pd.getReadMethod()
									.invoke(domainVersion,
											CommonUtils.EMPTY_OBJECT_ARRAY);
							if (hiliTarget != null) {
								Entity writeable = ((Entity) hiliTarget)
										.writeable();
								if (writeable != null) {
									/*
									 * not part of domain - use the target
									 */
									hiliTarget = writeable;
								}
								try {
									// just so it can be nulled
									pd.getWriteMethod().invoke(entity,
											new Object[] { hiliTarget });
								} catch (InvocationTargetException e) {
									if (e.getTargetException() instanceof UnsupportedOperationException) {
									} else {
										throw e;
									}
								}
							}
						}
					}
					if (hiliTarget != null && !(hiliTarget instanceof IUser)
							&& !(hiliTarget instanceof IGroup)) {
						try {
							pd.getWriteMethod().invoke(entity,
									new Object[] { null });
						} catch (InvocationTargetException e) {
							if (e.getTargetException() instanceof UnsupportedOperationException) {
							} else {
								throw e;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void doCascadeDeletes(final Entity entity) {
		PropertyAccessor propertyAccessor = Reflections.propertyAccessor();
		SEUtilities.iterateForPropertyWithAnnotation(entity, Association.class,
				new HasAnnotationCallback<Association>() {
					@Override
					public void apply(Association association,
							PropertyReflector propertyReflector) {
						if (association.cascadeDeletes()) {
							Object object = propertyReflector
									.getPropertyValue(entity);
							if (object instanceof Set) {
								for (Entity target : (Set<Entity>) object) {
									ThreadlocalTransformManager.get()
											.deleteObject(target, true);
								}
							}
						}
					}
				});
	}
}
