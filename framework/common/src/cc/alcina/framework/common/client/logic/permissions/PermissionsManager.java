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
package cc.alcina.framework.common.client.logic.permissions;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Permission.SimplePermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StackDebug;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

/**
 * <h2>Notes</h2>
 * <p>
 * Permissions type ADMIN_OR_OWNER pretty much mandates that the object
 * implement HasOwner
 * </p>
 * 
 * @author Nick Reddel
 */
public class PermissionsManager implements DomainTransformListener {
	public static String SYSTEM_GROUP_NAME = "system";

	public static String SYSTEM_USER_NAME = "system_user";

	public static String ANONYMOUS_USER_NAME = "anonymous";

	public static String ANONYMOUS_GROUP_NAME = "anonymous";

	public static String ADMINISTRATORS_GROUP_NAME = "Administrators";

	public static String INITIAL_ADMINISTRATOR_USER_NAME = "admin@nodomain.cc";

	public static String INITIAL_ADMINISTRATOR_PASSWORD = "admin";

	public static String DEVELOPERS_GROUP_NAME = "Developers";

	private static String administratorGroupName = "Administrators";

	private static String developerGroupName = "Developers";

	private static String anonymousUserName = "anonymous";

	private static PermissionsManager factoryInstance;

	private static PermissionsExtension permissionsExtension;

	public static final Permissible ROOT_PERMISSIBLE = new Permissible() {
		@Override
		public AccessLevel accessLevel() {
			return AccessLevel.ROOT;
		}

		@Override
		public String rule() {
			return null;
		}
	};

	public static final Permissible ADMIN_PERMISSIBLE = new Permissible() {
		@Override
		public AccessLevel accessLevel() {
			return AccessLevel.ADMIN;
		}

		@Override
		public String rule() {
			return null;
		}
	};

	public static final String CONTEXT_CREATION_PARENT = PermissionsManager.class
			.getName() + ".CONTEXT_CREATION_PARENT";

	public static StackDebug stackDebug = new StackDebug("PermissionsManager");

	private static Topic<ClientInstance> topicClientInstance = Topic.local();

	private static Topic<LoginState> topicLoginState = Topic.local();

	private static Topic<OnlineState> topicOnlineState = Topic.local();

	public static void confirmDepth(int depth) {
		Preconditions.checkState(get().depth0() == depth);
	}

	public static int depth() {
		if (factoryInstance == null) {
			return 0;
		}
		return get().depth0();
	}

	public static PermissionsManager get() {
		if (factoryInstance == null) {
			factoryInstance = new PermissionsManager();
		}
		PermissionsManager pm = factoryInstance.getT();
		if (pm != null) {
			return pm;
		}
		return factoryInstance;
	}

	public static String getAdministratorGroupName() {
		return administratorGroupName;
	}

	public static String getAnonymousUserName() {
		return anonymousUserName;
	}

	public static String getDeveloperGroupName() {
		return developerGroupName;
	}

	public static ObjectPermissions getObjectPermissions(Class domainClass) {
		ObjectPermissions objectPermissions = Reflections.classLookup()
				.getAnnotationForClass(domainClass, ObjectPermissions.class);
		return objectPermissions == null ? get().getDefaultObjectPermissions()
				: objectPermissions;
	}

	public static PermissionsExtension getPermissionsExtension() {
		return permissionsExtension;
	}

	public static boolean hasDeletePermission(Object object) {
		AnnotationLocation clazzLocation = new AnnotationLocation(
				object.getClass(), null);
		Bean beanInfo = clazzLocation.getAnnotation(Bean.class);
		ObjectPermissions op = clazzLocation
				.getAnnotation(ObjectPermissions.class);
		if (op == null) {
			return false;
		} else {
			return PermissionsManager.get().isPermitted(object, op.delete());
		}
	}

	public static boolean hasReadPermission(Object object) {
		AnnotationLocation clazzLocation = new AnnotationLocation(
				object.getClass(), null);
		Bean beanInfo = clazzLocation.getAnnotation(Bean.class);
		ObjectPermissions op = clazzLocation
				.getAnnotation(ObjectPermissions.class);
		if (op == null) {
			return false;
		} else {
			return PermissionsManager.get().checkEffectivePropertyPermission(op,
					null, object, true);
		}
	}

