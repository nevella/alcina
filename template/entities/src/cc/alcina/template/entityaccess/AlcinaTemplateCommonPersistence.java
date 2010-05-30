/**
 * 
 */
package cc.alcina.template.entityaccess;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

import org.hibernate.proxy.LazyInitializer;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.template.AlcinaTemplate;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.WrappedObject;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.entity.util.GraphProjection.ClassFieldPair;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;
import cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.persistent.ClassRefImpl;
import cc.alcina.template.cs.persistent.ClientInstanceImpl;
import cc.alcina.template.cs.persistent.IidImpl;
import cc.alcina.template.cs.persistent.LogRecord;
import cc.alcina.template.j2seentities.DomainTransformEventPersistentImpl;
import cc.alcina.template.j2seentities.DomainTransformRequestPersistentImpl;

@AlcinaTemplate
@Stateless
public class AlcinaTemplateCommonPersistence
		extends
		CommonPersistenceBase<ClientInstanceImpl, AlcinaTemplateUser, AlcinaTemplateGroup, IidImpl>
		implements CommonPersistenceLocal {
	public static final String RemoteJNDIName = AlcinaTemplateCommonPersistence.class
			.getSimpleName()
			+ "/remote";

	public static final String LocalJNDIName = AlcinaTemplateCommonPersistence.class
			.getSimpleName()
			+ "/local";

	public AlcinaTemplateCommonPersistence() {
		super();
	}

	public AlcinaTemplateCommonPersistence(EntityManager em) {
		super(em);
	}

	protected InstantiateImplCallback createUserAndGroupInstantiator() {
		return new InstantiateImplCallback<LazyInitializer>() {
			public boolean instantiateLazyInitializer(
					LazyInitializer initializer, ClassFieldPair context) {
				Class persistentClass = initializer.getPersistentClass();
				return IUser.class.isAssignableFrom(persistentClass)
						|| IGroup.class.isAssignableFrom(persistentClass);
			}
		};
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

	@Override
	public String getAnonymousUserName() {
		return AlcinaTemplateAccessConstants.ANONYMOUS_USER;
	}

	@Override
	public String getSystemUserName() {
		return AlcinaTemplateAccessConstants.SYSTEM_USER;
	}
}