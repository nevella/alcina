package cc.alcina.template.entityaccess;

import static cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.hibernate.proxy.LazyInitializer;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;
import cc.alcina.framework.entity.util.JaxbUtils;
import cc.alcina.framework.entity.util.UnixCrypt;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.persistent.Bookmark;
import cc.alcina.template.cs.persistent.ClientInstanceImpl;
import cc.alcina.template.cs.persistent.IidImpl;


@Stateless
public class AlcinaTemplatePersistence
		extends
		AppPersistenceBase<ClientInstanceImpl, AlcinaTemplateUser, AlcinaTemplateGroup, IidImpl>
		implements AlcinaTemplatePersistenceLocal {
	public static final String RemoteJNDIName = AlcinaTemplatePersistence.class
			.getSimpleName()
			+ "/remote";

	public static final String LocalJNDIName = AlcinaTemplatePersistence.class
			.getSimpleName()
			;

	public AlcinaTemplateObjects loadInitial(boolean internal) throws Exception {
		getCommonPersistence().connectPermissionsManagerToLiveObjects();
		String key = "Initial load - user "
				+ PermissionsManager.get().getUserName();
		MetricLogging.get().start(key);
		AlcinaTemplateObjects initialObjects = new AlcinaTemplateObjects();
		GeneralProperties generalProperties = EntityLayerLocator.get()
				.wrappedObjectProvider().getWrappedObjectForUser(
						GeneralProperties.class, 0, em);
		initialObjects.setGeneralProperties(generalProperties);
		List<AlcinaTemplateGroup> groups = getVisibleGroups();
		initialObjects.getGroups().addAll(groups);
		initialObjects.setClassRefs(ClassRef.all());
		initialObjects.setCurrentUser((AlcinaTemplateUser) PermissionsManager
				.get().getUser());
		initialObjects.setServerDate(new Date());
		initialObjects.setBookmarks(getAllForUser(Bookmark.class));
		MetricLogging.get().end(key);
		return new EntityUtils().detachedClone(initialObjects);
	}

	@PersistenceContext
	protected EntityManager em;

	@PersistenceUnit
	protected EntityManagerFactory factory;

	public AlcinaTemplatePersistence(EntityManager entityManager) {
		super();
		if (entityManager != null) {
			em = entityManager;
		}
	}

	public AlcinaTemplatePersistence() {
		this(null);
	}
	@Override
	public List<AlcinaTemplateGroup> getVisibleGroups() {
		return new ArrayList<AlcinaTemplateGroup>(super.getVisibleGroups());
	}

	public void createSystemGroupsAndUsers() {
		String n = SYSTEM_GROUP;
		if (getGroupByName(n) == null) {
			PermissionsManager.get().setUser(null);
			AlcinaTemplateGroup g = new AlcinaTemplateGroup();
			em.persist(g);
			g.setGroupName(SYSTEM_GROUP);
			AlcinaTemplateUser u = new AlcinaTemplateUser();
			u.setUserName(SYSTEM_USER);
			u.setPrimaryGroup(g);
			u.setSystem(true);
			PermissionsManager.get().setUser(u);
			em.persist(u);
		}
		n = ANONYMOUS_GROUP;
		if (getGroupByName(n) == null) {
			AlcinaTemplateGroup g = new AlcinaTemplateGroup();
			g.setGroupName(ANONYMOUS_GROUP);
			AlcinaTemplateUser u = new AlcinaTemplateUser();
			u.setUserName(ANONYMOUS_USER);
			u.setPrimaryGroup(g);
			u.setSystem(true);
			PermissionsManager.get().setUser(u);
			em.persist(g);
		}
		n = AlcinaTemplateAccessConstants.ADMINISTRATORS_GROUP_NAME;
		if (getGroupByName(n) == null) {
			AlcinaTemplateGroup g = new AlcinaTemplateGroup();
			g.setGroupName(n);
			AlcinaTemplateUser u = new AlcinaTemplateUser();
			u
					.setUserName(AlcinaTemplateAccessConstants.INITIAL_ADMINISTRATOR_USER_NAME);
			u.setPrimaryGroup(g);
			u.setSalt(u.getUserName());
			u
					.setPassword(UnixCrypt
							.crypt(
									u.getSalt(),
									AlcinaTemplateAccessConstants.INITIAL_ADMINISTRATOR_PASSWORD));
			PermissionsManager.get().setUser(u);
			em.persist(g);
		}
	}

	@SuppressWarnings("unchecked")
	AlcinaTemplateGroup getGroupByName(String name) {
		List<AlcinaTemplateGroup> l = em.createQuery(
				"from AlcinaTemplateGroup g where g.groupName = ?")
				.setParameter(1, name).getResultList();
		return (l.size() == 0) ? null : l.get(0);
	}

	@Override
	protected CommonPersistenceLocal getCommonPersistence() {
		if (commonPersistence == null) {
			commonPersistence = new AlcinaTemplateCommonPersistence(em);
		}
		return commonPersistence;
	}

	public List<AlcinaTemplateGroup> getAllGroups() {
		return new EntityUtils().detachedClone(getAllGroupEntities(),
				userGroupGetterCallback);
	}

	private InstantiateImplCallback userGroupGetterCallback = new InstantiateImplCallback<LazyInitializer>() {
		public boolean instantiateLazyInitializer(LazyInitializer initializer,
				GraphProjectionContext context) {
			Class persistentClass = initializer.getPersistentClass();
			return persistentClass == AlcinaTemplateGroup.class
					|| persistentClass == AlcinaTemplateUser.class;
		}
	};

	

	@Override
	protected void initServiceImpl() {
		EntityLayerLocator.get().registerWrappedObjectProvider(
				new AlcinaTemplateWrappedObjectProvider());
		EntityLayerLocator.get().registerCommonPersistenceProvider(
				AlcinaTemplateEjbLocator.get());
		CommonLocator.get().registerCurrentUtcDateProvider(
				ObjectPersistenceHelper.get());
	}
	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	@Override
	protected EntityManagerFactory getEntityManagerFactory() {
		return factory;
	}

}
