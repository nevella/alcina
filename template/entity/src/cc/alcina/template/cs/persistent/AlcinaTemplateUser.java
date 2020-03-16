package cc.alcina.template.cs.persistent;

// Generated Sep 20, 2008 12:40:03 PM by Hibernate Tools 3.2.2.GA
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;

import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cc.alcina.framework.common.client.actions.instances.ChangePasswordClientAction;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.gwittir.validator.EmailAddressValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionableOwnable;
import cc.alcina.framework.common.client.logic.reflection.Action;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectActions;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.gwt.client.gwittir.customiser.SelectorCustomiser;
import cc.alcina.template.cs.constants.AlcinaTemplateSiteConstants;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

@javax.persistence.Entity
@Table(name = "users", schema = "public")
@SequenceGenerator(allocationSize=1,name = "users_id_seq", sequenceName = "users_id_seq")
@Bean(actions = @ObjectActions( {
		@Action(actionClass = ViewAction.class),
		@Action(actionClass = EditAction.class),
		@Action(actionClass = CreateAction.class),
		@Action(actionClass = DeleteAction.class),
		@Action(actionClass = ChangePasswordClientAction.class, permission = @Permission(access = AccessLevel.ADMIN_OR_OWNER)) }), displayNamePropertyName = "userName")
@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ADMIN))

