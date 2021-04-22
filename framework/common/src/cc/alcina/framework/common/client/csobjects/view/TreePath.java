package cc.alcina.framework.common.client.csobjects.view;

import java.io.Serializable;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.CommonUtils;

public class TreePath implements Serializable {
	public static TreePath from(String path) {
		TreePath root = new TreePath();
		TreePath cursor = null;
		for (String segment : path.split(".")) {
			TreePath parent = cursor;
			cursor = new TreePath();
			cursor.parent = parent;
			cursor.fromSegment(segment);
		}
		return root;
	}

	private TreePath parent;

	private transient String cached;

	private String segment;

	private transient EntityLocator locator;

	private transient String discriminator;

	private String path;

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

	public void setDiscriminator(String discriminator) {
		this.discriminator = discriminator;
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
		cached = null;
	}

	@Override
	public String toString() {
		if (cached == null) {
			cached = parent == null ? segment
					: parent.toString() + "." + segment;
		}
		return cached;
	}

	private void fromSegment(String segment) {
		String[] parts = segment.split(",");
		locator = new EntityLocator();
		locator.id = Long.parseLong(parts[0]);
		if (parts.length > 0) {
			discriminator = parts[1];
		}
	}

	private void refreshSegment() {
		cached = null;
		Preconditions.checkState(locator.id != 0);
		segment = String.valueOf(locator.id);
		if (discriminator != null) {
			segment += "," + CommonUtils.friendlyConstant(discriminator, "-");
		}
	}
}