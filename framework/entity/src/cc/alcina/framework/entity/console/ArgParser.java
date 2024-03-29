package cc.alcina.framework.entity.console;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

@SuppressWarnings("unused")
public class ArgParser {
	private List<String> argv;

	private Set<String> flags = new LinkedHashSet<>();

	private Map<String, String> keyedValues = new LinkedHashMap<>();

	public ArgParser(String[] argv) {
		this.argv = Arrays.asList(argv).stream().collect(Collectors.toList());
	}

	public void addFlag(String flag) {
		// a above
	}

	public String asCommandString() {
		return argv.stream().collect(Collectors.joining(" "));
	}

	public String get(String flag) {
		int idx = argv.indexOf(flag);
		if (idx != -1) {
			return argv.get(idx + 1);
		} else {
			return null;
		}
	}

	public String getAndRemove() {
		return argv.remove(0);
	}

	public Parameter getAndRemove(String prefix) {
		Parameter result = new Parameter();
		for (String arg : argv) {
			if (arg.startsWith(prefix)) {
				result.value = arg.substring(prefix.length());
				argv.remove(arg);
				break;
			}
		}
		return result;
	}

	public boolean has(String flag) {
		return argv.contains(flag);
	}

	public boolean hasAndRemove(String flag) {
		Preconditions.checkState(flag.startsWith("--"));
		if (argv.contains(flag)) {
			argv.remove(flag);
			return true;
		} else {
			return false;
		}
	}

	public void populateObject(Object object) {
		// not done - but populate based on field names and types
	}

	public class Parameter {
		String value;

		public Optional<Boolean> booleanValue() {
			return stringValue().map(Boolean::parseBoolean);
		}

		public Optional<Integer> intValue() {
			return stringValue().map(Integer::parseInt);
		}

		public Optional<String> stringValue() {
			return Optional.ofNullable(value);
		}
	}
}
