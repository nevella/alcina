/*
 * Copyright 2009 Google Inc.
 *
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
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.ThreeWaySetResult;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.ModuleTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.ModuleTypes.TypeList;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Type;

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
 *   - (implementation: those registrations are omitted from AppImplRegistrations.entries)
 * - for classes which are reachable by async serialization (incoming rpc), mark their *direct* subtypes as reachable
 * </pre>
 */
@LinkerOrder(Order.POST)
@Shardable
public class ReflectionReachabilityLinker extends Linker {
	static int pass = 1;

	@Override
	public String getDescription() {
		return "Calculates reachable reflective classes";
	}

	/*
	 * There is no prettier way to get this information without hacking the
	 * compiler directly
	 */
	@Override
	public ArtifactSet link(TreeLogger logger, LinkerContext context,
			ArtifactSet artifacts, boolean onePermutation) {
		SortedSet<StandardCompilationResult> compilationResults = artifacts
				.find(StandardCompilationResult.class);
		// if obf, ignore for reachability filtering
		// TODO - 2022 - obf probably elides because of the getNames()
		// computation - check
		if (compilationResults.size() > 0 && onePermutation
				&& !((StandardLinkerContext) context).isOutputCompact()) {
			FragmentIdToName idToName = new FragmentIdToName(artifacts);
			Multiset<Integer, Set<String>> reachedClassOrConstantNames = getNames(
					compilationResults);
			File typesFile = ReachabilityData.getCacheFile("reachability.json");
			File registrationsFile = ReachabilityData.getRegistryFile();
			File reflectableTypesFile = ReachabilityData
					.getReflectableTypesFile();
			ModuleTypes moduleTypes = ReachabilityData
					.deserialize(ModuleTypes.class, typesFile);
			AppImplRegistrations appRegistrations = ReachabilityData
					.deserialize(AppImplRegistrations.class, registrationsFile);
			AppReflectableTypes reflectableTypes = ReachabilityData.deserialize(
					AppReflectableTypes.class, reflectableTypesFile);
			reflectableTypes.buildLookup();
			boolean delta = false;
			com.google.gwt.dev.Compiler.recompile = false;
			List<Integer> fragmentIds = idToName.getKeysInDependencyOrder();
			for (Integer fragmentId : fragmentIds) {
				Set<String> typesVisibleFromSymbolNames = reachedClassOrConstantNames
						.get(fragmentId);
				// can be null if no symbols (e.g. leftover fragment for single
				// split point program)
				typesVisibleFromSymbolNames = typesVisibleFromSymbolNames == null
						? Collections.emptySet()
						: typesVisibleFromSymbolNames;
				String moduleName = idToName.idToName(fragmentId);
				delta |= applyReachedNames(logger, moduleName,
						typesVisibleFromSymbolNames, moduleTypes,
						appRegistrations, reflectableTypes, idToName);
			}
			if (delta) {
				ReachabilityData.serialize(moduleTypes, typesFile);
				int maxPass = Integer.getInteger("reachability.pass", 25);
				if (pass++ < maxPass) {
					logger.log(TreeLogger.Type.INFO, Ax.format(
							"Recompile - reflection changes - pass %s", pass));
					com.google.gwt.dev.Compiler.recompile = true;
				}
			}
		}
		return artifacts;
	}

