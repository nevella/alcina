package cc.alcina.framework.common.client.csobjects.view;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class TreePath extends Model {
	public static TreePath from(String path) {
		TreePath cursor = new TreePath();
		for (String segment : path.split("\\.")) {
			TreePath parent = cursor;
			cursor = new TreePath();
			cursor.parent = parent;
			cursor.fromSegment(segment);
		}
		return cursor;
	}

	private TreePath parent;

	private transient String cached;

	private String segment = "";

	private transient EntityLocator locator;

	private transient String discriminator;

	private String path;

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TreePath && toString().equals(obj.toString());
	}

	public String getDiscriminator() {
		return this.discriminator;
	}

	public EntityLocator getLocator() {
		return this.locator;
	}

	public TreePath getParent() {
		return this.parent;
	}

	public String getPath() {
		return this.path;
	}

	public String getSegment() {
		return this.segment;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public boolean provideIsEmpty() {
		return toString().isEmpty();
	}

	public void putDiscriminator(Object object) {
		setDiscriminator(CommonUtils.friendlyConstant(object, "-"));
	}

	public void setDiscriminator(String discriminator) {
		this.discriminator = discriminator;
		refreshSegment();
	}

	public void setLocator(EntityLocator locator) {
		this.locator = locator;
		refreshSegment();
	}

	public void setParent(TreePath parent) {
		this.parent = parent;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setSegment(String segment) {
		this.segment = segment;
	}

	@Override
	public String toString() {
		if (cached == null) {
			cached = parent == null || parent.toString().isEmpty() ? segment
					: parent.toString() + "." + segment;
		}
		return cached;
	}

	private void fromSegment(String segment) {
		locator = new EntityLocator();
		if (segment.matches("\\d+")) {
			locator.id = Long.parseLong(segment);
		} else {
			discriminator = segment;
		}
		refreshSegment();
	}

	private void refreshSegment() {
		cached = null;
		Preconditions.checkState(discriminator == null ^ locator == null);
		if (discriminator == null) {
			segment = String.valueOf(locator.id);
		} else {
			segment = discriminator;
		}
	}
}