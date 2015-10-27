package cc.alcina.template.cs.persistent;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasOwner;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.Action;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectActions;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.customiser.UrlCustomiser;

@Entity
@Table(name = "bookmark", schema = "public")
@BeanInfo(displayNamePropertyName = "title", actions = @ObjectActions({
		@Action(actionClass = ViewAction.class),
		@Action(actionClass = EditAction.class),
		@Action(actionClass = CreateAction.class),
		@Action(actionClass = DeleteAction.class) }))
@SequenceGenerator(allocationSize = 1, name = "bookmark_id_seq", sequenceName = "bookmark_id_seq")
@ObjectPermissions(create = @Permission(access = AccessLevel.LOGGED_IN), read = @Permission(access = AccessLevel.ADMIN_OR_OWNER), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER), delete = @Permission(access = AccessLevel.ADMIN_OR_OWNER))
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class Bookmark extends DomainBaseVersionable implements
		Comparable<Bookmark>, HasOwner {
	private long id;

	private String title;

	private String url;

	private Bookmark parent;

	private Set<Bookmark> children = new LinkedHashSet<Bookmark>();

	private AlcinaTemplateUser user;

	public Bookmark() {
	}

	public Bookmark(long id) {
		this.id = id;
	}

	public int compareTo(Bookmark o) {
		return CommonUtils.compareWithNullMinusOne(getTitle(), o.getTitle());
	}

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "parent")
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Children", orderingHint = 10, displayMask = DisplayInfo.DISPLAY_AS_TREE_NODE
			| DisplayInfo.DISPLAY_AS_TREE_NODE_WITHOUT_CONTAINER))
	@Association(implementationClass = Bookmark.class, propertyName = "parent")
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<Bookmark> getChildren() {
		return children;
	}

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(generator = "bookmark_id_seq")
	public long getId() {
		return this.id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@DisplayInfo(name = "Parent", orderingHint = 25)
	@Association(implementationClass = Bookmark.class, propertyName = "children")
	public Bookmark getParent() {
		return this.parent;
	}

	@DisplayInfo(name = "Title", orderingHint = 15)
	public String getTitle() {
		return title;
	}

	@DisplayInfo(name = "URL", orderingHint = 20)
	@CustomiserInfo(customiserClass = UrlCustomiser.class, parameters = { @NamedParameter(name = UrlCustomiser.TARGET, stringValue = "_blank") })
	public String getUrl() {
		return url;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public AlcinaTemplateUser getUser() {
		return user;
	}

	public void setChildren(Set<Bookmark> children) {
		Set<Bookmark> old_children = this.children;
		this.children = children;
		propertyChangeSupport().firePropertyChange("children", old_children,
				children);
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setParent(Bookmark parent) {
		Bookmark old_parent = this.parent;
		this.parent = parent;
		propertyChangeSupport()
				.firePropertyChange("parent", old_parent, parent);
	}

	public void setTitle(String title) {
		String old_title = this.title;
		this.title = title;
		propertyChangeSupport().firePropertyChange("title", old_title, title);
	}

	public void setUrl(String url) {
		String old_url = this.url;
		this.url = url;
		propertyChangeSupport().firePropertyChange("url", old_url, url);
	}

	public void setUser(AlcinaTemplateUser user) {
		AlcinaTemplateUser old_user = this.user;
		this.user = user;
		propertyChangeSupport().firePropertyChange("user", old_user, user);
	}

	@Override
	@Transient
	public IUser getOwner() {
		return getUser();
	}
}
