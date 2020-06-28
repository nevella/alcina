/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.collections.CollectionFilter;

/**
 * 
 * @author Nick Reddel
 */
public class ContentNode implements Serializable {
	private String title;

	private String content;

	private String url;

	private List<ContentNode> children = new ArrayList<ContentNode>();

	private List<String> labels = new ArrayList<String>();

	private Date lastModified;

	private long id;

	public ContentNode() {
	}

	public ContentNode cloneWithFilter(CollectionFilter<ContentNode> filter) {
		if (!filter.allow(this)) {
			return null;
		}
		ContentNode copy = new ContentNode();
		copy.setContent(getContent());
		copy.setId(getId());
		copy.setLastModified(getLastModified());
		copy.setTitle(getTitle());
		copy.setUrl(getUrl());
		for (ContentNode child : getChildren()) {
			ContentNode childCopy = child.cloneWithFilter(filter);
			if (childCopy != null) {
				copy.getChildren().add(childCopy);
			}
		}
		return copy;
	}

	public Map<String, ContentNode> createTitleMap() {
		Map<String, ContentNode> result = new HashMap<String, ContentNode>();
		List<ContentNode> kids = getAllChildren(null);
		for (ContentNode dn : kids) {
			result.put(dn.getTitle().toLowerCase(), dn);
		}
		return result;
	}

	public List<ContentNode> getAllChildren(List<ContentNode> currentList) {
		if (currentList == null) {
			currentList = new ArrayList<ContentNode>();
		}
		currentList.add(this);
		for (ContentNode n : getChildren()) {
			n.getAllChildren(currentList);
		}
		return currentList;
	}

	public List<ContentNode> getChildren() {
		return children;
	}

	public String getContent() {
		return this.content;
	}

	public long getId() {
		return this.id;
	}

	public List<String> getLabels() {
		return labels;
	}

	public Date getLastModified() {
		return this.lastModified;
	}

	public String getTitle() {
		return this.title;
	}

	public String getUrl() {
		return this.url;
	}

	public void setChildren(List<ContentNode> children) {
		this.children = children;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
