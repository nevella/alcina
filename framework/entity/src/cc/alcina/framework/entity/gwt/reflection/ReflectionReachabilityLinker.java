package cc.alcina.framework.entity.gwt.reflection;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.linker.impl.StandardCompilationResult;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.reachability.ReflectionModule;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.ThreeWaySetResult;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.LegacyModuleAssignments;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.ModuleTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.ModuleTypes.TypeList;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Reason;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Reason.Category;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Type;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.TypesReasons;

/**
 * Calculates reachable reflective classes from registration data.
 *
 * Initial algorithm (multipass):
 *
 * <pre>
 * - get pruned app types from js artifact
 * - calculate registrations with all keys in pruned app types
 * - omit registrations which are effectively markers (is a) rather than implementations (is the):
 *   - are multiple unless
 *   - the registree implements Registration.EnumDiscriminator and is a subclass of the first registration key
 *   - or the implements Registration.ensure
 *   - (implementation: those registrations are omitted from AppImplRegistrations.entries)
 * - for classes which are reachable by async serialization (incoming rpc), mark their *direct* subtypes as reachable
 * </pre>
 * 
 * Interplay with codemodule layout:
 * 
 * <pre>
 * - in order, most important restriction is;
 *   - preloader
 *   - initial module (minimal reflection, maximal rendered server-side)
 *   - align non-base inclusions by
 * </pre>
 */
@LinkerOrder(Order.POST)
@Shardable
public class ReflectionReachabilityLinker extends Linker {
	static int pass = 1;

	private ReachabilityLinkerPeer peer;

	private FragmentIdToName idToName;

	private ModuleTypes moduleTypes;

	private AppImplRegistrations appRegistrations;

	private AppReflectableTypes reflectableTypes;

	private LegacyModuleAssignments legacyModuleAssignments;

	private TypesReasons typesReasons;

	private TreeLogger logger;

	private File typesFile;

	private File typesReasonsFile;

	@Override
	public String getDescription() {
		return "Calculates reachable reflective classes";
	}

	@Override
	public ArtifactSet link(TreeLogger logger, LinkerContext context,
			ArtifactSet artifacts, boolean onePermutation) {
		if (ReachabilityData.dataFolder == null) {
			// ClientReflectionGenerator not invoked, exit
			return artifacts;
		}
		this.logger = logger;
		SortedSet<StandardCompilationResult> compilationResults = artifacts
				.find(StandardCompilationResult.class);
		// if obf, ignore for reachability filtering
		// TODO - 2022 - obf probably elides because of the getNames()
		// computation - check
		if (compilationResults.size() > 0 && onePermutation
				&& !((StandardLinkerContext) context).isOutputCompact()) {
			idToName = new FragmentIdToName(artifacts);
			Multiset<Integer, Set<String>> reachedClassOrConstantNames = getNames(
					compilationResults);
			typesFile = ReachabilityData
					.getReachabilityFile("reachability.json");
			typesReasonsFile = ReachabilityData
					.getReachabilityFile("reachability-reasons.json");
			moduleTypes = ReachabilityData.deserialize(ModuleTypes.class,
					typesFile);
			appRegistrations = AppImplRegistrations.fromArtifact(artifacts);
			reflectableTypes = AppReflectableTypes.fromArtifact(artifacts);
			legacyModuleAssignments = LegacyModuleAssignments
					.fromArtifact(artifacts);
			reflectableTypes.buildLookup();
			this.peer = ReachabilityData
					.newInstance(ReachabilityData.linkerPeerClass);
			this.peer.init(reflectableTypes);
			boolean delta = false;
			com.google.gwt.dev.Compiler.recompile = false;
			List<Integer> fragmentIds = idToName.getKeysInDependencyOrder();
			typesReasons = new TypesReasons();
			for (Integer fragmentId : fragmentIds) {
				// can be null if no symbols (e.g. leftover fragment for single
				// split point program)
				Set<String> typesVisibleFromSymbolNames = reachedClassOrConstantNames
						.getAndEnsure(fragmentId);
				String moduleName = idToName.idToName(fragmentId);
				delta |= applyReachedNames(moduleName,
						typesVisibleFromSymbolNames);
			}
			if (delta) {
				recordChangesAndCheckNextPass();
			}
		}
		return artifacts;
	}

	/**
	 * @param typeReasons
	 *
	 */
	private boolean applyReachedNames(String moduleName,
			Set<String> typesVisibleFromSymbolNames) {
		ModuleReachabilityComputation computeData = new ModuleReachabilityComputation(
				moduleName, typesVisibleFromSymbolNames);
		return computeData.computeReachability();
	}

