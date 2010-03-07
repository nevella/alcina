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

package cc.alcina.framework.common.client.search;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.HasTreeRenderingInfo;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;


@BeanInfo(displayNamePropertyName = "displayName")
@RegistryLocation(registryPoint = JaxbContextRegistration.class)
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class CriteriaGroup extends BaseBindable implements HasTreeRenderingInfo {
	private transient String displayName;

	private FilterCombinator combinator = FilterCombinator.AND;

	protected transient boolean userVisible = true;

	private Set<SearchCriterion> criteria = new LinkedHashSet<SearchCriterion>();

	private transient Class<?> entityClass;

	private transient boolean renderable = true;

	private transient CollectionProvider collectionProvider;

	public CriteriaGroup() {
	}

	public CollectionFilter getFilter() {
		return null;
	}

	public void addCriterion(SearchCriterion criterion) {
		criteria.add(criterion);
	}

	/*
	 * only used for single-table search, compiled out for client
	 */
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters ewp = new EqlWithParameters();
		if (criteria.size() == 0) {
			return ewp;
		}
		StringBuffer sb = new StringBuffer();
		int ct = 0;
		for (SearchCriterion searchCriterion : criteria) {
			EqlWithParameters ewp2 = searchCriterion.eql();
			ewp.parameters.addAll(ewp2.parameters);
			if (CommonUtils.isNullOrEmpty(ewp2.eql)) {
				continue;
			}
			if (ct++ == 0) {
				sb.append("(");
			} else {
				sb.append(" " + combinator.toString() + " ");
			}
			sb.append(ewp2.eql);
		}
		if (ct != 0) {
			sb.append(")");
		}
		ewp.eql = sb.toString();
		return ewp;
	}

	public FilterCombinator getCombinator() {
		return combinator;
	}

	public Set<SearchCriterion> getCriteria() {
		return this.criteria;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Class<?> getEntityClass() {
		return this.entityClass;
	}

	public String hint() {
		return null;
	}

	public boolean isRenderable() {
		return renderable;
	}

	public boolean isUserVisible() {
		return this.userVisible;
	}

	public Collection<? extends HasTreeRenderingInfo> renderableChildren() {
		return getCriteria();
	}

	public String renderablePropertyName() {
		return null;
	}

	public boolean renderChildrenHorizontally() {
		return true;
	}

	public String renderCss() {
		return "sub-head";
	}

	public BoundWidgetProvider renderCustomiser() {
		return null;
	}

	public RenderInstruction renderInstruction() {
		if (!renderable || !userVisible) {
			return RenderInstruction.NO_RENDER;
		}
		return RenderInstruction.AS_TITLE;
	}

	public void setCombinator(FilterCombinator combinator) {
		this.combinator = combinator;
	}

	public void setCriteria(Set<SearchCriterion> criteria) {
		Set<SearchCriterion> old_criteria = this.criteria;
		this.criteria = criteria;
		propertyChangeSupport.firePropertyChange("criteria", old_criteria,
				criteria);
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

	public void setRenderable(boolean noRender) {
		this.renderable = noRender;
	}

	public String toHtml() {
		if (criteria.size() == 0) {
			return "";
		}
		String result = CommonUtils.isNullOrEmpty(getDisplayName()) ? ""
				: CommonUtils.pluralise(CommonUtils
						.capitaliseFirst(getDisplayName()), criteria)
						+ ": ";
		int ct = 0;
		Set<String> searchCriterionHtmlSet = new HashSet<String>();
		for (SearchCriterion searchCriterion : criteria) {
			String searchCriterionHtml = searchCriterion.toHtml();
			if (searchCriterionHtmlSet.contains(searchCriterionHtml)) {
				continue;
			}
			searchCriterionHtmlSet.add(searchCriterionHtml);
			if (ct++ != 0) {
				result += " " + combinator.toString().toLowerCase() + " ";
			}
			result += searchCriterionHtml;
		}
		return result;
	}
	@SuppressWarnings("unchecked")
	public <T extends SearchCriterion> T soleCriterion(){
		return (T) criteria.iterator().next();
	}
	public void toSoleCriterion(SearchCriterion criterion) {
		criteria.clear();
		criteria.add(criterion);
	}

	public String toString() {
		if (criteria.size() == 0) {
			return "";
		}
		String result = CommonUtils.isNullOrEmpty(getDisplayName()) ? ""
				: CommonUtils.pluralise(CommonUtils
						.capitaliseFirst(getDisplayName()), criteria)
						+ ": ";
		int ct = 0;
		for (SearchCriterion searchCriterion : criteria) {
			if (ct++ != 0) {
				result += " " + combinator.toString().toLowerCase() + " ";
			}
			result += searchCriterion.toString();
		}
		return result;
	}

	public void putCollectionProvider(CollectionProvider collectionProvider) {
		this.collectionProvider = collectionProvider;
	}

	public CollectionProvider collectionProvider() {
		return collectionProvider;
	}
}
