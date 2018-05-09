package cc.alcina.framework.common.client.entity.knowns;

import java.util.Set;

import javax.persistence.MappedSuperclass;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.util.CommonUtils;

@MappedSuperclass
@DomainTransformPersistable
public abstract class KnownNodePersistent<U extends KnownNodePersistent>
		extends AbstractDomainBase<U> {
	protected long id;
	
	private String path;
	
	private String value;
	
	private KnownNodePersistent parent;
	
	private Set<KnownNodePersistent> children=new LiSet<>();

	public KnownNodePersistent() {
		super();
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("%s : %s", id, path);
	}
}