	public static boolean hasWritePermission(Object object) {
		AnnotationLocation clazzLocation = new AnnotationLocation(
				object.getClass(), null);
		Bean beanInfo = clazzLocation.getAnnotation(Bean.class);
		ObjectPermissions op = clazzLocation
				.getAnnotation(ObjectPermissions.class);
		if (op == null) {
			return false;
		} else {
			return PermissionsManager.get().checkEffectivePropertyPermission(op,
					null, object, true);
		}
	}

	public static boolean isOffline() {
		return get().getOnlineState() == OnlineState.OFFLINE;
	}

	public static boolean isOnline() {
		return !isOffline();
	}

	public static void register(PermissionsManager pm) {
		factoryInstance = pm;
	}

	public static void removePerThreadContext() {
		if (factoryInstance == null) {
			return;
		}
		factoryInstance.removePerThreadContext0();
	}

	public static void
			setAdministratorGroupName(String administratorGroupName) {
		PermissionsManager.administratorGroupName = administratorGroupName;
	}

	public static void setAnonymousUserName(String anonymousUserName) {
		PermissionsManager.anonymousUserName = anonymousUserName;
	}

	public static void setDeveloperGroupName(String developerGroupName) {
		PermissionsManager.developerGroupName = developerGroupName;
	}

	public static void
			setPermissionsExtension(PermissionsExtension permissionsExtension) {
		PermissionsManager.permissionsExtension = permissionsExtension;
	}

	public static Topic<ClientInstance> topicClientInstance() {
		return topicClientInstance;
	}

	public static Topic<LoginState> topicLoginState() {
		return topicLoginState;
	}

	public static Topic<OnlineState> topicOnlineState() {
		return topicOnlineState;
	}

	private static void recursivePopulateGroupMemberships(Set<IGroup> members,
			Set<IGroup> processed) {
		while (true) {
			boolean maybeAllProcessed = true;
			for (Iterator<IGroup> itr = members.iterator(); itr.hasNext();) {
				IGroup group = itr.next();
				if (processed.contains(group)) {
					continue;
				} else {
					processed.add(group);
					members.addAll(group.getMemberOfGroups());
					maybeAllProcessed = false;
					break;
				}
			}
			if (maybeAllProcessed) {
				break;
			}
		}
	}

	private LoginState loginState = LoginState.NOT_LOGGED_IN;

	private OnlineState onlineState = OnlineState.ONLINE;

	private long userId;

	private IUser user;

	private ClientInstance clientInstance;

	private HashMap<String, IGroup> groupMap;

	private PropertyPermissions defaultPropertyPermissions = new PropertyPermissions() {
		@Override
		public Class<? extends Annotation> annotationType() {
			return PropertyPermissions.class;
		}

		@Override
		public Permission read() {
			return SimplePermissions.getPermission(AccessLevel.EVERYONE);
		}

		@Override
		public Permission write() {
			return SimplePermissions.getPermission(AccessLevel.ADMIN_OR_OWNER);
		}
	};

	private ObjectPermissions defaultObjectPermissions = new ObjectPermissions() {
		@Override
		public Class<? extends Annotation> annotationType() {
			return ObjectPermissions.class;
		}

		@Override
		public Permission create() {
			return SimplePermissions.getPermission(AccessLevel.ROOT);
		}

		@Override
		public Permission delete() {
			return SimplePermissions.getPermission(AccessLevel.ROOT);
		}

		@Override
		public Permission read() {
			return SimplePermissions.getPermission(AccessLevel.ADMIN_OR_OWNER);
		}

		@Override
		public Permission write() {
			return SimplePermissions.getPermission(AccessLevel.ADMIN_OR_OWNER);
		}
	};

	protected Stack<PermissionsState> stateStack = new Stack<>();

	private Long authenticatedClientInstanceId;

	private boolean allPermissible = false;

	private boolean root;

	private boolean overrideAsOwnedObject;

	protected PermissionsManager() {
		super();
	}

	public void appShutdown() {
		factoryInstance = null;
	}

	public boolean checkEffectivePropertyPermission(Object bean,
			String propertyName, boolean read) {
		Class<? extends Object> clazz = bean.getClass();
		ObjectPermissions op = Reflections.classLookup()
				.getAnnotationForClass(clazz, ObjectPermissions.class);
		PropertyPermissions pp = Reflections.propertyAccessor()
				.getAnnotationForProperty(clazz, PropertyPermissions.class,
						propertyName);
		return checkEffectivePropertyPermission(op, pp, bean, read);
	}

