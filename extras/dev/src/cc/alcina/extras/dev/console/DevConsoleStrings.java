package cc.alcina.extras.dev.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.CollectionFilters.InverseFilter;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
public class DevConsoleStrings {
	public transient Set<String> tags = new LinkedHashSet<String>();

	public List<DevConsoleString> strings = new ArrayList<DevConsoleString>();

	public void add(String name, List<String> tags, String content) {
		DevConsoleString dcs = new DevConsoleString(name, tags, content);
		strings.add(dcs);
		remap();
	}

	public DevConsoleString get(final String name) {
		CollectionFilter<DevConsoleString> hasNameFilter = new CollectionFilter<DevConsoleStrings.DevConsoleString>() {
			@Override
			public boolean allow(DevConsoleString o) {
				return o.name.equals(name);
			}
		};
		return CommonUtils
				.last(CollectionFilters.filter(strings, hasNameFilter));
	}

	public List<DevConsoleString> list(final List<String> tags) {
		CollectionFilter<DevConsoleString> hasTagsFilter = new CollectionFilter<DevConsoleStrings.DevConsoleString>() {
			@Override
			public boolean allow(DevConsoleString o) {
				return o.hasTags(tags);
			}
		};
		return CollectionFilters.filter(strings, hasTagsFilter);
	}

	public Collection<String> listTags() {
		return tags;
	}

	public void remove(final String name) {
		CollectionFilter<DevConsoleString> hasNameFilter = new CollectionFilter<DevConsoleStrings.DevConsoleString>() {
			@Override
			public boolean allow(DevConsoleString o) {
				return o.name.toLowerCase().startsWith(name);
			}
		};
		CollectionFilters.filterInPlace(strings,
				new InverseFilter(hasNameFilter));
		remap();
	}

	private void remap() {
		tags.clear();
		for (DevConsoleString s : strings) {
			tags.addAll(s.tags);
		}
	}

	public static class DevConsoleString {
		public List<String> tags = new ArrayList<String>();

		public String content;

		public String name;

		public DevConsoleString() {
		}

		public DevConsoleString(String name, List<String> tags,
				String content) {
			this.tags = tags;
			this.content = content;
			this.name = name;
		}

		public boolean hasTags(List<String> test) {
			test.remove("");
			tag_loop: for (String tag : test) {
				for (String existing : tags) {
					if (existing.toLowerCase().equals(tag.toLowerCase())) {
						continue tag_loop;
					}
				}
				// not matched
				return false;
			}
			return true;
		}
	}
}
