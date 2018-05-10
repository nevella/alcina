package cc.alcina.framework.entity.entityaccess.knowns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.MappedSuperclass;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.util.CommonUtils;

@MappedSuperclass
@DomainTransformPersistable
public abstract class KnownNode extends AbstractDomainBase<KnownNode>
		implements IVersionable {
	protected long id;

	public KnownNode() {
		super();
	}

	public void setId(long id) {
		this.id = id;
	}

	public abstract KnownNode parent();

	public abstract String pathSegment();

	@Override
	public String toString() {
		return CommonUtils.formatJ("%s : %s", id, path());
	}

	public String path() {
		KnownNode cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			segments.add(cursor.pathSegment());
		}
		Collections.reverse(segments);
		return segments.stream().collect(Collectors.joining("/"));
	}
}