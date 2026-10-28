package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.util.StringMap;

public interface StylePassthrough {
	default void passThrough(Set<String> incoming, Set<String> result,
			List<String> names) {
		names.stream().filter(incoming::contains).forEach(result::add);
	}

	default void passThrough(StringMap incoming, StringMap result,
			List<String> names) {
		names.stream().filter(incoming::containsKey)
				.forEach(name -> result.put(name, incoming.get(name)));
	}
}