	/*
	 * There is no prettier way to get this information without hacking the
	 * compiler directly. That said, Alcina already does...slightly...it
	 * wouldn't be totes cray cray
	 */
	private Multiset<Integer, Set<String>>
			getNames(SortedSet<StandardCompilationResult> compilationResults) {
		StandardCompilationResult result = compilationResults.iterator().next();
		SymbolData[] symbols = result.getSymbolMap();
		Multiset<Integer, Set<String>> reachedClassOrConstantNames = new Multiset<>();
		// filter for classLiteral assignments
		Pattern toClassName = Pattern.compile("^L(.+)_\\d+_classLit$");
		Pattern mathodSignaturePattern = Pattern.compile("^\\((.*?)\\).*");
		Pattern mathodArgumentPattern = Pattern.compile("([A-Z])([^;]+)");
		for (SymbolData symbol : symbols) {
			int fragmentNumber = symbol.getFragmentNumber();
			if (fragmentNumber == -1) {
				continue;
				// unknown -> initial -- nope, leave that for logic elsewhere
				// fragmentNumber = 0;
			}
			if ("com.google.gwt.lang.ClassLiteralHolder"
					.equals(symbol.getClassName())) {
				String symbolName = symbol.getSymbolName();
				if (symbolName != null) {
					Matcher m = toClassName.matcher(symbolName);
					if (m.matches()) {
						String className = m.group(1).replace("_", ".")
								.replace("$", ".");
						if (className.matches(".+\\.\\d+.*")) {
							// lambda, etc
						} else {
							reachedClassOrConstantNames.add(fragmentNumber,
									className);
						}
					}
				}
			} else {
				if (symbol.isMethod()) {
					Matcher m = mathodSignaturePattern.matcher(
							((StandardSymbolData) symbol).getMethodSig());
					m.matches();
					String arguments = m.group(1);
					m = mathodArgumentPattern.matcher(arguments);
					// reached from methods - e.g. rpc asyncCallback
					while (m.find()) {
						if (m.group(1).equals("L")) {
							String className = m.group(2).replace("/", ".")
									.replace("$", ".");
							reachedClassOrConstantNames.add(fragmentNumber,
									className);
						}
					}
				}
			}
		}
		return reachedClassOrConstantNames;
	}

	protected void recordChangesAndCheckNextPass() {
		int maxPass = Integer.getInteger("reachability.pass", 25);
		if (maxPass > 0) {
			ReachabilityData.serializeReachabilityFile(logger, moduleTypes,
					typesFile);
		}
		if (!Boolean.getBoolean("reachability.production")) {
			typesReasons.sort();
			ReachabilityData.serializeReachabilityFile(logger, typesReasons,
					typesReasonsFile);
			if (pass++ < maxPass) {
				logger.log(TreeLogger.Type.INFO, Ax.format(
						"Recompile - reflection changes - pass %s", pass));
				com.google.gwt.dev.Compiler.recompile = true;
			}
		}
	}

