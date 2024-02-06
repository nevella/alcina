package cc.alcina.extras.dev.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.Ax;

@XmlRootElement
@Registration(JaxbContextRegistration.class)
public class DevConsoleStrings {
	public transient Set<String> tags = new LinkedHashSet<String>();

	public List<DevConsoleString> strings = new ArrayList<DevConsoleString>();

	public void add(String name, List<String> tags, String content) {
		DevConsoleString dcs = new DevConsoleString(name, tags, content);
		strings.add(dcs);
		remap();
	}

	public DevConsoleString get(final String name) {
		return strings.stream().filter(o -> o.name.equals(name))
				.reduce(Ax.last()).orElse(null);
	}

	public List<DevConsoleString> list(final List<String> tags) {
		return strings.stream().filter(o -> o.hasTags(tags))
				.collect(Collectors.toList());
	}

	public Collection<String> listTags() {
		return tags;
	}

	private void remap() {
		tags.clear();
		for (DevConsoleString s : strings) {
			tags.addAll(s.tags);
		}
	}

	public void remove(final String name) {
		strings.removeIf(o -> o.name.toLowerCase().startsWith(name));
		remap();
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
