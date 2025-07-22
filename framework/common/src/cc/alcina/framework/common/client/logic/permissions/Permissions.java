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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Permission.SimplePermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StackDebug;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.common.client.util.Topic;

/**
 * <h2>Notes</h2>
 * <p>
 * Permissions type ADMIN_OR_OWNER pretty much mandates that the object
 * implement HasOwner
 * </p>
 * <p>
 * TODO - 2022 - make most methods static, call through to content instance.
 * Note that topics should be client only (and static)
 *
 * <p>
 * FIXME - dirndl 1x2 - move Permission.rule to a type, (Rule) - rule is the
 * name + the evaluator.
 * 
 * <p>
 * FIXME - possibly (probably) clientinstance should be a normal part of
 * context, but that'll take testing
 * 
 * Note re clientinstances and context - most of the time when pushing/popping,
 * the clientinstance will not be specified - rather just the user and/or root
 * state.
 * 
 * So if the pushed context has null clientinstance, and the current
 * clientinstance is non null, the null will effectively be replaced by the
 * current non-null client instance. Which implies that once clientInstance is
 * non null, the only way to null it is to pop the context
 *
 * @author Nick Reddel
 */
public class Permissions implements DomainTransformListener {
	@Reflected
	public enum LoginState {
		NOT_LOGGED_IN, LOGGED_IN
	}

	public static interface PermissionsExtension extends Registration.Ensure {
		default Boolean isPermitted(Object o, Object assigningTo,
				Permissible p) {
			return isPermitted(o, p);
		}

		public Boolean isPermitted(Object o, Permissible p);
	}

	@Reflected
	@Registration(PermissionsExtensionForRule.class)
	public static abstract class PermissionsExtensionForRule
			implements PermissionsExtension {
		public abstract String getRuleName();
	}

	public static class PermissionsContext {
		public static PermissionsContext root() {
			return new PermissionsContext(Permissions.get().getSystemUser(),
					LoginState.LOGGED_IN, true, null);
		}

		public PermissionsContext clone() {
			return new PermissionsContext(user, loginState, root,
					clientInstance);
		}

		public IUser user;

		public ClientInstance clientInstance;

		public boolean root;

		public LoginState loginState;

		public OnlineState onlineState;

		public Map<String, IGroup> groupMap;

		public PermissionsContext(IUser user, LoginState loginState,
				boolean root, ClientInstance clientInstance) {
			this.user = user;
			this.loginState = loginState;
			this.root = root;
			this.clientInstance = clientInstance;
		}

		public PermissionsContext() {
		}

		public void copyTo(Permissions pm) {
			pm.user = user;
			pm.groupMap = groupMap;
			pm.loginState = loginState;
			pm.onlineState = onlineState;
			pm.root = root;
			if (clientInstance != null) {
				pm.clientInstance = clientInstance;
			}
		}
	}

	/**
	 * <p>
	 * Note - make sure the environment is ready before instantiating i.e.
	 * servlet layer:
	 * </p>
	 * <code>
	 *  ObjectPersistenceHelper.get();
	 * 		Permissions.register(ThreadedPermissions.tpmInstance());
	 * 		</code>
	 *
	 *
	 */
	public static class RegistryPermissionsExtension
			implements PermissionsExtension {
		Map<String, PermissionsExtensionForRule> perNameRules = AlcinaCollections
				.newUnqiueMap();

		Map<Class<? extends Permissible>, Rule> perClassRules = AlcinaCollections
				.newUnqiueMap();

		public RegistryPermissionsExtension() {
			Registry.query(PermissionsExtensionForRule.class).implementations()
					.forEach(this::register);
		}

		public PermissionsExtensionForRule getExtension(String ruleName) {
			return perNameRules.get(ruleName);
		}

		@Override
		public Boolean isPermitted(Object target, Object assigningTo,
				Permissible p) {
			Class<? extends Object> targetClass = target == null ? null
					: target.getClass();
			String ruleName = p != null ? p.rule() : "";
			Class<? extends Permissible> permissibleClass = p != null
					? p.getClass()
					: null;
			if (permissibleClass != null) {
				Rule rule = perClassRules.computeIfAbsent(permissibleClass,
						clazz -> Registry.optional(Rule.class, permissibleClass)
								.orElse(null));
				if (rule != null) {
					return rule.isPermittedTyped(target, assigningTo, p);
				}
			}
			if (perNameRules.containsKey(ruleName)) {
				PermissionsExtensionForRule extension = perNameRules
						.get(ruleName);
				return extension.isPermitted(target, assigningTo, p);
			}
			return null;
		}

		@Override
		public Boolean isPermitted(Object o, Permissible p) {
			return isPermitted(o, null, p);
		}

		public void register(PermissionsExtensionForRule ext) {
			perNameRules.put(ext.getRuleName(), ext);
		}
	}