	public static class ReachabilityArtifact extends SyntheticArtifact {
		public ReachabilityArtifact(String log) {
			super(ReflectionReachabilityLinker.class, "reachabilityData",
					log.getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * Matches fragment ids (ints) to fragment names
	 * 
	 */
	static class FragmentIdToName {
		Map<Integer, String> fragmentNames = new LinkedHashMap<>();

		public FragmentIdToName(ArtifactSet artifacts) {
			SyntheticArtifact splitPointsArtifact = artifacts
					.find(SyntheticArtifact.class).stream().filter(a -> a
							.getPartialPath().equals("splitPoints0.xml.gz"))
					.findFirst().get();
			try {
				BufferedInputStream stream = new BufferedInputStream(
						new GZIPInputStream(splitPointsArtifact
								.getContents(TreeLogger.NULL)));
				String xml = ResourceUtilities.readStreamToString(stream);
				Pattern p = Pattern.compile(
						"<splitpoint id=\"(\\d+)\" " + "location=\"(.+?)\"/>");
				Matcher m = p.matcher(xml);
				while (m.find()) {
					String simpleClassName = m.group(2)
							.replaceFirst(".+[.$](.+)", "$1");
					fragmentNames.put(Integer.parseInt(m.group(1)),
							simpleClassName);
				}
				int debug = 3;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public List<String> computeDepdendencyNames(String moduleName) {
			if (Objects.equals(moduleName, ReflectionModule.INITIAL)) {
				return Collections.EMPTY_LIST;
			} else if (Objects.equals(moduleName, ReflectionModule.LEFTOVER)) {
				return Collections.singletonList(ReflectionModule.INITIAL);
			} else {
				return Arrays.asList(ReflectionModule.INITIAL,
						ReflectionModule.LEFTOVER);
			}
		}

		public int fragmentId(String moduleName) {
			switch (moduleName) {
			case ReflectionModule.INITIAL:
				return 0;
			case ReflectionModule.LEFTOVER:
				return fragmentNames.size() + 1;
			case ReflectionModule.UNKNOWN:
				return fragmentNames.size() + 2;
			case ReflectionModule.EXCLUDED:
				return fragmentNames.size() + 3;
			default:
				return fragmentNames.entrySet().stream()
						.filter(e -> e.getValue().equals(moduleName))
						.findFirst().get().getKey();
			}
		}

		public List<Integer> getKeysInDependencyOrder() {
			List<Integer> keys = new ArrayList<>();
			keys.add(0);
			if (fragmentNames.size() > 0) {
				// leftover
				keys.add(fragmentNames.size() + 1);
			}
			fragmentNames.keySet().forEach(keys::add);
			return keys;
		}

		String idToName(Integer key) {
			if (key.intValue() == 0) {
				return ReflectionModule.INITIAL;
			} else if (fragmentNames.containsKey(key)) {
				return fragmentNames.get(key);
			} else {
				if (key == fragmentNames.size() + 1) {
					return ReflectionModule.LEFTOVER;
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
	}

	class ModuleReachabilityComputation {
		// reflected types contained in the code module
		private TypeList typeList;

		String moduleName;

		// types reflected by modules the current module being analysed depends
		// on (so not to be included in this module)
		private Set<Type> dependencyModuleTypes;

		// types reflected by this module in the previous pass. Constructed from
		// reachability.json, immutable
		private Set<Type> previousPassReflectedTypes;

		// types reflected by this module.
		private Set<Type> reflectedTypes = new LinkedHashSet<>();

		// all types (reflected or not) contained in current module
		private Set<Type> visibleTypes;

		private Set<String> typesVisibleFromSymbolNames;

		public ModuleReachabilityComputation(String moduleName,
				Set<String> typesVisibleFromSymbolNames) {
			this.moduleName = moduleName;
			this.typesVisibleFromSymbolNames = typesVisibleFromSymbolNames;
			typeList = moduleTypes.ensureTypeList(moduleName);
			dependencyModuleTypes = moduleTypes
					.typesFor(idToName.computeDepdendencyNames(moduleName));
			previousPassReflectedTypes = Collections
					.unmodifiableSet(typeList.types.stream()
							.collect(AlcinaCollectors.toLinkedHashSet()));
			visibleTypes = typesVisibleFromSymbolNames.stream().map(Type::get)
					.collect(Collectors.toSet());
		}

		private void addWithReason(Category category, Type type) {
			addWithReason(category, type, peer.explain(type).orElse(null),
					false);
		}

		private void addWithReason(Category category, Type type,
				String reasonMessage, boolean required) {
			if (dependencyModuleTypes.contains(type)) {
				// already in a loaded module
				return;
			}
			if (reflectedTypes.contains(type)) {
				return;
			}
			boolean permit = peer.permit(type);
			if (!permit && required) {
				logger.log(TreeLogger.Type.WARN, Ax.format(
						"Should be explicitly permitted (excluded but required): %s",
						type));
			}
			String assignToModuleName = permit ? moduleName
					: ReflectionModule.EXCLUDED;
			Reason reason = new Reason(idToName.fragmentId(assignToModuleName),
					assignToModuleName, category, reasonMessage);
			if (permit) {
				reflectedTypes.add(type);
			}
			typesReasons.add(reason, type);
		}

		private boolean logAndComputeDelta() {
			ThreeWaySetResult<Type> split = CommonUtils
					.threeWaySplit(previousPassReflectedTypes, reflectedTypes);
			typeList.types = reflectedTypes.stream().sorted()
					.collect(Collectors.toList());
			logger.log(TreeLogger.Type.INFO, Ax.format(
					"Reachability [%s] - :: %s", moduleName, split.toSizes()));
			/*
			 * Reasoning for addition to reflective/reachable set is recorded in
			 * the generated reachability-snapshot...reasons.json file. End goal
			 * is to justify those inclusions via declarative rules so that code
			 * size is minimised.
			 */
			boolean unknownAssignmentChanged = moduleTypes
					.unknownToNotReached();
			boolean delta = split.firstOnly.size() > 0
					|| split.secondOnly.size() > 0 || unknownAssignmentChanged;
			return delta;
		}

		/*
		 * Only add the return value hierarchy - arguments we pass must be
		 * directly reachable from code, so will have reflection data injected
		 * by addFromVisibleModuleTypes()
		 */
		protected void addAsyncReachableTypes() {
			int passDelta = 0;
			Set<Type> initialAsyncReachableTypes = visibleTypes.stream()
					.filter(reflectableTypes::contains)
					.map(reflectableTypes::typeHierarchy)
					.flatMap(th -> th.rpcSerializableTypes.stream())
					.collect(AlcinaCollectors.toLinkedHashSet());
			Set<Type> asyncReachableTypes = initialAsyncReachableTypes;
			/*
			 * For each reachable async type, add settabletypes and subtypes.
			 * Loop until all added
			 */
			do {
				Set<Type> passTypes = asyncReachableTypes.stream()
						.collect(AlcinaCollectors.toLinkedHashSet());
				asyncReachableTypes.stream().filter(reflectableTypes::contains)
						.map(reflectableTypes::typeHierarchy)
						.flatMap(h -> Stream.concat(h.settableTypes.stream(),
								h.subtypes.stream()))
						.forEach(passTypes::add);
				passDelta = passTypes.size() - asyncReachableTypes.size();
				asyncReachableTypes = passTypes;
			} while (passDelta > 0);
			/*
			 * we've computed potential incoming types from deserialization, now
			 * add them to outgoing reachables
			 */
			asyncReachableTypes.stream().filter(reflectableTypes::contains)
					.forEach(type -> {
						addWithReason(Reason.Category.RPC, type);
					});
		}

		protected void addExplicitlyRequiredTypes() {
			reflectableTypes.byType.keySet().stream()
					.filter(t -> !typesVisibleFromSymbolNames
							.contains(t.qualifiedSourceName))
					.filter(peer::hasExplicitTypePermission)
					.filter(peer::permit).forEach(t -> {
						logger.log(TreeLogger.Type.INFO,
								Ax.format("Explicit addition: %s",
										t.qualifiedSourceName));
						addWithReason(Reason.Category.EXPLICIT_PERMISSION, t);
					});
		}

		protected void addFromLegacyAssignments() {
			/*
			 * transitional - add (and only add) all types in the legacy module
			 */
			reflectedTypes.clear();
			reflectableTypes.byType
					.keySet().stream().filter(t -> legacyModuleAssignments
							.isAssignedToModule(t, moduleName))
					.forEach(reflectedTypes::add);
		}

		protected void addFromReachedKeyRegistrations() {
			appRegistrations.entries.stream()
					.filter(e -> e.isVisible(visibleTypes)).forEach(e -> {
						addWithReason(Reason.Category.REGISTRY, e.registered);
					});
		}

		protected void addFromVisibleModuleTypes() {
			typesVisibleFromSymbolNames.stream().map(Type::get)
					.filter(reflectableTypes::contains).forEach(t -> {
						addWithReason(Reason.Category.CODE, t);
					});
		}

		protected void addSupertypes() {
			reflectedTypes.
			// copy to avoid cme
					stream().collect(Collectors.toList()).stream()
					.filter(reflectableTypes::contains)
					.map(reflectableTypes::typeHierarchy)
					.map(t -> t.typeAndSuperTypes).flatMap(Collection::stream)
					.forEach(type -> {
						addWithReason(Category.HIERARCHY, type,
								// no rules used, since these are required
								"required by subtype", true);
					});
		}

		boolean computeReachability() {
			/*
			 * Realising the goal - 10 or so years on - a type is reachable (for
			 * reflection) if:
			 * 
			 * 1. contained in reached types
			 * 
			 * 2. if all registration keys are visible
			 * 
			 * 3. if contained (including subtypes) in types reachable from
			 * reflective rpc requests/responses
			 * 
			 * 4. If explicitly defined (due to issues with reachability
			 * algorithm)
			 * 
			 * 5. if a supertype of [1-5]
			 *
			 */
			addFromVisibleModuleTypes();
			if (legacyModuleAssignments.hasAssignments()
					&& !Boolean.getBoolean("reachability.nonLegacy")) {
				addFromLegacyAssignments();
			} else {
				addFromReachedKeyRegistrations();
				addAsyncReachableTypes();
				addExplicitlyRequiredTypes();
			}
			addSupertypes();
			if (pass > 0) {
				/*
				 * after first pass, don't remove from reachability unless
				 * reached by previous dependencies
				 * 
				 * REVISIT - this prevents some weird unstable looping -
				 * hopefully remove
				 *
				 */
				// previousPassReflectedTypes.stream()
				// .forEach(reflectedTypes::add);
			}
			boolean delta = logAndComputeDelta();
			return delta;
		}
	}
}
