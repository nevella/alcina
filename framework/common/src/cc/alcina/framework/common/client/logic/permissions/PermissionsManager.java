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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.Vetoer;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Permission.SimplePermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

@SuppressWarnings("unchecked")
/**
 *<h2>Notes</h2>
 *<p>Permissions type ADMIN_OR_OWNER pretty much mandates that the object implement HasOwner</p>
 * @author Nick Reddel
 */
public class PermissionsManager extends BaseBindable implements Vetoer,
		DomainTransformListener {
	private LoginState loginState = LoginState.NOT_LOGGED_IN;

	private OnlineState onlineState = OnlineState.ONLINE;

	public static final String PROP_LOGIN_STATE = "loginState";

	public static final String PROP_ONLINE_STATE = "onlineState";

	private long userId;

	private static String administratorGroupName = "Administrators";

	private static String developerGroupName = "Developers";

	private PropertyChangeListener userListener;

	private static PermissionsManager theInstance;

	public static PermissionsManager get() {
		if (theInstance == null) {
			theInstance = new PermissionsManager();
		}
		PermissionsManager pm = theInstance.getT();
		if (pm != null) {
			return pm;
		}
		return theInstance;
	}

	public static void register(PermissionsManager pm) {
		theInstance = pm;
	}

	private IUser user;

	private IUser instantiatedUser;

	protected IUser getInstantiatedUser() {
		return this.instantiatedUser;
	}

	protected void setInstantiatedUser(IUser instantiatedUser) {
		this.instantiatedUser = instantiatedUser;
	}

	private HashMap<String, IGroup> groupMap;

	private PropertyPermissions defaultPropertyPermissions = new PropertyPermissions() {
		public Class<? extends Annotation> annotationType() {
			return PropertyPermissions.class;
		}

		public Permission read() {
			return SimplePermissions.getPermission(AccessLevel.EVERYONE);
		}

		public Permission write() {
			return SimplePermissions.getPermission(AccessLevel.ADMIN_OR_OWNER);
		}
	};

	private ObjectPermissions defaultObjectPermissions = new ObjectPermissions() {
		public Class<? extends Annotation> annotationType() {
			return ObjectPermissions.class;
		}

		public Permission create() {
			return SimplePermissions.getPermission(AccessLevel.ROOT);
		}

		public Permission delete() {
			return SimplePermissions.getPermission(AccessLevel.ROOT);
		}

		public Permission read() {
			return SimplePermissions.getPermission(AccessLevel.ADMIN_OR_OWNER);
		}

		public Permission write() {
			return SimplePermissions.getPermission(AccessLevel.ADMIN_OR_OWNER);
		}
	};

	protected Stack<IUser> userStack = new Stack<IUser>();

	protected Stack<LoginState> stateStack = new Stack<LoginState>();

	private static PermissionsExtension permissionsExtension;

	protected PermissionsManager() {
		super();
		this.userListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				groupMap = null;
			}
		};
	}

	public void appShutdown() {
		theInstance = null;
	}

	public boolean checkEffectivePropertyPermission(Object bean,
			String propertyName, boolean read) {
		Class<? extends Object> clazz = bean.getClass();
		ObjectPermissions op = CommonLocator.get().classLookup()
				.getAnnotationForClass(clazz, ObjectPermissions.class);
		PropertyPermissions pp = CommonLocator
				.get()
				.propertyAccessor()
				.getAnnotationForProperty(clazz, PropertyPermissions.class,
						propertyName);
		return checkEffectivePropertyPermission(op, pp, bean, read);
	}

	public boolean checkEffectivePropertyPermission(ObjectPermissions op,
			PropertyPermissions pp, Object bean, boolean read) {
		op = op == null ? PermissionsManager.get()
				.getDefaultObjectPermissions() : op;
		if (pp == null
				&& !PermissionsManager.get().isPermissible(bean,
						read ? op.read() : op.write())) {
			return false;
		}
		if (op != null && pp == null) {// assume defined object permissions
			// define read/write better than
			// property defaults
			return true;
		}
		pp = pp == null ? getDefaultPropertyPermissions() : pp;
		return isPermissible(bean, read ? pp.read() : pp.write());
	}

	public boolean checkReadable(Class clazz, String propertyName, Object bean) {
		ClassLookup classLookup = CommonLocator.get().classLookup();
		PropertyAccessor propertyAccessor = CommonLocator.get()
				.propertyAccessor();
		ObjectPermissions op = classLookup.getAnnotationForClass(clazz,
				ObjectPermissions.class);
		PropertyPermissions pp = propertyAccessor.getAnnotationForProperty(
				clazz, PropertyPermissions.class, propertyName);
		return PermissionsManager.get().checkEffectivePropertyPermission(op,
				pp,
				bean == null ? classLookup.getTemplateInstance(clazz) : bean,
				true);
	}

	public void copyTo(PermissionsManager newThreadInstance) {
		newThreadInstance.user = user;
		newThreadInstance.groupMap = groupMap;
		newThreadInstance.loginState = loginState;
		newThreadInstance.userId = userId;
		newThreadInstance.onlineState = onlineState;
	}

	public void domainTransform(DomainTransformEvent evt)
			throws DomainTransformException {
		if (evt.getSource() instanceof IGroup) {
			groupMap = null;
		}
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

	public static PermissionsExtension getPermissionsExtension() {
		return permissionsExtension;
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
		if (groupMap != null) {
			return groupMap;
		}
		Set<IGroup> groups = new HashSet<IGroup>();
		if (user != null) {
			if (user.getPrimaryGroup() != null) {
				groups.add(user.getPrimaryGroup());
			}
			groups.addAll(user.getSecondaryGroups());
			recursivePopulateGroupMemberships(groups, new HashSet<IGroup>());
		}
		groupMap = new HashMap<String, IGroup>();
		for (Iterator<IGroup> itr = groups.iterator(); itr.hasNext();) {
			IGroup group = itr.next();
			groupMap.put(group.getName(), group);
		}
		return groupMap;
	}

	public long getUserId() {
		return this.userId;
	}

	public String getUserName() {
		return (getUser() == null) ? null : getUser().getUserName();
	}

	public boolean isAdmin() {
		if (getAdministratorGroupName() == null || !isLoggedIn()) {
			return false;
		} else {
			return isMemberOfGroup(getAdministratorGroupName());
		}
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

	public boolean isMemberOfGroup(String groupName) {
		return getUserGroups().containsKey(groupName);
	}
	public boolean isMemberOfGroups(Collection<String> groupNames) {
		for (String groupName : groupNames) {
			if(isMemberOfGroup(groupName)){
				return true;
			}
		}
		return false;
	}

	private boolean allPermissible = false;

	public static final Permissible ROOT_PERMISSIBLE = new Permissible() {
		public AccessLevel accessLevel() {
			return AccessLevel.ROOT;
		}

		public String rule() {
			return null;
		}
	};

	public static final Permissible ADMIN_PERMISSIBLE = new Permissible() {
		public AccessLevel accessLevel() {
			return AccessLevel.ADMIN;
		}

		public String rule() {
			return null;
		}
	};

	public boolean isPermissible(Object o, Permissible p) {
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
					permitted |= isOwnerOf((HasOwner) o);
				}
			}
		}
		if (!permitted) {
			if (getPermissionsExtension() != null) {
				Boolean b = getPermissionsExtension().isPermitted(o, p);
				if (b != null) {
					permitted = b;
				}
			}
		}
		return permitted;
	}

	public boolean isOwnerOf(HasOwner hasOwner) {
		return (hasOwner.getOwner() == null || hasOwner.getOwner().equals(
				instantiatedUser));
	}

	public boolean isPermissible(Object o, Permission p) {
		return isPermissible(o, new AnnotatedPermissible(p));
	}

	public boolean isPermissible(Permissible p) {
		return isPermissible(null, p);
	}

	public IUser popUser() {
		if (userStack.size() == 0) {
			setLoginState(LoginState.NOT_LOGGED_IN);
			return getUser();
		}
		setLoginState(stateStack.pop());
		IUser poppedUser = userStack.pop();
		IUser currentUser = getUser();
		setUser(poppedUser);
		return currentUser;
	}

	public void prepareVersionable(IVersionable u) {
		Date now = new Date();
		if (u.getLastModificationDate() == null) {
			u.setCreationUser(getUser());
			u.setCreationDate(now);
		}
		u.setLastModificationUser(getUser());
		u.setLastModificationDate(now);
	}

	public void pushUser(IUser user, LoginState loginState) {
		if (getUser() != null) {
			userStack.push(getUser());
			stateStack.push(getLoginState());
		}
		setLoginState(loginState);
		setUser(user);
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
		propertyChangeSupport().firePropertyChange("loginState", old_loginState,
				loginState);
	}

	public void setOnlineState(OnlineState onlineState) {
		OnlineState old_onlineState = this.onlineState;
		this.onlineState = onlineState;
		propertyChangeSupport().firePropertyChange("onlineState",
				old_onlineState, onlineState);
	}

	public static void setPermissionsExtension(
			PermissionsExtension permissionsExtension) {
		PermissionsManager.permissionsExtension = permissionsExtension;
	}

	public void setUser(IUser user) {
		groupMap = null;
		if (this.user != null
				&& this.user instanceof SourcesPropertyChangeEvents) {
			SourcesPropertyChangeEvents spce = (SourcesPropertyChangeEvents) user;
			try {
				spce.removePropertyChangeListener(userListener);
			} catch (Exception e) {
				// nullpointer somewhere
			}
		}
		this.user = user;
		this.instantiatedUser = user;
		if (this.user != null) {
			this.userId = user.getId();
		}
		if (this.user != null
				&& this.user instanceof SourcesPropertyChangeEvents) {
			SourcesPropertyChangeEvents spce = (SourcesPropertyChangeEvents) user;
			spce.addPropertyChangeListener(userListener);
		}
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public boolean veto(Object object) {
		PermissionsManager pmLocal = PermissionsManager.get();
		if (pmLocal != this) {
			return pmLocal.veto(object);
		}
		if (!(object instanceof Permissible)) {
			throw new WrappedRuntimeException(
					"Object not instance of permissible",
					SuggestedAction.NOTIFY_WARNING);
		}
		return !isPermissible((Permissible) object);
	}

	protected boolean isRoot() {
		return false;
	}

	protected void recursivePopulateGroupMemberships(Set<IGroup> members,
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

	public void setAllPermissible(boolean allPermissible) {
		this.allPermissible = allPermissible;
	}

	public boolean isAllPermissible() {
		return allPermissible;
	}

	public static void setAdministratorGroupName(String administratorGroupName) {
		PermissionsManager.administratorGroupName = administratorGroupName;
	}

	public static String getAdministratorGroupName() {
		return administratorGroupName;
	}

	public static void setDeveloperGroupName(String developerGroupName) {
		PermissionsManager.developerGroupName = developerGroupName;
	}

	public static String getDeveloperGroupName() {
		return developerGroupName;
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
	}

	@RegistryLocation(registryPoint = PermissionsExtensionForClass.class, j2seOnly = false)
	@ClientInstantiable
	public static abstract class PermissionsExtensionForClass<C> implements
			PermissionsExtension {
		public abstract Class<C> getGenericClass();
	}

	@RegistryLocation(registryPoint = PermissionsExtensionForRule.class, j2seOnly = false)
	@ClientInstantiable
	public static abstract class PermissionsExtensionForRule implements
			PermissionsExtension {
		public abstract String getRuleName();
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
	public static class RegistryPermissionsExtension implements
			PermissionsExtension {
		Map<Class, PermissionsExtensionForClass> extensionMapForClass = new HashMap<Class, PermissionsExtensionForClass>();

		Map<String, PermissionsExtensionForRule> extensionMapForRule = new HashMap<String, PermissionsExtensionForRule>();

		public RegistryPermissionsExtension(Registry registry) {
			try {
				List<Class> lookup = registry.lookup(false,
						PermissionsExtensionForClass.class, void.class, false);
				for (Class clazz : lookup) {
					PermissionsExtensionForClass ext = (PermissionsExtensionForClass) CommonLocator
							.get().classLookup().newInstance(clazz);
					extensionMapForClass.put(ext.getGenericClass(), ext);
				}
				lookup = registry.lookup(false,
						PermissionsExtensionForRule.class, void.class, false);
				for (Class clazz : lookup) {
					PermissionsExtensionForRule ext = (PermissionsExtensionForRule) CommonLocator
							.get().classLookup().newInstance(clazz);
					extensionMapForRule.put(ext.getRuleName(), ext);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public Boolean isPermitted(Object o, Permissible p) {
			Class<? extends Object> clazz = o == null ? null : o.getClass();
			String ruleName = p != null ? p.rule() : "";
			if (extensionMapForClass.containsKey(clazz)) {
				return extensionMapForClass.get(clazz).isPermitted(o, p);
			}
			if (extensionMapForRule.containsKey(ruleName)) {
				return extensionMapForRule.get(ruleName).isPermitted(o, p);
			}
			return null;
		}
	}
}
