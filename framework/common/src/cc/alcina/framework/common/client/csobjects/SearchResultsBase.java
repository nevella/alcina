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
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResultsBase<B extends SearchResult> implements Serializable {
	private String log = "";

	private int totalResultCount;

	private int pageNumber;

	private int pageResultCount;

	@XmlTransient
	SearchDefinition searchDefinition;

	@XmlTransient
	private GArrayList<B> results;

	public SearchResultsBase() {
		super();
	}

	public String getLog() {
		return log;
	}

	public int getPageNumber() {
		return this.pageNumber;
	}

	public int getPageResultCount() {
		return this.pageResultCount;
	}

	public String getResultsDescriptionHtml() {
		SearchDefinition def = getSearchDefinition();
		String template = "You are on page %s of %s with %s results for: %s. "
				+ "%s.";
		String noResultsTemplate = "No results were returned for: %s. ";
		String tplt = (totalResultCount == 0) ? noResultsTemplate : template;
		String searchDef = def.filterDescription(true);
		if (!CommonUtils.isNullOrEmpty(def.getName())) {
			searchDef = "<span class='alcina-SearchDefinitionName'>"
					+ def.getName() + "</span> - " + searchDef;
		}
		String orderDef = def.orderDescription(true);
		return tplt == noResultsTemplate ? CommonUtils.formatJ(tplt, searchDef)
				: CommonUtils.formatJ(tplt, pageNumber, pageCount(),
						totalResultCount, searchDef, orderDef);
	}

	public String getResultsDescriptionText() {
		return getResultsDescriptionHtml();
	}

	public SearchDefinition getSearchDefinition() {
		return this.searchDefinition;
	}

	public int getTotalResultCount() {
		return this.totalResultCount;
	}

	public int pageCount() {
		return (int) Math.floor(((double) (totalResultCount - 1))
				/ getSearchDefinition().getResultsPerPage()) + 1;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public void setPageResultCount(int pageResultCount) {
		this.pageResultCount = pageResultCount;
	}

	public void setSearchDefinition(SearchDefinition searchDefinition) {
		this.searchDefinition = searchDefinition;
	}

	public void setTotalResultCount(int totalResultCount) {
		this.totalResultCount = totalResultCount;
	}

	public void setResults(List<B> results) {
		this.results = new GArrayList<B>(results);
	}

	public List<B> getResults() {
		return results;
	}
}