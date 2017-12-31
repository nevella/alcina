package cc.alcina.framework.entity.entityaccess;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.entity.PersistentSingleton;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.permissions.IVersionableOwnable;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.Wrapper;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaLoggingTags;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.WrappedObjectProvider;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class WrappedObjectPersistence {
	public static final String CONTEXT_THROW_MISSING_WRAPPED_OBJECT = WrappedObjectPersistence.class
			.getName() + ".CONTEXT_THROW_MISSING_WRAPPED_OBJECT";

	public static final String CONTEXT_IGNORE_MISSING_WRAPPED_OBJECT = WrappedObjectPersistence.class
			.getName() + ".CONTEXT_IGNORE_MISSING_WRAPPED_OBJECT";

	static Map<Class, List<PropertyDescriptor>> wrapperDescriptors = new LinkedHashMap<Class, List<PropertyDescriptor>>();

	public void checkWrappedObjectAccess(HasId wrapper, WrappedObject wrapped,
			Class clazz) throws PermissionsException {
		try {
			checkWrappedObjectAccess0(wrapper, wrapped, clazz);
		} catch (NullPointerException npe) {
			System.out.format(
					"Problem checking wrapped object access: %s %s %s\n",
					wrapper, wrapped, clazz);
		}
	}

	public <T extends HasId> List<Long> getWrapperIds(Collection<T> wrappers) {
		List<Long> wrapperIds = new ArrayList<Long>();
		try {
			for (HasId wrapper : wrappers) {
				PropertyDescriptor[] pds = Introspector
						.getBeanInfo(wrapper.getClass())
						.getPropertyDescriptors();
				for (PropertyDescriptor pd : pds) {
					if (pd.getReadMethod() != null) {
						Wrapper info = pd.getReadMethod()
								.getAnnotation(Wrapper.class);
						if (info != null) {
							PropertyDescriptor idpd = SEUtilities
									.getPropertyDescriptorByName(
											wrapper.getClass(),
											info.idPropertyName());
							Long wrapperId = (Long) idpd.getReadMethod().invoke(
									wrapper, CommonUtils.EMPTY_OBJECT_ARRAY);
							if (wrapperId != null) {
								wrapperIds.add(wrapperId);
							}
						}
					}
				}
			}
			return wrapperIds;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void unwrap(HasId wrapper, EntityManager entityManager,
			WrappedObjectProvider wrappedObjectProvider) throws Exception {
		unwrap(wrapper, entityManager, wrappedObjectProvider,
				new LinkedHashSet<Long>());
	}

	public void unwrap(HasId wrapper, EntityManager entityManager,
			WrappedObjectProvider wrappedObjectProvider,
			Set<Long> invalidatedWrapperIds) throws Exception {
		for (PropertyDescriptor pd : ensureWrapperDescriptors(
				wrapper.getClass())) {
			Wrapper info = pd.getReadMethod().getAnnotation(Wrapper.class);
			PropertyDescriptor idpd = SEUtilities.getPropertyDescriptorByName(
					wrapper.getClass(), info.idPropertyName());
			Long wrapperId = (Long) idpd.getReadMethod().invoke(wrapper,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			boolean invalidated = invalidatedWrapperIds.contains(wrapperId);
			if (invalidated) {
				invalidatedWrapperIds.remove(wrapperId);
			}
			if (!invalidated && pd.getReadMethod().invoke(wrapper,
					new Object[0]) != null) {
				continue;
			}
			if (wrapperId != null) {
				try {
					Class<? extends WrapperPersistable> pType = (Class<? extends WrapperPersistable>) pd
							.getPropertyType();
					if (info.defaultImplementationType() != Void.class) {
						pType = info.defaultImplementationType();
					}
					WrappedObject wrappedObject = wrappedObjectProvider
							.getObjectWrapperForUser(pType, wrapperId,
									entityManager);
					Object unwrapped = null;
					if (wrappedObject == null) {
						if (LooseContext
								.is(CONTEXT_IGNORE_MISSING_WRAPPED_OBJECT)) {
							TaggedLogger logger = Registry
									.impl(TaggedLoggers.class).getLogger(null,
											AlcinaLoggingTags.WRAPPED_OBJECT_REF_INTEGRITY);
							logger.format(
									"Warning - ref integrity (wrapped object) - missing %s.%s #%s",
									wrapper.getClass(), pd.getName(),
									wrapper.getId());
							if (LooseContext.getBoolean(
									CONTEXT_THROW_MISSING_WRAPPED_OBJECT)) {
								throw new MissingWrappedObjectException();
							} else {
								unwrapped = pType.newInstance();
							}
						}
					} else {
						checkWrappedObjectAccess(wrapper, wrappedObject, pType);
						unwrapped = wrappedObject.getObject();
					}
					pd.getWriteMethod().invoke(wrapper, unwrapped);
				} catch (RuntimeException e) {
					System.out.format("Exception unwrapping/getObject %s\n",
							wrapperId);
					throw e;
				}
			}
		}
	}

	private void checkWrappedObjectAccess0(HasId wrapper, WrappedObject wrapped,
			Class clazz) throws PermissionsException {
		if (!PersistentSingleton.class.isAssignableFrom(clazz)
				&& wrapped != null && wrapped.getUser()
						.getId() != PermissionsManager.get().getUserId()) {
			if (wrapper != null) {
				if (wrapper instanceof IVersionableOwnable) {
					IVersionableOwnable ivo = (IVersionableOwnable) wrapper;
					if (ivo.getOwner().getId() == wrapped.getUser().getId()) {
						return;// permitted
					}
					if (ivo.getOwnerGroup() != null && PermissionsManager.get()
							.isMemberOfGroup(ivo.getOwnerGroup().getName())) {
						return;// ditto
					}
				}
			}
			Optional<WrappedObjectPermissionsExtension> extension = Registry
					.implOptional(WrappedObjectPermissionsExtension.class);
			if (extension.isPresent()) {
				if (extension.get().allow(wrapper)) {
					return;
				}
			}
			if (PermissionsManager.get()
					.isPermissible(PermissionsManager.ADMIN_PERMISSIBLE)) {
				if (!PermissionsManager.get()
						.isPermissible(PermissionsManager.ROOT_PERMISSIBLE)) {
					System.err.println(CommonUtils.formatJ(
							"Warn - allowing access to %s : %s only via admin override",
							wrapper == null ? "(null wrapper)"
									: HiliHelper.asDomainPoint(wrapper),
							HiliHelper.asDomainPoint(wrapped)));
				}
				return;// permitted
			}
			throw new PermissionsException(CommonUtils.formatJ(
					"Permissions exception: "
							+ "access denied to object  %s for user %s",
					wrapped.getId(), PermissionsManager.get().getUserId()));
		}
	}

	private List<PropertyDescriptor> ensureWrapperDescriptors(
			Class<? extends HasId> clazz) throws Exception {
		if (!wrapperDescriptors.containsKey(clazz)) {
			List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
			PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null) {
					Wrapper info = pd.getReadMethod()
							.getAnnotation(Wrapper.class);
					if (info != null) {
						descriptors.add(pd);
					}
				}
			}
			synchronized (wrapperDescriptors) {
				wrapperDescriptors.put(clazz, descriptors);
			}
		}
		return wrapperDescriptors.get(clazz);
	}

	public static class MissingWrappedObjectException extends Exception {
	}

	public interface WrappedObjectPermissionsExtension {
		boolean allow(HasId wrapper);
	}
}
