package cc.alcina.framework.entity.entityaccess;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.entity.PersistentSingleton;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliHelper;
import cc.alcina.framework.common.client.logic.permissions.IVersionableOwnable;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.WrapperInfo;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.WrappedObjectProvider;
import cc.alcina.framework.entity.logic.EntityLayerLocator;

public class WrappedObjectPersistence {
	public void unwrap(HasId wrapper, EntityManager entityManager, WrappedObjectProvider wrappedObjectProvider) throws Exception {
		PropertyDescriptor[] pds = Introspector.getBeanInfo(wrapper.getClass())
				.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			if (pd.getReadMethod() != null) {
				WrapperInfo info = pd.getReadMethod().getAnnotation(
						WrapperInfo.class);
				if (info != null) {
					PropertyDescriptor idpd = SEUtilities.descriptorByName(
							wrapper.getClass(), info.idPropertyName());
					Long wrapperId = (Long) idpd.getReadMethod().invoke(
							wrapper, CommonUtils.EMPTY_OBJECT_ARRAY);
					if (wrapperId != null) {
						Class<? extends WrapperPersistable> pType = (Class<? extends WrapperPersistable>) pd
								.getPropertyType();
						if (info.defaultImplementationType() != Void.class) {
							pType = info.defaultImplementationType();
						}
						WrappedObject wrappedObject = wrappedObjectProvider
								.getObjectWrapperForUser(pType, wrapperId,
										entityManager);
						checkWrappedObjectAccess(wrapper, wrappedObject, pType);
						Object unwrapped = wrappedObject.getObject();
						pd.getWriteMethod().invoke(wrapper, unwrapped);
					}
				}
			}
		}
	}

	public void checkWrappedObjectAccess(HasId wrapper, WrappedObject wrapped,
			Class clazz) throws PermissionsException {
		if (!PersistentSingleton.class.isAssignableFrom(clazz)
				&& wrapped != null
				&& wrapped.getUser().getId() != PermissionsManager.get()
						.getUserId()) {
			if (wrapper != null) {
				if (wrapper instanceof IVersionableOwnable) {
					IVersionableOwnable ivo = (IVersionableOwnable) wrapper;
					if (ivo.getOwner().getId() == wrapped.getUser().getId()) {
						return;// permitted
					}
					if (ivo.getOwnerGroup() != null
							&& PermissionsManager.get().isMemberOfGroup(
									ivo.getOwnerGroup().getName())) {
						return;// ditto
					}
				}
			}
			if (PermissionsManager.get().isPermissible(
					PermissionsManager.ADMIN_PERMISSIBLE)) {
				if (!PermissionsManager.get().isPermissible(
						PermissionsManager.ROOT_PERMISSIBLE)) {
					System.err
							.println(CommonUtils
									.formatJ(
											"Warn - allowing access to %s : %s only via admin override",
											wrapper == null ? "(null wrapper)"
													: HiliHelper
															.asDomainPoint(wrapper),
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
}
