package cc.alcina.framework.entity.entityaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;

@MappedSuperclass
@DomainTransformPersistable
@RegistryLocation(registryPoint = KnownNodePersistent.class)
public abstract class KnownNodePersistent extends
		AbstractDomainBase<KnownNodePersistent> implements IVersionable {
	protected long id;

	private Set<KnownNodePersistent> children=new LiSet<>();

	private String name;

	private String properties;

	private KnownNodePersistent parent;

	public KnownNodePersistent() {
	}

	public KnownNodePersistent(String name) {
		setName(name);
	}

	@Transient
	public Set<KnownNodePersistent> getChildren() {
		return this.children;
	}

	public String getName() {
		return this.name;
	}

	@Transient
	public KnownNodePersistent getParent() {
		return this.parent;
	}

	@Transient
	@Lob
	public String getProperties() {
		return this.properties;
	}

	public String path() {
		KnownNodePersistent cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			segments.add(cursor.getName());
		}
		Collections.reverse(segments);
		return segments.stream().collect(Collectors.joining("/"));
	}

	public void setChildren(Set<KnownNodePersistent> children) {
		Set<KnownNodePersistent> old_children = this.children;
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

	public void setParent(KnownNodePersistent parent) {
		KnownNodePersistent old_parent = this.parent;
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

	@Override
	public String toString() {
		return CommonUtils.formatJ("%s : %s", id, path());
	}
}