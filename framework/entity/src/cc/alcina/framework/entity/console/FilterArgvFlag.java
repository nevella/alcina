package cc.alcina.framework.entity.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterArgvFlag {
	public boolean contains;

	public String[] argv;

	public FilterArgvFlag(String[] argv, String flag) {
		List<String> strs = new ArrayList<String>(Arrays.asList(argv));
		this.contains = strs.remove(flag);
		this.argv = (String[]) strs.toArray(new String[strs.size()]);
	}
}