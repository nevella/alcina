package cc.alcina.framework.entity.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ArgParser {
	private List<String> argv;

	private Set<String> flags = new LinkedHashSet<>();

	private Map<String, String> keyedValues = new LinkedHashMap<>();

	private List<String> values = new ArrayList<>();

	public ArgParser(String[] argv) {
		this.argv = Arrays.asList(argv).stream().collect(Collectors.toList());
	}

	public void addFlag(String flag) {
		// a above
	}

	public String get(String flag) {
		int idx = argv.indexOf(flag);
		if (idx != -1) {
			return argv.get(idx + 1);
		} else {
			return null;
		}
	}

	public boolean has(String flag) {
		return argv.contains(flag);
	}

	public void populateObject(Object object) {
		// not done - but populate based on field names and types
	}
}