	public interface GetSystemUserClientInstance {
		ClientInstance getClientInstance();
	}

	public static class Names {
		public static String SYSTEM_GROUP = "system";

		public static String SYSTEM_USER = "system_user";

		public static String ANONYMOUS_USER = "anonymous";

		public static String ANONYMOUS_GROUP = "anonymous";

		public static String ADMINISTRATORS_GROUP = "Administrators";

		public static String INITIAL_ADMINISTRATOR_USER = "admin@nodomain.cc";

		public static String INITIAL_ADMINISTRATOR_PASSWORD = "admin";

		public static String DEVELOPERS_GROUP = "Developers";
	}

	private static Permissions factoryInstance;

	static PermissionsExtension permissionsExtension;

	public static void initialise() {
		permissionsExtension = new RegistryPermissionsExtension();
	}

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

	public static final String CONTEXT_CREATION_PARENT = Permissions.class
			.getName() + ".CONTEXT_CREATION_PARENT";

	public static StackDebug stackDebug = new StackDebug("Permissions");

	public static void pushSystemOrCurrentUserAsRoot() {
		get().pushSystemOrCurrentUserAsRoot0();
	}

	public static void
			runThrowingWithPushedSystemUserIfNeeded(ThrowingRunnable runnable) {
		try {
			if (isRoot()) {
				runnable.run();
			} else {
				try {
					pushSystemUser();
					runnable.run();
				} finally {
					popContext();
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void runWithPushedSystemUserIfNeeded(Runnable runnable) {
		try {
			if (isRoot()) {
				runnable.run();
			} else {
				try {
					pushSystemUser();
					runnable.run();
				} finally {
					popContext();
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void confirmDepth(int depth) {
		Preconditions.checkState(get().depth0() == depth);
	}

	public static Topic<LoginState> topicLoginStateChange() {
		return get().topicLoginStateChange;
	}

	public static int depth() {
		if (factoryInstance == null) {
			return 0;
		}
		return get().depth0();
	}

	public static Permissions get() {
		if (factoryInstance == null) {
			factoryInstance = new Permissions();
		}
		Permissions pm = factoryInstance.getPerThreadInstance();
		if (pm != null) {
			return pm;
		}
		return factoryInstance;
	}

	public static ObjectPermissions getObjectPermissions(Class<?> domainClass) {
		ObjectPermissions objectPermissions = Reflections.at(domainClass)
				.annotation(ObjectPermissions.class);
		return objectPermissions == null ? get().getDefaultObjectPermissions()
				: objectPermissions;
	}

	public static Set<IGroup> getReachableGroups(IUser user) {
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

	public static boolean hasAdminAccessLevel() {
		return get().getMaxPropertyAccessLevel().ordinal() >= AccessLevel.ADMIN
				.ordinal();
	}

	public static boolean hasDeletePermission(Object object) {
		AnnotationLocation clazzLocation = new AnnotationLocation(
				object.getClass(), null);
		ObjectPermissions op = clazzLocation
				.getAnnotation(ObjectPermissions.class);
		if (op == null) {
			return false;
		} else {
			return Permissions.isPermitted(object, op.delete());
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
			return Permissions.get().checkEffectivePropertyPermission(op, null,
					object, true);
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
			return Permissions.get().checkEffectivePropertyPermission(op, null,
					object, false);
		}
	}

	public static boolean isDeveloper() {
		if (!get().isLoggedIn()) {
			return false;
		} else {
			return get().isMemberOfGroup(Names.DEVELOPERS_GROUP);
		}
	}

	public static boolean isPermitted(Object o, String ruleName) {
		Permissible p = new Permissible() {
			@Override
			public AccessLevel accessLevel() {
				return AccessLevel.ROOT;
			}

			@Override
			public String rule() {
				return ruleName;
			}
		};
		return get().isPermitted(o, p, false);
	}

	/**
	 * For checking permissions when the type of o may have an @Permission
	 * annotation
	 * 
	 * @param o
	 * @param ruleName
	 * @return
	 */
	public static boolean isPermitted(Object o) {
		Permission permission = Reflections.at(o.getClass())
				.annotation(Permission.class);
		if (permission == null) {
			return true;
		}
		Permissible p = new AnnotatedPermissible(permission);
		return get().isPermitted(o, p, false);
	}

	public static boolean isSystemUser() {
		return Objects.equals(get().getSystemUser(), get().getUser());
	}

	public static void register(Permissions permissions) {
		factoryInstance = permissions;
	}

	public static void removePerThreadContext() {
		if (factoryInstance == null) {
			return;
		}
		factoryInstance.removePerThreadContext0();
	}

	public static void runAsUser(IUser user, ThrowingRunnable runnable) {
		try {
			Permissions.pushUser(user, LoginState.LOGGED_IN);
			ThrowingRunnable.asRunnable(runnable).run();
		} finally {
			Permissions.popContext();
		}
	}

	public static boolean isRoot() {
		return get().isRoot0();
	}

	public static void popContext() {
		get().popContext0();
	}

	public static void pushCurrentUser() {
		get().pushCurrentUser0();
	}

	public static IUser pushSystemUser() {
		return get().pushSystemUser0();
	}

	public static <T> T callWithPushedSystemUserIfNeeded(Callable<T> callable)
			throws Exception {
		if (isRoot()) {
			return callable.call();
		} else {
			try {
				pushSystemUser();
				return callable.call();
			} finally {
				popContext();
			}
		}
	}

	public static <T> T
			callWithPushedSystemUserIfNeededNoThrow(Callable<T> callable) {
		try {
			if (isRoot()) {
				return callable.call();
			} else {
				try {
					pushSystemUser();
					return callable.call();
				} finally {
					popContext();
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void pushUser(IUser user, LoginState loginState) {
		pushUser(user, loginState, false, null);
	}

	public static void pushUser(IUser user, LoginState loginState, boolean root,
			ClientInstance clientInstance) {
		get().pushContext(user, loginState, root, clientInstance);
	}

	public static boolean hasContext() {
		return depth() != 0;
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

	protected Topic<ClientInstance> topicClientInstanceChange = Topic.create();

	private Topic<LoginState> topicLoginStateChange = Topic.create();

	private LoginState loginState = LoginState.NOT_LOGGED_IN;

	private OnlineState onlineState = OnlineState.ONLINE;

	private IUser user;

	private ClientInstance clientInstance;

	private Map<String, IGroup> groupMap;

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

	protected Stack<PermissionsContext> contextStack = new Stack<>();

	private boolean root;

	protected Permissions() {
		super();
	}

	public void appShutdown() {
		factoryInstance = null;
	}

	public boolean checkEffectivePropertyPermission(Object bean,
			String propertyName, boolean read) {
		Class<? extends Object> clazz = bean.getClass();
		ObjectPermissions op = Reflections.at(clazz)
				.annotation(ObjectPermissions.class);
		PropertyPermissions pp = Reflections.at(clazz).property(propertyName)
				.annotation(PropertyPermissions.class);
		return checkEffectivePropertyPermission(op, pp, bean, read);
	}

	public boolean checkEffectivePropertyPermission(ObjectPermissions op,
			PropertyPermissions pp, Object bean, boolean read) {
		op = op == null ? Permissions.get().getDefaultObjectPermissions() : op;
		if (pp == null && !Permissions.isPermitted(bean,
				read ? op.read() : op.write())) {
			return false;
		}
		if (op != null && pp == null) {
			// assume defined object permissions
			// define read/write better than
			// property defaults
			return true;
		} else {
			pp = pp == null ? getDefaultPropertyPermissions() : pp;
			return isPermitted(bean, read ? pp.read() : pp.write());
		}
	}

	public boolean checkReadable(Class<?> clazz, String propertyName,
			Object bean) {
		ClassReflector<?> reflector = Reflections.at(clazz);
		ObjectPermissions op = reflector.annotation(ObjectPermissions.class);
		PropertyPermissions pp = reflector.property(propertyName)
				.annotation(PropertyPermissions.class);
		return Permissions.get().checkEffectivePropertyPermission(op, pp,
				bean == null ? reflector.templateInstance() : bean, true);
	}

	@Override
	public void domainTransform(DomainTransformEvent evt)
			throws DomainTransformException {
		if (evt.getSource() instanceof IGroup) {
			invalidateGroupMap();
		}
	}

	/**
	 * @see cc.alcina.framework.common.client.logic.domaintransform.ClientInstance.self()
	 */
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
		Set<IGroup> groups = getReachableGroups(user);
		groupMap = new HashMap<String, IGroup>();
		for (Iterator<IGroup> itr = groups.iterator(); itr.hasNext();) {
			IGroup group = itr.next();
			groupMap.put(group.getName(), group);
		}
		Map<String, IGroup> result = groupMap;
		if (user != this.user) {
			invalidateGroupMap();
		}
		return result;
	}

	public long getUserId() {
		return getUser() == null ? 0L : getUser().getId();
	}

	public String getUserName() {
		return getUser() == null ? null : getUser().getUserName();
	}

	public String getUserString() {
		return Ax.format("%s/%s", getUserId(), getUserName());
	}

	public boolean isAdmin() {
		if (!isLoggedIn()) {
			return false;
		} else {
			return isMemberOfGroup(Names.ADMINISTRATORS_GROUP);
		}
	}

	public boolean isAnonymousUser() {
		return getUser() == null || Names.ANONYMOUS_USER.equals(getUserName());
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

	public static boolean isPermitted(Object o, Object assigningTo,
			Permissible p, boolean doNotEvaluateNullObjectPermissions) {
		return get().isPermitted0(o, assigningTo, p,
				doNotEvaluateNullObjectPermissions);
	}

	protected boolean isPermitted0(Object o, Object assigningTo, Permissible p,
			boolean doNotEvaluateNullObjectPermissions) {
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
				permitted |= Objects.equals(Permissions.get().getUser(), o);
			}
		}
		if (!permitted && !doNotEvaluateNullObjectPermissions) {
			permitted = evaluateRules(o, assigningTo, p, permitted);
		}
		return permitted;
	}

	public static boolean isPermitted(Object o, Permissible p) {
		return isPermitted(o, p, false);
	}

	public static boolean isPermitted(Object o, Permissible p,
			boolean doNotEvaluateNullObjectPermissions) {
		return get().isPermitted0(o, null, p,
				doNotEvaluateNullObjectPermissions);
	}

	public static boolean isPermitted(Object o, Permission p) {
		return isPermitted(o, new AnnotatedPermissible(p));
	}

	public static boolean isPermitted(Permissible p) {
		return isPermitted(null, p);
	}

	// pretty much only for create (or explicit) permi8ssion checks (all others
	// may be
	// object-dependent)
	public static boolean isPermitted(Permission nullTargetPermission) {
		return isPermitted(new AnnotatedPermissible(nullTargetPermission));
	}

	public boolean isPermittedClass(Object object,
			Permission defaultPermission) {
		if (object instanceof Permissible) {
			return isPermitted((Permissible) object);
		}
		Permission permission = Reflections.at(object)
				.annotation(Permission.class);
		return isPermitted(permission != null ? permission : defaultPermission);
	}

	public boolean permitDueToOwnership(HasOwner hasOwner) {
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

	public PermissionsContext toPermissionsContext() {
		return new PermissionsContext(getUser(), getLoginState(), isRoot(),
				getClientInstance());
	}

	// This should never be necessary, if the code always surrounds user
	// push/pop in try/finally...but...
	public void reset() {
		contextStack.clear();
		setRoot(false);
		setUser(null);
		setLoginState(LoginState.NOT_LOGGED_IN);
	}

	public PermissionsContext snapshotContext() {
		PermissionsContext context = new PermissionsContext();
		context.user = user;
		context.groupMap = groupMap == null ? null : new HashMap<>(groupMap);
		context.loginState = loginState;
		context.onlineState = onlineState;
		context.root = root;
		return context;
	}

	public static void pushContext(PermissionsContext newContext) {
		get().pushContext0(newContext);
	}

	void pushContext0(PermissionsContext newContext) {
		stackDebug.maybeDebugStack(contextStack, true);
		PermissionsContext currentContext = toPermissionsContext();
		contextStack.push(currentContext);
		applyContext(newContext);
	}

	/*
	 * Clients need a base context, since event/js entry doesn't go through a
	 * permissions layer. This call will fail unless the Permissions context
	 * stack is empty
	 */
	public void replaceContext(PermissionsContext baseContext) {
		replaceContext(baseContext, true);
	}

	protected void replaceContext(PermissionsContext context,
			boolean baseOnly) {
		Preconditions.checkState(!baseOnly || !hasContext());
		applyContext(context);
	}

	protected void setClientInstance(ClientInstance clientInstance) {
		ClientInstance old_clientInstance = this.clientInstance;
		this.clientInstance = clientInstance;
		if (!Objects.equals(clientInstance, old_clientInstance)) {
			topicClientInstanceChange.publish(clientInstance);
		}
	}

	protected void setLoginState(LoginState loginState) {
		LoginState old_loginState = this.loginState;
		this.loginState = loginState;
		if (loginState != old_loginState) {
			topicLoginStateChange().publish(loginState);
		}
	}

	protected void setRoot(boolean root) {
		this.root = root;
	}

	protected void setUser(IUser user) {
		invalidateGroupMap();
		this.user = user;
		if (user == null) {
			// do not fire listeners
			loginState = LoginState.NOT_LOGGED_IN;
		}
	}

	protected void pushSystemOrCurrentUserAsRoot0() {
		if (isLoggedIn() || getSystemUser() == null) {
			pushUser(getUser(), getLoginState(), true, null);
		} else {
			pushSystemUser();
		}
	}

	protected Permissions getPerThreadInstance() {
		return null;
	}

	protected boolean isRoot0() {
		return root;
	}

	protected void popContext0() {
		stackDebug.maybeDebugStack(contextStack, false);
		IUser currentUser = getUser();
		PermissionsContext context = contextStack.pop();
		setLoginState(context.loginState);
		setUser(context.user);
		setRoot(context.root);
		setClientInstance(context.clientInstance);
	}

	protected void pushCurrentUser0() {
		pushUser(getUser(), getLoginState(), isRoot(), getClientInstance());
	}

	protected IUser pushSystemUser0() {
		IUser systemUser = getSystemUser();
		pushContext(systemUser, LoginState.LOGGED_IN, true, Registry
				.impl(GetSystemUserClientInstance.class).getClientInstance());
		return systemUser;
	}

	/*
	 * Note that in some cases user !=clientInstance.getUser - basically if
	 * running with the permissions of user X but recording domain mutation to
	 * the graph of (server) clientinstance Y
	 */
	protected void pushContext(IUser user, LoginState loginState, boolean root,
			ClientInstance clientInstance) {
		pushContext(
				new PermissionsContext(user, loginState, root, clientInstance));
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

	protected void applyContext(PermissionsContext context) {
		if (context.clientInstance != null) {
			setClientInstance(context.clientInstance);
		}
		setLoginState(context.loginState);
		setRoot(context.root);
		setUser(context.user);
	}

	private int depth0() {
		return contextStack.size();
	}

	boolean evaluateRules(Object o, Object assigningTo, Permissible p,
			boolean permitted) {
		Boolean b = null;
		if (assigningTo != null) {
			b = permissionsExtension.isPermitted(o, assigningTo, p);
		} else {
			b = permissionsExtension.isPermitted(o, p);
		}
		if (b != null) {
			permitted = b;
		}
		return permitted;
	}
}
