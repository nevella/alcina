package cc.alcina.framework.entity.entityaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;

@MappedSuperclass
@DomainTransformPersistable
@RegistryLocation(registryPoint = KnownNodePersistentDomainStore.class)
public abstract class KnownNodePersistentDomainStore extends
		Entity<KnownNodePersistentDomainStore> implements IVersionable,KnownNodePersistent {
	protected long id;

	private Set<KnownNodePersistentDomainStore> children=new LiSet<>();

	private String name;

	private String properties;

	private KnownNodePersistentDomainStore parent;

	public KnownNodePersistentDomainStore() {
	}

	public KnownNodePersistentDomainStore(String name) {
		setName(name);
	}

	@Transient
	public Set<KnownNodePersistentDomainStore> getChildren() {
		return this.children;
	}

	public String getName() {
		return this.name;
	}

	@Transient
	public KnownNodePersistentDomainStore getParent() {
		return this.parent;
	}

	@Transient
	@Lob
	public String getProperties() {
		return this.properties;
	}


	public void setChildren(Set<KnownNodePersistentDomainStore> children) {
		Set<KnownNodePersistentDomainStore> old_children = this.children;
		this.children = children;
		propertyChangeSupport().firePropertyChange("children", old_children,
				children);
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setName(String name) {
		String old_name = this.name;
		this.name = name;
		propertyChangeSupport().firePropertyChange("name", old_name, name);
	}

	public void setParent(KnownNodePersistentDomainStore parent) {
		KnownNodePersistentDomainStore old_parent = this.parent;
		this.parent = parent;
		propertyChangeSupport().firePropertyChange("parent", old_parent,
				parent);
	}

	public void setProperties(String properties) {
		String old_properties = this.properties;
		this.properties = properties;
		propertyChangeSupport().firePropertyChange("properties", old_properties,
				properties);
	}
	private String path() {
		KnownNodePersistentDomainStore cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			segments.add(cursor.getName());
		}
		Collections.reverse(segments);
		return segments.stream().collect(Collectors.joining("/"));
	}
	@Override
	public String toString() {
		return Ax.format("%s : %s", id, path());
	}
}