	public boolean checkEffectivePropertyPermission(ObjectPermissions op,
			PropertyPermissions pp, Object bean, boolean read) {
		op = op == null ? PermissionsManager.get().getDefaultObjectPermissions()
				: op;
		if (pp == null && !PermissionsManager.get().isPermitted(bean,
				read ? op.read() : op.write())) {
			return false;
		}
		if (op != null && pp == null) {// assume defined object permissions
			// define read/write better than
			// property defaults
			return true;
		}
		pp = pp == null ? getDefaultPropertyPermissions() : pp;
		return isPermitted(bean, read ? pp.read() : pp.write());
	}

	public boolean checkReadable(Class clazz, String propertyName,
			Object bean) {
		ClassLookup classLookup = Reflections.classLookup();
		PropertyAccessor propertyAccessor = Reflections.propertyAccessor();
		ObjectPermissions op = classLookup.getAnnotationForClass(clazz,
				ObjectPermissions.class);
		PropertyPermissions pp = propertyAccessor.getAnnotationForProperty(
				clazz, PropertyPermissions.class, propertyName);
		return PermissionsManager.get().checkEffectivePropertyPermission(op, pp,
				bean == null ? classLookup.getTemplateInstance(clazz) : bean,
				true);
	}

	@Override
	public void domainTransform(DomainTransformEvent evt)
			throws DomainTransformException {
		if (evt.getSource() instanceof IGroup) {
			invalidateGroupMap();
		}
	}

	public Long getAuthenticatedClientInstanceId() {
		return this.authenticatedClientInstanceId;
	}

	public ClientInstance getClientInstance() {
		return this.clientInstance;
	}

	public Long getClientInstanceId() {
		ClientInstance clientInstance = getClientInstance();
		return clientInstance == null ? null : clientInstance.getId();
	}

	public ObjectPermissions getDefaultObjectPermissions() {
		return this.defaultObjectPermissions;
	}

	public PropertyPermissions getDefaultPropertyPermissions() {
		return defaultPropertyPermissions;
	}

	public LoginState getLoginState() {
		return this.loginState;
	}

	public AccessLevel getMaxPropertyAccessLevel() {
		if (isRoot()) {
			return AccessLevel.ROOT;
		}
		if (getLoginState() == LoginState.NOT_LOGGED_IN) {
			return AccessLevel.EVERYONE;
		} else {
			if (isDeveloper()) {
				return AccessLevel.DEVELOPER;
			}
			if (isAdmin()) {
				return AccessLevel.ADMIN;
			}
			return AccessLevel.LOGGED_IN;
		}
	}

	public OnlineState getOnlineState() {
		return onlineState;
	}

	public Set<IGroup> getReachableGroups(IUser user) {
		Set<IGroup> groups = new LinkedHashSet<IGroup>();
		if (user != null) {
			if (user.getPrimaryGroup() != null) {
				groups.add(user.getPrimaryGroup());
			}
			groups.addAll(user.getSecondaryGroups());
			recursivePopulateGroupMemberships(groups, new HashSet<IGroup>());
		}
		return groups;
	}

	public String getSystemUserName() {
		// TODO Auto-generated method stub
		return null;
	}

	public PermissionsManager getT() {
		return null;
	}

	public IUser getUser() {
		return this.user;
	}

	// poss cache
	public Map<String, ? extends IGroup> getUserGroups() {
		return getUserGroups(user);
	}

	public Map<String, ? extends IGroup> getUserGroups(IUser user) {
		if (user != this.user) {
			invalidateGroupMap();
		}
		if (groupMap != null) {
			return groupMap;
		}
		synchronized (this) {
			Set<IGroup> groups = getReachableGroups(user);
			groupMap = new HashMap<String, IGroup>();
			for (Iterator<IGroup> itr = groups.iterator(); itr.hasNext();) {
				IGroup group = itr.next();
				groupMap.put(group.getName(), group);
			}
			HashMap<String, IGroup> result = groupMap;
			if (user != this.user) {
				invalidateGroupMap();
			}
			return result;
		}
	}