@Introspectable
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = IUser.class)
public class AlcinaTemplateUser extends DomainBaseVersionable implements IUser,
		Comparable<AlcinaTemplateUser>, IVersionableOwnable {
	private long id;

	private String userName;

	private String password;

	private String firstName;

	private Boolean system;

	private String salt;

	private String lastName;

	private Date lastLogin;

	private String passwordRecovery;

	private Boolean deleted;

	private String rememberMeCookie;

	AlcinaTemplateGroup primaryGroup;

	Set<AlcinaTemplateGroup> secondaryGroups = new HashSet<AlcinaTemplateGroup>();

	Set<IidImpl> iids;

	public AlcinaTemplateUser() {
	}

	public AlcinaTemplateUser(long id) {
		this.id = id;
	}

	public int compareTo(AlcinaTemplateUser o) {
		return _compareTo(o);
	}

	@Override
	protected String comparisonString() {
		if (comparisonString != null) {
			return comparisonString;
		}
		comparisonString = getUserName() == null ? null : getUserName()
				.toLowerCase();
		return comparisonString;
	}

	public String friendlyName() {
		if (getFirstName() != null || getLastName() != null) {
			String s = getFirstName();
			s = s == null ? "" : s + " ";
			s += (getLastName() == null) ? "" : getLastName();
		}
		return getUserName();
	}

	@Column(name = "deleted")
	@DisplayInfo(name = "Deleted", orderingHint = 210)
	@PropertyPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN))
	public Boolean getDeleted() {
		return this.deleted;
	}

	/**
	 * Compatibility
	 * 
	 * @see cc.alcina.framework.common.client.logic.permissions.IUser#getEmail()
	 */
	@Transient
	public String getEmail() {
		String un = getUserName();
		if (un == null || un.contains("@")) {
			return un;
		}
		return un + AlcinaTemplateSiteConstants.DOMAIN;
	}

	@Column(name = "first_name")
	@DisplayInfo(name = "First name", orderingHint = 40)
	@PropertyPermissions(read = @Permission(access = AccessLevel.ADMIN_OR_OWNER), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER))
	public String getFirstName() {
		return this.firstName;
	}

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(generator = "users_id_seq")
	@XmlElement(name = "id")
	public long getId() {
		return this.id;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "rememberMeUser")
	@XmlTransient
	public Set<IidImpl> getIids() {
		return this.iids;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_login", length = 29)
	@DisplayInfo(name = "Last login", orderingHint = 710)
	@PropertyPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ROOT))
	public Date getLastLogin() {
		return this.lastLogin;
	}

	@Column(name = "last_name")
	@DisplayInfo(name = "Last name", orderingHint = 50)
	@PropertyPermissions(read = @Permission(access = AccessLevel.ADMIN_OR_OWNER), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER))
	public String getLastName() {
		return this.lastName;
	}

	@Transient
	@XmlTransient
	public IUser getOwner() {
		return this;
	}

	@Transient
	@XmlTransient
	public IGroup getOwnerGroup() {
		// not required
		return null;
	}

	@Column(name = "password")
	@DisplayInfo(name = "Password")
	@PropertyPermissions(read = @Permission(access = AccessLevel.ROOT), write = @Permission(access = AccessLevel.ROOT))
	public String getPassword() {
		return this.password;
	}

	@Transient
	public String getPasswordHash() {
		return getPassword();
	}

	@Column(name = "password_recovery")
	public String getPasswordRecovery() {
		return this.passwordRecovery;
	}

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = AlcinaTemplateGroup.class)
	@JoinColumn(name = "primary_group_id")
	xeroId
	@XmlTransient
	// unused - for the moment
	public IGroup getPrimaryGroup() {
		return (AlcinaTemplateGroup) this.primaryGroup;
	}

	@Column(name = "remember_me_cookie")
	public String getRememberMeCookie() {
		return this.rememberMeCookie;
	}

	public String getSalt() {
		return salt;
	}

	@ManyToMany(mappedBy = "memberUsers", targetEntity = AlcinaTemplateGroup.class)
	@DisplayInfo(name = "Groups", orderingHint = 96)
	@Association(implementationClass = AlcinaTemplateGroup.class, propertyName = "memberUsers")
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER))
	@Custom(customiserClass = SelectorCustomiser.class)
	xeroId
	@XmlTransient
	public Set<AlcinaTemplateGroup> getSecondaryGroups() {
		return (Set<AlcinaTemplateGroup>) this.secondaryGroups;
	}

	@Column(name = "username")
	@DisplayInfo(name = "User name (Email)", orderingHint = 1)
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER))
	@Validators(validators = {
			@Validator(validator = EmailAddressValidator.class),
			@Validator(validator = ServerUniquenessValidator.class, parameters = {
					@NamedParameter(name = ServerUniquenessValidator.OBJECT_CLASS, classValue = AlcinaTemplateUser.class),
					@NamedParameter(name = ServerUniquenessValidator.PROPERTY_NAME, stringValue = "username"),
					@NamedParameter(name = Validator.FEEDBACK_MESSAGE, stringValue = "This email address is in use") }) })
	public String getUserName() {
		return this.userName;
	}

	public void setDeleted(Boolean deleted) {
		Boolean old_deleted = this.deleted;
		this.deleted = deleted;
		propertyChangeSupport().firePropertyChange("deleted", old_deleted,
				deleted);
	}

	public void setFirstName(String firstName) {
		String old_firstName = this.firstName;
		this.firstName = firstName;
		propertyChangeSupport().firePropertyChange("firstName", old_firstName,
				firstName);
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setIids(Set<IidImpl> iids) {
		this.iids = iids;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public void setLastName(String lastName) {
		String old_lastName = this.lastName;
		this.lastName = lastName;
		propertyChangeSupport().firePropertyChange("lastName", old_lastName,
				lastName);
	}

	public void setOwner(IUser owner) {
		// not required
	}

	public void setOwnerGroup(IGroup ownerGroup) {
		// not required
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setPasswordRecovery(String passwordRecovery) {
		this.passwordRecovery = passwordRecovery;
	}

	public void setPrimaryGroup(IGroup primaryGroup) {
		IGroup old_primaryGroup = this.primaryGroup;
		this.primaryGroup = (AlcinaTemplateGroup) primaryGroup;
		propertyChangeSupport().firePropertyChange("primaryGroup",
				old_primaryGroup, primaryGroup);
	}

	public void setRememberMeCookie(String rememberMeCookie) {
		this.rememberMeCookie = rememberMeCookie;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	@SuppressWarnings("unchecked")
	public void setSecondaryGroups(Set<? extends IGroup> secondaryGroups) {
		Set<? extends IGroup> old_secondaryGroups = this.secondaryGroups;
		this.secondaryGroups = (Set<AlcinaTemplateGroup>) secondaryGroups;
		propertyChangeSupport().firePropertyChange("secondaryGroups",
				old_secondaryGroups, secondaryGroups);
	}

	public void setUserName(String userName) {
		String old_userName = this.userName;
		this.userName = userName;
		propertyChangeSupport().firePropertyChange("userName", old_userName,
				userName);
		comparisonString = userName == null ? null : userName.toLowerCase();
	}

	public void setSystem(Boolean system) {
		Boolean old_system = this.system;
		this.system = system;
		propertyChangeSupport().firePropertyChange("system", old_system, system);
	}

	@DisplayInfo(name = "System", orderingHint = 1010)
	@PropertyPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.DEVELOPER))
	public Boolean getSystem() {
		return system;
	}
}