	/**
	 * @param idToName
	 *
	 */
	private boolean applyReachedNames(TreeLogger logger, String moduleName,
			Set<String> typesVisibleFromSymbolNames, ModuleTypes moduleTypes,
			AppImplRegistrations appRegistrations,
			AppReflectableTypes reflectableTypes, FragmentIdToName idToName) {
		TypeList typeList = moduleTypes.ensureTypeList(moduleName);
		Set<Type> dependencyModuleTypes = moduleTypes
				.typesFor(idToName.computeDepdendencyNames(moduleName));
		Set<Type> incomingReflectedModuleTypes = typeList.types.stream()
				.collect(AlcinaCollectors.toLinkedHashSet());
		// copy
		Set<Type> previouslyVisibleTypes = incomingReflectedModuleTypes.stream()
				.collect(AlcinaCollectors.toLinkedHashSet());
		Set<Type> visibleTypes = typesVisibleFromSymbolNames.stream()
				.map(Type::get).collect(Collectors.toSet());
		/*
		 * The key ... 10 or so years on - a type is reachable (for reflection)
		 * if contained in reached types *or* if all registration keys are
		 * visible
		 *
		 * Note - module dependencies not implemented (yet) - that will add to
		 * the visible keys set
		 */
		Set<Type> outgoingReflectedModuleTypes = typesVisibleFromSymbolNames
				.stream().map(Type::get).filter(reflectableTypes::contains)
				.collect(AlcinaCollectors.toLinkedHashSet());
		Set<AppImplRegistrations.Entry> addedFromRegistration = new LinkedHashSet<>();
		Set<Type> addedFromHierarchy = new LinkedHashSet<>();
		Set<Type> addedFromAsyncSerialization = new LinkedHashSet<>();
		appRegistrations.entries.stream().filter(e -> e.isVisible(visibleTypes))
				.filter(e -> !dependencyModuleTypes.contains(e.registered))
				.forEach(e -> {
					outgoingReflectedModuleTypes.add(e.registered);
					if (previouslyVisibleTypes.add(e.registered)) {
						addedFromRegistration.add(e);
					}
				});
		int passDelta = 0;
		Set<Type> initialAsyncReachableTypes = visibleTypes.stream()
				.filter(reflectableTypes::contains)
				.map(reflectableTypes::typeHierarchy)
				.flatMap(th -> th.asyncSerializableTypes.stream())
				.collect(AlcinaCollectors.toLinkedHashSet());
		Set<Type> asyncReachableTypes = initialAsyncReachableTypes;
		/*
		 * For each reachable async type, add settabletypes and subtypes. Loop
		 * until all added
		 */
		do {
			Set<Type> passTypes = asyncReachableTypes.stream()
					.collect(AlcinaCollectors.toLinkedHashSet());
			Type orElse = passTypes.stream()
					.filter(t -> t.qualifiedSourceName.equals(
							"cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta"))
					.findFirst().orElse(null);
			boolean b = reflectableTypes.contains(orElse);
			asyncReachableTypes.stream().filter(reflectableTypes::contains)
					.map(reflectableTypes::typeHierarchy)
					.flatMap(h -> Stream.concat(h.settableTypes.stream(),
							h.subtypes.stream()))
					.forEach(passTypes::add);
			passDelta = passTypes.size() - asyncReachableTypes.size();
			asyncReachableTypes = passTypes;
		} while (passDelta > 0);
		asyncReachableTypes.stream().filter(reflectableTypes::contains)
				.filter(t -> !dependencyModuleTypes.contains(t))
				.forEach(type -> {
					outgoingReflectedModuleTypes.add(type);
					if (previouslyVisibleTypes.add(type)) {
						addedFromAsyncSerialization.add(type);
					}
				});
		// copy to avoid cme
		outgoingReflectedModuleTypes.stream().collect(Collectors.toList())
				.stream().filter(reflectableTypes::contains)
				.map(reflectableTypes::typeHierarchy)
				.map(t -> t.typeAndSuperTypes).flatMap(Collection::stream)
				.filter(t -> !dependencyModuleTypes.contains(t))
				.forEach(type -> {
					outgoingReflectedModuleTypes.add(type);
					if (previouslyVisibleTypes.add(type)) {
						addedFromHierarchy.add(type);
					}
				});
		if (pass > 0) {
			/*
			 * after first pass, don't remove from reachability unless reached
			 * by previous dependencies
			 *
			 */
			incomingReflectedModuleTypes.stream()
					.forEach(outgoingReflectedModuleTypes::add);
		}
		outgoingReflectedModuleTypes.removeAll(dependencyModuleTypes);
		ThreeWaySetResult<Type> split = CommonUtils.threeWaySplit(
				incomingReflectedModuleTypes, outgoingReflectedModuleTypes);
		typeList.types = outgoingReflectedModuleTypes.stream()
				.collect(Collectors.toList());
		logger.log(TreeLogger.Type.INFO, Ax.format("Reachability [%s] - :: %s",
				moduleName, split.toSizes()));
		int maxEmit = Integer.getInteger("reachability.emit", 5);
		addedFromRegistration.stream().limit(maxEmit).forEach(t -> {
			logger.log(TreeLogger.Type.INFO, Ax.format("\t[r]: %s", t));
		});
		addedFromHierarchy.stream().limit(maxEmit).forEach(t -> {
			logger.log(TreeLogger.Type.INFO, Ax.format("\t[h]: %s", t));
		});
		addedFromAsyncSerialization.stream().limit(maxEmit).forEach(t -> {
			logger.log(TreeLogger.Type.INFO, Ax.format("\t[s]: %s", t));
		});
		boolean delta = split.firstOnly.size() > 0
				|| split.secondOnly.size() > 0;
		return delta;
	}

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
				// unknown -> initial
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

	public static class ReachabilityArtifact extends SyntheticArtifact {
		public ReachabilityArtifact(String log) {
			super(ReflectionReachabilityLinker.class, "reachabilityData",
					log.getBytes(StandardCharsets.UTF_8));
		}
	}

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
}