	public long getUserId() {
		return this.userId;
	}

	public String getUserName() {
		return (getUser() == null) ? null : getUser().getUserName();
	}

	public String getUserString() {
		return Ax.format("%s/%s", getUserId(), getUserName());
	}

	public boolean isAdmin() {
		if (getAdministratorGroupName() == null || !isLoggedIn()) {
			return false;
		} else {
			return isMemberOfGroup(getAdministratorGroupName());
		}
	}

	public boolean isAllPermissible() {
		return allPermissible;
	}

	public boolean isAnonymousUser() {
		return getAnonymousUserName().equals(getUserName());
	}

	public boolean isDeveloper() {
		if (getAdministratorGroupName() == null || !isLoggedIn()) {
			return false;
		} else {
			return isMemberOfGroup(getDeveloperGroupName());
		}
	}

	public boolean isLoggedIn() {
		return getLoginState() != LoginState.NOT_LOGGED_IN;
	}

	public boolean isMemberOfGroup(long groupId) {
		for (IGroup group : getUserGroups().values()) {
			if (group.getId() == groupId) {
				return true;
			}
		}
		return false;
	}

	public boolean isMemberOfGroup(String groupName) {
		return getUserGroups().containsKey(groupName);
	}

	public boolean isMemberOfGroups(Collection<String> groupNames) {
		for (String groupName : groupNames) {
			if (isMemberOfGroup(groupName)) {
				return true;
			}
		}
		return false;
	}

	public boolean isOverrideAsOwnedObject() {
		return this.overrideAsOwnedObject;
	}

	public boolean isPermitted(Object o, Object assigningTo, Permissible p,
			boolean doNotEvaluateNullObjectPermissions) {
		if (allPermissible) {
			return true;
		}
		if (p.accessLevel().equals(AccessLevel.GROUP)) {
			if (p.rule() != null && p.rule().length() != 0
					&& isMemberOfGroup(p.rule())) {
				return true;
			}
			if (o instanceof IVersionableOwnable) {
				IVersionableOwnable ivo = (IVersionableOwnable) o;
				if (ivo.getOwnerGroup() == null
						|| isMemberOfGroup(ivo.getOwnerGroup().getName())) {
					return true;
				}
			}
		}
		boolean permitted = false;
		if (p.accessLevel() != null
				&& p.accessLevel() != AccessLevel.ADMIN_OR_OWNER) {
			permitted = p.accessLevel().ordinal() <= getMaxPropertyAccessLevel()
					.ordinal();
		} else {
			if (getMaxPropertyAccessLevel().ordinal() >= AccessLevel.ADMIN
					.ordinal()) {
				permitted = true;
			}
			if (isLoggedIn()) {
				if (o instanceof HasOwner) {
					permitted |= permitDueToOwnership((HasOwner) o);
				}
			} else {
				permitted |= Objects.equals(PermissionsManager.get().getUser(),
						o);
			}
		}
		if (!permitted && !doNotEvaluateNullObjectPermissions) {
			if (getPermissionsExtension() != null) {
				Boolean b = null;
				if (assigningTo != null) {
					b = getPermissionsExtension().isPermitted(o, assigningTo,
							p);
				} else {
					b = getPermissionsExtension().isPermitted(o, p);
				}
				if (b != null) {
					permitted = b;
				}
			}
		}
		return permitted;
	}

	public boolean isPermitted(Object o, Permissible p) {
		return isPermitted(o, p, false);
	}

	public boolean isPermitted(Object o, Permissible p,
			boolean doNotEvaluateNullObjectPermissions) {
		return isPermitted(o, null, p, doNotEvaluateNullObjectPermissions);
	}

	public boolean isPermitted(Object o, Permission p) {
		return isPermitted(o, new AnnotatedPermissible(p));
	}

	public boolean isPermitted(Permissible p) {
		return isPermitted(null, p);
	}

	// pretty much only for create permi8ssion checks (all others may be
	// object-dependent)
	public boolean isPermitted(Permission create) {
		return isPermitted(new AnnotatedPermissible(create));
	}

	public boolean isPermittedClass(Object object,
			Permission defaultPermission) {
		if (object instanceof Permissible) {
			return isPermitted((Permissible) object);
		}
		Permission permission = Reflections.classLookup()
				.getAnnotationForClass(object.getClass(), Permission.class);
		return isPermitted(permission != null ? permission : defaultPermission);
	}

