package cc.alcina.framework.entity.entityaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.MappedSuperclass;

public interface KnownNodePersistent {

	public String getName();


	public String getProperties();


	default String path() {
		KnownNodePersistent cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			segments.add(cursor.getName());
		}
		Collections.reverse(segments);
		return segments.stream().collect(Collectors.joining("/"));
	}
}
