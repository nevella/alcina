package cc.alcina.framework.common.client.domain.search;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.domain.CompositeFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class DomainSearcher {
    private static UnsortedMultikeyMap<DomainCriterionHandler> handlers = new UnsortedMultikeyMap<DomainCriterionHandler>(
            2);

    private static Map<Class, DomainDefinitionHandler> definitionHandlers = new LinkedHashMap<>();

    public static boolean useSequentialSearch = false;

    private static synchronized void setupHandlers() {
        if (handlers.isEmpty()) {
            List<DomainCriterionHandler> impls = Registry
                    .impls(DomainCriterionHandler.class);
            for (DomainCriterionHandler handler : impls) {
                handlers.put(handler.handlesSearchDefinition(),
                        handler.handlesSearchCriterion(), handler);
            }
            List<DomainDefinitionHandler> defImpls = Registry
                    .impls(DomainDefinitionHandler.class);
            for (DomainDefinitionHandler handler : defImpls) {
                definitionHandlers.put(handler.handlesSearchDefinition(),
                        handler);
            }
        }
    }

    private SearchDefinition def;

    private LockingDomainQuery query;

    public DomainSearcher() {
    }

    public <T extends HasIdAndLocalId> List<T> search(SearchDefinition def,
            Class<T> clazz, Comparator<T> order) {
        query = useSequentialSearch ? new LockingDomainQuery()
                : Registry.impl(LockingDomainQuery.class);
        query.setQueryClass(clazz);
        this.def = def;
        query.def = def;
        setupHandlers();
        processDefinitionHandler();
        processHandlers();
        List<T> list;
        try {
            query.readLock(true);
            list = query.list();
            list = Registry.impl(DomainSearcherAppFilter.class).filter(def,
                    list);
        } finally {
            query.readLock(false);
        }
        list.sort(order);
        return list;
    }

    private DomainCriterionHandler getCriterionHandler(SearchCriterion sc) {
        return handlers.get(def.getClass(), sc.getClass());
    }

    private void processDefinitionHandler() {
        DomainDefinitionHandler handler = definitionHandlers
                .get(def.getClass());
        if (handler != null) {
            query.filter(handler.getFilter(def));
        }
    }

    protected void processHandlers() {
        Set<CriteriaGroup> criteriaGroups = def.getCriteriaGroups();
        for (CriteriaGroup cg : criteriaGroups) {
            if (!cg.provideIsEmpty()) {
                CompositeFilter cgFilter = new CompositeFilter(
                        cg.getCombinator() == FilterCombinator.OR);
                boolean added = false;
                for (SearchCriterion sc : (Set<SearchCriterion>) cg
                        .getCriteria()) {
                    DomainCriterionHandler handler = getCriterionHandler(sc);
                    if (handler == null) {
                        System.err.println(CommonUtils.formatJ(
                                "No handler for def/class %s - %s\n",
                                def.getClass().getSimpleName(),
                                sc.getClass().getSimpleName()));
                        continue;
                    }
                    DomainFilter filter = handler.getFilter(sc);
                    if (filter != null) {
                        cgFilter.add(filter);
                        added = true;
                    }
                }
                if (added) {
                    query.filter(cgFilter);
                }
            }
        }
    }

    @RegistryLocation(registryPoint = DomainLocker.class, implementationType = ImplementationType.SINGLETON)
    public static class DomainLocker {
        public void readLock(boolean lock) {
        }
    }

    @RegistryLocation(registryPoint = DomainSearcherAppFilter.class, implementationType = ImplementationType.INSTANCE)
    public static abstract class DomainSearcherAppFilter {
        public abstract <T extends HasIdAndLocalId> List<T> filter(
                SearchDefinition def, List<T> list);
    }

    public static class DomainSearcherAppFilter_DefaultImpl
            extends DomainSearcherAppFilter {
        @Override
        public <T extends HasIdAndLocalId> List<T> filter(SearchDefinition def,
                List<T> list) {
            return list;
        }
    }
}
