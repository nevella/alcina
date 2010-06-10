package cc.alcina.template.entityaccess;

import java.util.List;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.PersistentSingleton;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

import cc.alcina.framework.entity.domaintransform.WrappedObjectProvider;
import cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;

@SuppressWarnings("unchecked")
public class AlcinaTemplateWrappedObjectProvider implements WrappedObjectProvider {
	public <T extends WrapperPersistable> WrappedObjectImpl<T> getObjectWrapperForUser(
			Class<T> c, long id, EntityManager em) throws Exception {
		WrappedObjectImpl<T> wrappedObject = em.find(
				WrappedObjectImpl.class, id);
		if (wrappedObject == null && !(GwtMultiplePersistable.class.isAssignableFrom(c))) {
			String userName = (PersistentSingleton.class.isAssignableFrom(c)) ? AlcinaTemplateAccessConstants.SYSTEM_USER
					: PermissionsManager.get().getUserName();
			List l = em
					.createQuery(
							"from WrappedObjectImpl w where w.user.userName = ? and w.className = ?")
					.setParameter(1, userName).setParameter(2, c.getName())
					.getResultList();
			wrappedObject = (WrappedObjectImpl) ((l.size() == 0) ? null : l
					.get(0));
		}
		if (wrappedObject == null) {
			AlcinaTemplateUser user = (PersistentSingleton.class.isAssignableFrom(c)) ? new AlcinaTemplateCommonPersistence(
					em).getSystemUser()
					: (AlcinaTemplateUser) PermissionsManager.get().getUser();
			wrappedObject = new WrappedObjectImpl();
			em.persist(wrappedObject);
			wrappedObject.setUser(user);
			WrapperPersistable newInstance = c.newInstance();
			wrappedObject.setObject((T) newInstance);
		}
		return wrappedObject;
	}

	public <T extends WrapperPersistable> T getWrappedObjectForUser(
			Class<T> c, EntityManager em) throws Exception {
		return getWrappedObjectForUser(c, 0, em);
	}

	public <T extends WrapperPersistable> T getWrappedObjectForUser(
			Class<T> c, long id, EntityManager em) throws Exception {
		WrappedObjectImpl<T> wrappedObject = getObjectWrapperForUser(c, id,
				em);
		return wrappedObject.getObject();
	}

	public List<Class> getJaxbSubclasses() {
		 return Registry.get().lookup(JaxbContextRegistration.class);
	}
}
