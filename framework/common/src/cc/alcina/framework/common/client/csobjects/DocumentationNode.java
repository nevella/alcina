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

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class DocumentationNode implements Serializable {
	private String title;

	private String content;

	private String url;

	private List<DocumentationNode> children = new ArrayList<DocumentationNode>();

	public DocumentationNode() {
	}
	public Map<String,DocumentationNode> createTitleMap(){
		Map<String,DocumentationNode> result = new HashMap<String, DocumentationNode>();
		List<DocumentationNode> kids = getAllChildren(null);
		for (DocumentationNode dn : kids) {
			result.put(dn.getTitle().toLowerCase(), dn);
		}
		return result;
	}
	public List<DocumentationNode> getAllChildren(
			List<DocumentationNode> currentList) {
		if (currentList == null) {
			currentList = new ArrayList<DocumentationNode>();
		}
		currentList.add(this);
		for (DocumentationNode n : getChildren()) {
			n.getAllChildren(currentList);
		}
		return currentList;
	}

	private long id;

	private Date lastModified;

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getLastModified() {
		return this.lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public void setChildren(List<DocumentationNode> children) {
		this.children = children;
	}

	public List<DocumentationNode> getChildren() {
		return children;
	}
}
