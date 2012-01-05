/**
 * 
 */
package cc.alcina.template.entityaccess;

import java.util.Date;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.proxy.LazyInitializer;

import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;

import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;
import cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.persistent.ClientInstanceImpl;
import cc.alcina.template.cs.persistent.IidImpl;
import cc.alcina.template.cs.persistent.LogRecord;

@Stateless
public class AlcinaTemplateCommonPersistence
		extends
		CommonPersistenceBase<ClientInstanceImpl, AlcinaTemplateUser, AlcinaTemplateGroup, IidImpl>
		implements CommonPersistenceLocal {
	public static final String RemoteJNDIName = AlcinaTemplateCommonPersistence.class
			.getSimpleName() + "/remote";

	public static final String LocalJNDIName = AlcinaTemplateCommonPersistence.class
			.getSimpleName();

	@PersistenceContext
	private EntityManager entityManager;

	public AlcinaTemplateCommonPersistence() {
		super();
	}

	public AlcinaTemplateCommonPersistence(EntityManager em) {
		super(em);
	}

	@Override
	public String getAnonymousUserName() {
		return AlcinaTemplateAccessConstants.ANONYMOUS_USER;
	}

	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	@Override
	public String getSystemUserName() {
		return AlcinaTemplateAccessConstants.SYSTEM_USER;
	}

	public long log(String message, String componentKey) {
		LogRecord l = new LogRecord();
		getEntityManager().persist(l);
		l.setCreatedOn(new Date());
		l.setComponentKey(componentKey);
		l.setText(message);
		l.setUserId(PermissionsManager.get().getUserId());
		return l.getId();
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	protected InstantiateImplCallback createUserAndGroupInstantiator() {
		return new InstantiateImplCallback<LazyInitializer>() {
			public boolean instantiateLazyInitializer(
					LazyInitializer initializer, GraphProjectionContext context) {
				Class persistentClass = initializer.getPersistentClass();
				return IUser.class.isAssignableFrom(persistentClass)
						|| IGroup.class.isAssignableFrom(persistentClass);
			}
		};
	}
}