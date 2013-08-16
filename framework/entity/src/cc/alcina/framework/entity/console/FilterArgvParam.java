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

	public FilterArgvParam(String[] argv, int index) {
		List<String> strs = new ArrayList<String>(Arrays.asList(argv));
		this.value = CommonUtils.first(strs);
		if (value != null) {
			strs.remove(0);
		}
		this.argv = (String[]) strs.toArray(new String[strs.size()]);
	}

	public String valueOrDefault(String defaultValue) {
		return value != null ? value : defaultValue;
	}
}