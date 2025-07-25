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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.collections.IsInstanceFilter;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.permissions.HasPermissionsValidation;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;

/**
 * <p>
 * Doc - work in progress - topics are:
 * <ul>
 * <li>criteria groups - operate as more-or-less a set of parentheses (a
 * grouping on the criterion) as well as organisation hooks for the ui. Note
 * movement away from CGs to multi-valued criteria
 * <li>criteria - the meat of the definition - define constraints on the search.
 * <li>ordering
 * <li>eql
 * <li>string representation
 * <li>serialization
 * </ul>
 * 
 * 
 *
 * @author nick@alcina.cc
 *
 */
@Registrations({ @Registration(JaxbContextRegistration.class), })
public abstract class SearchDefinition extends Bindable
		implements TreeSerializable, TreeRenderable, ContentDefinition,
		HasPermissionsValidation, HasReflectiveEquivalence<SearchDefinition>,
		ReflectCloneable<SearchDefinition> {
	public static final transient int LARGE_SEARCH = 0xFF0000;

	public static final transient String CONTEXT_CURRENT_SEARCH_DEFINITION = SearchDefinition.class
			.getName() + ".CONTEXT_CURRENT_SEARCH_DEFINITION";

	/*
	 * Instructs the searcher to not project (results are for an identity with
	 * root/system privileges)
	 */
	public transient boolean withoutProjection;

	final transient String orderJoin = ", ";

	private int resultsPerPage;

	private String publicationType;

	private String name;

	private String orderName;

	private Set<CriteriaGroup> criteriaGroups = new LightSet<CriteriaGroup>()
			.withNotifyNullWrites();

	private Set<OrderGroup> orderGroups = new LightSet<OrderGroup>();

	protected transient String defaultFilterDescription = "";

	protected transient Map<String, String> propertyColumnAliases;

	private transient List<PropertyChangeListener> globalListeners = new ArrayList<>();

	// default 0 (rendered as 1 if non-empty results)
	private int pageNumber;

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
		return (Set<SearchCriterion>) getCriteriaGroups().stream()
				.filter(Objects::nonNull).map(CriteriaGroup::getCriteria)
				.flatMap(Collection::stream)
				.collect(AlcinaCollectors.toLinkedHashSet());
	}

	public <SC extends SearchCriterion> List<SC> allCriteria(Class<SC> clazz) {
		return (List<SC>) allCriteria().stream()
				.filter(new IsInstanceFilter(clazz))
				.collect(Collectors.toList());
	}

	public Set<OrderCriterion> allOrderCriteria() {
		return (Set<OrderCriterion>) getOrderGroups().stream()
				.filter(Objects::nonNull).map(CriteriaGroup::getCriteria)
				.flatMap(Collection::stream)
				.collect(AlcinaCollectors.toLinkedHashSet());
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

	public void ensureDefaultCriteria() {
		getCriteriaGroups().forEach(CriteriaGroup::ensureDefaultCriteria);
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
			if (!Permissions.isPermitted(cg)) {
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

	@PropertySerialization(path = "page")
	public int getPageNumber() {
		return this.pageNumber;
	}

	@Override
	public String getPublicationType() {
		return publicationType;
	}

	@PropertySerialization(path = "pageSize")
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
		CriterionPropertyNameMappings crMappings = Reflections.at(getClass())
				.annotation(CriterionPropertyNameMappings.class);
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

	protected String orderEql() {
		return "";
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

	private void propertyChangeDelta(SourcesPropertyChangeEvents o,
			PropertyChangeListener listener, boolean add) {
		if (add) {
			o.addPropertyChangeListener(listener);
		} else {
			o.removePropertyChangeListener(listener);
		}
	}

	public boolean provideHasUndefinedDisplayText() {
		return allCriteria().stream().anyMatch(c -> {
			if (c instanceof EntityCriterion) {
				return ((EntityCriterion) c).getDisplayText() == null;
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

	protected void putCriteriaGroup(CriteriaGroup cg) {
		criteriaGroups.add(cg);
	}

	protected void putOrderGroup(OrderGroup og) {
		orderGroups.add(og);
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

	public void setPageNumber(int pageNumber) {
		int old_pageNumber = this.pageNumber;
		this.pageNumber = pageNumber;
		propertyChangeSupport().firePropertyChange("pageNumber", old_pageNumber,
				pageNumber);
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
	public TreeSerializable.Customiser treeSerializationCustomiser() {
		return new Customiser(this);
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

	protected static class Customiser<S extends SearchDefinition>
			extends TreeSerializable.Customiser<S> {
		public Customiser(S serializable) {
			super(serializable);
		}

		@Override
		public String filterTestSerialized(String serialized) {
			if (serialized.contains(".displayText=")) {
				StringMap map = StringMap.fromPropertyString(serialized);
				map.keySet().removeIf(k -> k.endsWith(".displayText"));
				return map.toPropertyString();
			} else {
				return serialized;
			}
		}

		@Override
		public void onAfterTreeDeserialize() {
			super.onAfterTreeDeserialize();
			serializable.ensureDefaultCriteria();
		}

		@Override
		public void onAfterTreeSerialize() {
			super.onAfterTreeSerialize();
			serializable.ensureDefaultCriteria();
		}

		@Override
		public void onBeforeTreeDeserialize() {
			super.onBeforeTreeDeserialize();
			serializable.getCriteriaGroups()
					.forEach(CriteriaGroup::clearCriteria);
		}

		@Override
		public void onBeforeTreeSerialize() {
			serializable.getCriteriaGroups().forEach(cg -> cg.getCriteria()
					.removeIf(sc -> ((SearchCriterion) sc).emptyCriterion()));
			serializable.getOrderGroups().forEach(og -> og
					.treeSerializationCustomiser().onBeforeTreeSerialize());
		}
	}
}