	public boolean isRoot() {
		return root;
	}

	public boolean permitDueToOwnership(HasOwner hasOwner) {
		if (overrideAsOwnedObject) {
			return true;
		}
		if (hasOwner == null) {
			return false;
		}
		IUser owner = hasOwner.getOwner();
		if (owner == null) {
			return hasOwner instanceof Entity
					? TransformManager.get()
							.isInCreationRequest((Entity) hasOwner)
					: false;
		} else {
			return owner.equals(user);
		}
	}

	public IUser popSystemUser() {
		return popUser();
	}

	public IUser popUser() {
		stackDebug.maybeDebugStack(stateStack, false);
		IUser currentUser = getUser();
		PermissionsState state = stateStack.pop();
		setLoginState(state.loginState);
		setUser(state.user);
		setRoot(state.asRoot);
		return currentUser;
	}

	public void pushCurrentUser() {
		pushUser(getUser(), getLoginState(), isRoot());
	}

	public IUser pushSystemUser() {
		IUser systemUser = getSystemUser();
		pushUser(systemUser, LoginState.LOGGED_IN, true);
		return systemUser;
	}

	public void pushUser(IUser user, LoginState loginState) {
		pushUser(user, loginState, false);
	}

	public void pushUser(IUser user, LoginState loginState, boolean asRoot) {
		stackDebug.maybeDebugStack(stateStack, true);
		PermissionsState state = new PermissionsState(getUser(),
				getLoginState(), isRoot());
		stateStack.push(state);
		setLoginState(loginState);
		setUser(user);
		setRoot(asRoot);
	}

	// This should never be necessary, if the code always surrounds user
	// push/pop in try/finally...but...
	public void reset() {
		stateStack.clear();
		setRoot(false);
		setUser(null);
		setLoginState(LoginState.NOT_LOGGED_IN);
	}

	public void setAllPermissible(boolean allPermissible) {
		this.allPermissible = allPermissible;
	}

	public void setAuthenticatedClientInstanceId(
			Long authenticatedClientInstanceId) {
		this.authenticatedClientInstanceId = authenticatedClientInstanceId;
	}

	public void setClientInstance(ClientInstance clientInstance) {
		ClientInstance old_clientInstance = clientInstance;
		this.clientInstance = clientInstance;
		if (!Objects.equals(clientInstance, old_clientInstance)) {
			topicClientInstance().publish(clientInstance);
		}
	}

	public void setDefaultObjectPermissions(
			ObjectPermissions defaultObjectPermissions) {
		this.defaultObjectPermissions = defaultObjectPermissions;
	}

	public void setDefaultPropertyPermissions(
			PropertyPermissions defaultPropertyPermissions) {
		this.defaultPropertyPermissions = defaultPropertyPermissions;
	}

	public void setLoginState(LoginState loginState) {
		LoginState old_loginState = this.loginState;
		this.loginState = loginState;
		if (loginState != old_loginState) {
			topicLoginState().publish(loginState);
		}
	}

	public void setOnlineState(OnlineState onlineState) {
		OnlineState old_onlineState = this.onlineState;
		this.onlineState = onlineState;
		if (onlineState != old_onlineState) {
			topicOnlineState().publish(onlineState);
		}
	}

	public void setOverrideAsOwnedObject(boolean overrideAsOwnedObject) {
		this.overrideAsOwnedObject = overrideAsOwnedObject;
	}

	public void setRoot(boolean root) {
		this.root = root;
	}

