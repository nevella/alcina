package cc.alcina.framework.entity.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;

public class FilterArgvParam {
	public String[] argv;

	public String value;

	public FilterArgvParam(String[] argv, String key) {
		List<String> strs = new ArrayList<String>(Arrays.asList(argv));
		int i = strs.indexOf(key);
		if (i != -1) {
			strs.remove(i);
			value = strs.remove(i);
		}
		this.argv = (String[]) strs.toArray(new String[strs.size()]);
	}

	public FilterArgvParam(String[] argv) {
		this.argv = argv;
		next();
	}

	public String next() {
		moveNext();
		return this.value;
	}

	public boolean moveNext() {
		if (argv.length == 0) {
			return false;
		}
		List<String> strs = new ArrayList<String>(Arrays.asList(argv));
		this.value = CommonUtils.first(strs);
		strs.remove(0);
		this.argv = (String[]) strs.toArray(new String[strs.size()]);
		return true;
	}

	public String valueOrDefault(String defaultValue) {
		return value != null ? value : defaultValue;
	}

	public long getLong() {
		return Long.parseLong(valueOrDefault("0"));
	}
}