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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.IsClassFilter;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.permissions.HasPermissionsValidation;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.serializer.flat.TreeSerializable;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;

//FIXME - mvcc.4 - this shouldn't extend entity
@RegistryLocations({
		@RegistryLocation(registryPoint = JaxbContextRegistration.class),
		@RegistryLocation(registryPoint = TreeSerializable.class) })
public abstract class SearchDefinition extends WrapperPersistable
		implements TreeSerializable, TreeRenderable, ContentDefinition,
		HasPermissionsValidation, HasReflectiveEquivalence<SearchDefinition>,
		ReflectCloneable<SearchDefinition> {
	static final transient long serialVersionUID = -1L;

	public static final transient int LARGE_SEARCH = 0xFF0000;

	public static final transient String CONTEXT_CURRENT_SEARCH_DEFINITION = SearchDefinition.class
			.getName() + ":" + "current-search-definition";

	final transient String orderJoin = ", ";

	private int resultsPerPage;

	private String publicationType;

	private String name;

	private String orderName;

	private int charWidth;

	private int clientSearchIndex;

	private Set<CriteriaGroup> criteriaGroups = new LightSet<CriteriaGroup>();

	private Set<OrderGroup> orderGroups = new LightSet<OrderGroup>();

	protected transient String defaultFilterDescription = "";

	protected transient Map<String, String> propertyColumnAliases;

	private transient List<PropertyChangeListener> globalListeners = new ArrayList<>();

	public void addCriterionToSoleCriteriaGroup(SearchCriterion sc) {
		addCriterionToSoleCriteriaGroup(sc, false);
	}

	public void addCriterionToSoleCriteriaGroup(SearchCriterion sc,
			boolean knownEmptyCriterion) {
		assert criteriaGroups.size() == 1;
		criteriaGroups.iterator().next().addCriterion(sc);
		PropertyChangeEvent event = new PropertyChangeEvent(this, null, null,
				null);
		for (PropertyChangeListener listener : new ArrayList<>(
				globalListeners)) {
			sc.addPropertyChangeListener(listener);
			if (!sc.emptyCriterion() && !knownEmptyCriterion) {
				listener.propertyChange(event);
			}
		}
	}

	public void addToSoleCriteriaGroupAndRemoveExisting(SearchCriterion sc) {
		removeFromSoleCriteriaGroup(sc);
		addCriterionToSoleCriteriaGroup(sc, false);
	}

	public Set<SearchCriterion> allCriteria() {
		LinkedHashSet<SearchCriterion> result = new LinkedHashSet<SearchCriterion>();
		for (CriteriaGroup cg : getCriteriaGroups()) {
			result.addAll(cg.getCriteria());
		}
		return result;
	}

	public <SC extends SearchCriterion> List<SC> allCriteria(Class<SC> clazz) {
		return (List<SC>) CollectionFilters.filter(allCriteria(),
				new IsClassFilter(clazz));
	}

	public Set<OrderCriterion> allOrderCriteria() {
		LinkedHashSet<OrderCriterion> result = new LinkedHashSet<OrderCriterion>();
		for (OrderGroup cg : getOrderGroups()) {
			result.addAll(cg.getCriteria());
		}
		return result;
	}

	public void clearAllCriteria() {
		getCriteriaGroups().forEach(cg -> cg.getCriteria().clear());
	}

	public void clearOrderGroup(Class<? extends OrderGroup> clazz) {
		OrderGroup og = orderGroup(clazz);
		if (og != null) {
			og.getCriteria().clear();
		}
	}

	public <C extends CriteriaGroup> C criteriaGroup(Class<C> clazz) {
		return (C) criteriaGroups.stream().filter(Objects::nonNull)
				.filter(cg -> cg.getClass() == clazz).findFirst().orElse(null);
	}

	public <CG extends CriteriaGroup> CG ensureCriteriaGroup(Class<CG> clazz) {
		CG existing = criteriaGroup(clazz);
		if (existing == null) {
			CG newInstance = Reflections.newInstance(clazz);
			getCriteriaGroups().add(newInstance);
			return newInstance;
		} else {
			return existing;
		}
	}

	public void ensureCriteriaGroups(CriteriaGroup... criteriaGroups) {
		for (CriteriaGroup cg : criteriaGroups) {
			CriteriaGroup existing = criteriaGroup(cg.getClass());
			if (existing == null) {
				getCriteriaGroups().add(cg);
			}
		}
	}

	public <V extends OrderGroup> V ensureOrderGroup(V orderGroup) {
		V og = (V) orderGroup(orderGroup.getClass());
		if (og != null) {
			return og;
		}
		putOrderGroup(orderGroup);
		return orderGroup;
	}

	public EqlWithParameters eql(boolean withOrderClause) {
		EqlWithParameters ewp = new EqlWithParameters();
		if (criteriaGroups.size() == 0) {
			if (withOrderClause) {
				ewp.eql = orderEql();
			}
			return ewp;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("\n WHERE ");
		int ct = 0;
		int paramCounter = 1;
		for (CriteriaGroup cg : getCriteriaGroups()) {
			if (!PermissionsManager.get().isPermitted(cg)) {
				continue;
			}
			EqlWithParameters ewp2 = cg.eql();
			if (CommonUtils.isNullOrEmpty(ewp2.eql)) {
				continue;
			}
			if (ct++ != 0) {
				sb.append(" AND ");
			}
			String eql = "";
			String[] split = ewp2.eql.split("\\?");
			for (int i = 0; i < split.length; i++) {
				String s = split[i];
				eql += s;
				if (i < split.length - 1) {
					eql += "?" + paramCounter++;
				}
			}
			sb.append(eql);
			ewp.parameters.addAll(ewp2.parameters);
		}
		if (withOrderClause) {
			sb.append(orderEql());
		}
		ewp.eql = sb.toString();
		return ewp;
	}

	public String filterDescription(boolean html) {
		StringBuffer result = new StringBuffer();
		try {
			LooseContext.getContext().set(CONTEXT_CURRENT_SEARCH_DEFINITION,
					this);
			for (CriteriaGroup criteriaGroup : criteriaGroups) {
				String s = html ? criteriaGroup.toHtml()
						: criteriaGroup.toString();
				if (!CommonUtils.isNullOrEmpty(s)) {
					if (result.length() != 0) {
						result.append("; ");
					}
					result.append(s);
				}
			}
		} finally {
			LooseContext.getContext().remove(CONTEXT_CURRENT_SEARCH_DEFINITION);
		}
		return (result.length() == 0) ? defaultFilterDescription
				: result.toString();
	}

	public <V extends SearchCriterion> V firstCriterion(Class<V> clazz) {
		for (CriteriaGroup cg : getCriteriaGroups()) {
			for (SearchCriterion c : (Set<SearchCriterion>) cg.getCriteria()) {
				if (c.getClass() == clazz) {
					return (V) c;
				}
			}
		}
		return null;
	}

	public <V extends SearchCriterion> V firstCriterion(V sub) {
		V first = (V) firstCriterion(sub.getClass());
		return first != null ? first : sub;
	}

	@AlcinaTransient
	public int getCharWidth() {
		return this.charWidth;
	}

	@AlcinaTransient
	public int getClientSearchIndex() {
		return this.clientSearchIndex;
	}

	public Set<CriteriaGroup> getCriteriaGroups() {
		return this.criteriaGroups;
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return "";
	}

	public String getName() {
		return name;
	}

	public Set<OrderGroup> getOrderGroups() {
		return this.orderGroups;
	}

	/**
	 * Note - there's a slight risk of "injection" here...if truly concerned,
	 * subclass validatePermissions()
	 * 
	 * @return the property name for "order by" in an eql query, if any
	 */
	public String getOrderName() {
		return this.orderName;
	}

	@Override
	public String getPublicationType() {
		return publicationType;
	}

	public int getResultsPerPage() {
		return this.resultsPerPage;
	}

	public void globalPropertyChangeListener(PropertyChangeListener listener,
			boolean add) {
		if (add) {
			globalListeners.add(listener);
		} else {
			globalListeners.remove(listener);
		}
		allCriteria().forEach(c -> propertyChangeDelta(c, listener, add));
	}

	public String idEqlPrefix() {
		return null;
	}

	public void mapCriteriaToPropertyNames() {
		CriterionPropertyNameMappings crMappings = Reflections.classLookup()
				.getAnnotationForClass(getClass(),
						CriterionPropertyNameMappings.class);
		if (crMappings != null) {
			for (CriterionPropertyNameMapping mapping : crMappings.value()) {
				CriteriaGroup criteriaGroup = criteriaGroup(
						mapping.criteriaGroupClass());
				if (criteriaGroup != null) {
					criteriaGroup.map(mapping.criterionClass(),
							mapping.propertyName());
				}
			}
		}
	}

	public void maxResultsPerPage() {
		setResultsPerPage(Integer.MAX_VALUE);
	}

	@Override
	// because it's prettier, there's ambiguity about multiple criteriagroups w
	// 1 criterion vs 1 cg multiple criteria
	// so - combine (map to first)
	public void onAfterTreeDeserialize() {
		Multimap<?, List<CriteriaGroup>> byClass = criteriaGroups.stream()
				.collect(AlcinaCollectors
						.toKeyMultimap(CriteriaGroup::getClass));
		byClass.values().forEach(list -> {
			CriteriaGroup<?> first = list.get(0);
			for (int idx = 1; idx < list.size(); idx++) {
				CriteriaGroup later = list.get(idx);
				first.getCriteria().addAll(later.getCriteria());
				criteriaGroups.remove(later);
			}
		});
	}

	public void onBeforeRunSearch() {
	}

	public <V extends SearchCriterion> Optional<V>
			optionalFirstCriterion(Class<V> clazz) {
		return Optional.<V> ofNullable(firstCriterion(clazz));
	}

	public String orderDescription(boolean html) {
		StringBuffer result = new StringBuffer();
		for (OrderGroup orderGroup : orderGroups) {
			String s = (html ? orderGroup.toHtml() : orderGroup.toString())
					.trim();
			if (!CommonUtils.isNullOrEmpty(s)) {
				if (result.length() != 0) {
					result.append(", ");
				}
				result.append(s);
			}
		}
		return result.toString();
	}

	public <C extends OrderGroup> C orderGroup(Class<C> clazz) {
		return (C) orderGroups.stream().filter(cg -> cg.getClass() == clazz)
				.findFirst().orElse(null);
	}

	/**
	 * Note, this does not allow multiple orderings by default (to simplify
	 * injection avoidance) Override if you need multiple orderings
	 */
	public String propertyAlias(String propertyName) {
		if (propertyColumnAliases != null
				&& propertyColumnAliases.containsKey(propertyName)) {
			return propertyColumnAliases.get(propertyName);
		}
		if (!propertyName.matches("[A-Za-z0-9\\.]+")) {
			throw new RuntimeException(
					"Possible injection exception - order property: "
							+ propertyName);
		}
		return propertyName;
	}

	public boolean provideHasUndefinedDisplayText() {
		return allCriteria().stream().anyMatch(c -> {
			if (c instanceof TruncatedObjectCriterion) {
				return ((TruncatedObjectCriterion) c).getDisplayText() == null;
			}
			return false;
		});
	}

	/**
	 * For more complex search definitions, override this
	 */
	public Object provideResultsType() {
		return null;
	}

	public void removeCriterion(SearchCriterion sc,
			boolean doNotFireBecauseCriterionEmpty) {
		for (CriteriaGroup cg : getCriteriaGroups()) {
			cg.removeCriterion(sc);
		}
		PropertyChangeEvent event = new PropertyChangeEvent(this, null, null,
				null);
		for (PropertyChangeListener listener : new ArrayList<>(
				globalListeners)) {
			if (!sc.emptyCriterion() && !doNotFireBecauseCriterionEmpty) {
				listener.propertyChange(event);
			}
			sc.removePropertyChangeListener(listener);
		}
	}

	public void removeFromSoleCriteriaGroup(SearchCriterion sc) {
		assert criteriaGroups.size() == 1;
		criteriaGroups.iterator().next().getCriteria()
				.removeIf(sco -> sco.getClass() == sc.getClass());
	}

	public String resultEqlPrefix() {
		return null;
	}

	public void setCharWidth(int charWidth) {
		this.charWidth = charWidth;
	}

	public void setClientSearchIndex(int clientSearchIndex) {
		this.clientSearchIndex = clientSearchIndex;
	}

	public void setCriteriaGroups(Set<CriteriaGroup> criteriaGroups) {
		this.criteriaGroups = criteriaGroups;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOrderGroups(Set<OrderGroup> orderGroups) {
		this.orderGroups = orderGroups;
	}

	public void setOrderName(String orderName) {
		this.orderName = orderName;
	}

	public void setPublicationType(String publicationType) {
		this.publicationType = publicationType;
	}

	public void setResultsPerPage(int resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
	}

	public String toHtml() {
		return Ax.format("%s%s - %s",
				CommonUtils.isNullOrEmpty(getName()) ? ""
						: "<b>" + getName() + "</b> - ",
				filterDescription(true), orderDescription(true));
	}

	@Override
	public String toString() {
		FormatBuilder fb = new FormatBuilder();
		fb.separator(" - ");
		fb.appendIfNotBlank(filterDescription(false), orderDescription(false));
		return fb.toString();
	}

	@Override
	public String validatePermissions() {
		mapCriteriaToPropertyNames();
		List<CriteriaGroup> children = new ArrayList<CriteriaGroup>();
		children.addAll(getCriteriaGroups());
		children.addAll(getOrderGroups());
		return DefaultValidation.validatePermissions(this, children);
	}

	public SearchDefinition withCriterion(SearchCriterion sc) {
		addCriterionToSoleCriteriaGroup(sc, false);
		return this;
	}

	private void propertyChangeDelta(SourcesPropertyChangeEvents o,
			PropertyChangeListener listener, boolean add) {
		if (add) {
			o.addPropertyChangeListener(listener);
		} else {
			o.removePropertyChangeListener(listener);
		}
	}

	protected String orderEql() {
		return "";
	}

	protected void putCriteriaGroup(CriteriaGroup cg) {
		criteriaGroups.add(cg);
	}

	protected void putOrderGroup(OrderGroup og) {
		orderGroups.add(og);
	}
}
