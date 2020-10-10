package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class MvccCorrectnessToken {
	Set<String> checkedSources = Collections
			.synchronizedSet(new LinkedHashSet<>());
}