	public synchronized void setUser(IUser user) {
		root = false;
		invalidateGroupMap();
		this.user = user;
		if (this.user != null) {
			this.userId = user.getId();
		}
		if (user == null) {
			// do not fire listeners
			loginState = LoginState.NOT_LOGGED_IN;
		}
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public synchronized PermissionsManagerState snapshotState() {
		PermissionsManagerState state = new PermissionsManagerState();
		state.user = user;
		state.groupMap = groupMap == null ? null : new HashMap<>(groupMap);
		state.loginState = loginState;
		state.userId = userId;
		state.onlineState = onlineState;
		state.root = root;
		return state;
	}

	private int depth0() {
		return stateStack.size();
	}

	protected IUser getSystemUser() {
		return UserlandProvider.get().getSystemUser();
	}

	protected void invalidateGroupMap() {
		groupMap = null;
	}

	/*
	 * Overridden by threaded subclasses
	 */
	protected void removePerThreadContext0() {
	}

	@ClientInstantiable
	public enum LoginState {
		NOT_LOGGED_IN, LOGGED_IN
	}

	public enum OnlineState {
		OFFLINE, ONLINE
	}

	public static interface PermissionsExtension {
		public Boolean isPermitted(Object o, Permissible p);

		default Boolean isPermitted(Object o, Object assigningTo,
				Permissible p) {
			return isPermitted(o, p);
		}
	}

	@RegistryLocation(registryPoint = PermissionsExtensionForClass.class)
	@ClientInstantiable
	public static abstract class PermissionsExtensionForClass<C>
			implements PermissionsExtension {
		public abstract Class<C> getGenericClass();
	}

	@RegistryLocation(registryPoint = PermissionsExtensionForRule.class)
	@ClientInstantiable
	public static abstract class PermissionsExtensionForRule
			implements PermissionsExtension {
		public abstract String getRuleName();
	}

	public static class PermissionsManagerState {
		public IUser user;

		public HashMap<String, IGroup> groupMap;

		public LoginState loginState;

		public long userId;

		public OnlineState onlineState;

		public boolean root;

		public void copyTo(PermissionsManager pm) {
			pm.user = user;
			pm.groupMap = groupMap;
			pm.loginState = loginState;
			pm.userId = userId;
			pm.onlineState = onlineState;
			pm.root = root;
		}
	}

	public static class PermissionsState {
		public IUser user;

		public LoginState loginState;

		public boolean asRoot;

		public PermissionsState(IUser user, LoginState loginState,
				boolean asRoot) {
			this.user = user;
			this.loginState = loginState;
			this.asRoot = asRoot;
		}
	}

	/**
	 * <p>
	 * Note - make sure the environment is ready before instantiating i.e.
	 * servlet layer:
	 * </p>
	 * <code>
	 * ObjectPersistenceHelper.get();
		PermissionsManager.register(ThreadedPermissionsManager.tpmInstance());
		</code>
	 * 
	 * @author nick@alcina.cc
	 * 
	 */
	public static class RegistryPermissionsExtension
			implements PermissionsExtension {
		Map<Class, PermissionsExtensionForClass> extensionMapForClass = new HashMap<Class, PermissionsExtensionForClass>();

		Map<String, PermissionsExtensionForRule> extensionMapForRule = new HashMap<String, PermissionsExtensionForRule>();

		public RegistryPermissionsExtension(Registry registry) {
			try {
				List<Class> lookup = registry.lookup(false,
						PermissionsExtensionForClass.class, void.class, false);
				for (Class clazz : lookup) {
					PermissionsExtensionForClass ext = (PermissionsExtensionForClass) Reflections
							.classLookup().newInstance(clazz);
					extensionMapForClass.put(ext.getGenericClass(), ext);
				}
				lookup = registry.lookup(false,
						PermissionsExtensionForRule.class, void.class, false);
				for (Class clazz : lookup) {
					PermissionsExtensionForRule ext = (PermissionsExtensionForRule) Reflections
							.classLookup().newInstance(clazz);
					register(ext);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public PermissionsExtensionForRule getExtension(String ruleName) {
			return extensionMapForRule.get(ruleName);
		}

		@Override
		public Boolean isPermitted(Object o, Object assigningTo,
				Permissible p) {
			Class<? extends Object> clazz = o == null ? null : o.getClass();
			String ruleName = p != null ? p.rule() : "";
			if (extensionMapForClass.containsKey(clazz)) {
				return extensionMapForClass.get(clazz).isPermitted(o,
						assigningTo, p);
			}
			if (extensionMapForRule.containsKey(ruleName)) {
				PermissionsExtensionForRule extension = extensionMapForRule
						.get(ruleName);
				return extension.isPermitted(o, assigningTo, p);
			}
			return null;
		}

		@Override
		public Boolean isPermitted(Object o, Permissible p) {
			return isPermitted(o, null, p);
		}

		public void register(PermissionsExtensionForRule ext) {
			extensionMapForRule.put(ext.getRuleName(), ext);
		}
	}
}
