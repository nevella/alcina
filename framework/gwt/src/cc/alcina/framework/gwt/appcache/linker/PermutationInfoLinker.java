/*
 * Copyright 2008 Google Inc.
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
package cc.alcina.framework.gwt.appcache.linker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SoftPermutation;
import com.google.gwt.core.ext.linker.impl.PermutationsUtil;
import com.google.gwt.core.ext.linker.impl.PropertiesMappingArtifact;
import com.google.gwt.core.ext.linker.impl.SelectionInformation;
import com.google.gwt.core.linker.CrossSiteIframeLinker;

/**
 * List permutation info in a nice, digestible form
 */
@LinkerOrder(Order.POST)
@Shardable
public class PermutationInfoLinker extends AbstractLinker {
	EmittedArtifact permutationInfo = null;

	protected static PermutationsUtil permutationsUtil = new PermutationsUtil();

	public PermutationInfoLinker() {
	}

	@Override
	public String getDescription() {
		return "Permutation info linker";
	}

	@Override
	public ArtifactSet link(TreeLogger logger, LinkerContext context,
			ArtifactSet artifacts, boolean onePermutation)
			throws UnableToCompleteException {
		if (onePermutation) {
			ArtifactSet toReturn = new ArtifactSet(artifacts);
			ArtifactSet writableArtifacts = new ArtifactSet(artifacts);
			for (CompilationResult compilation : artifacts
					.find(CompilationResult.class)) {
				// pass a writable set so that other stages can use this set for
				// temporary storage
				toReturn.addAll(doEmitCompilation(logger, context, compilation,
						writableArtifacts));
			}
			return toReturn;
		} else {
			permutationsUtil.setupPermutationsMap(artifacts);
			ArtifactSet toReturn = new ArtifactSet(artifacts);
			maybeOutputPropertyMap(logger, context, toReturn);
			return toReturn;
		}
	}

	// Output compilation-mappings.txt
	protected void maybeOutputPropertyMap(TreeLogger logger,
			LinkerContext context, ArtifactSet toReturn) {
		if (permutationsUtil.getPermutationsMap() == null
				|| permutationsUtil.getPermutationsMap().isEmpty()) {
			return;
		}
		PropertiesMappingArtifact mappingArtifact = new PropertiesMappingArtifact(
				CrossSiteIframeLinker.class,
				permutationsUtil.getPermutationsMap());
		toReturn.add(mappingArtifact);
		EmittedArtifact serializedMap;
		try {
			String mappings = mappingArtifact.getSerialized();
			serializedMap = emitString(logger, mappings,
					"compilation-mappings.txt");
			// TODO(unnurg): make this Deploy
			serializedMap.setVisibility(Visibility.Public);
			toReturn.add(serializedMap);
		} catch (UnableToCompleteException e) {
			e.printStackTrace();
		}
	}

	protected Collection<Artifact<?>> doEmitCompilation(TreeLogger logger,
			LinkerContext context, CompilationResult result,
			ArtifactSet artifacts) throws UnableToCompleteException {
		Collection<Artifact<?>> toReturn = new ArrayList<Artifact<?>>();
		toReturn.addAll(emitSelectionInformation(result.getStrongName(), result));
		return toReturn;
	}

	private List<Artifact<?>> emitSelectionInformation(String strongName,
			CompilationResult result) {
		List<Artifact<?>> emitted = new ArrayList<Artifact<?>>();
		for (SortedMap<SelectionProperty, String> propertyMap : result
				.getPropertyMap()) {
			TreeMap<String, String> propMap = new TreeMap<String, String>();
			for (Map.Entry<SelectionProperty, String> entry : propertyMap
					.entrySet()) {
				propMap.put(entry.getKey().getName(), entry.getValue());
			}
			// The soft properties may not be a subset of the existing set
			for (SoftPermutation soft : result.getSoftPermutations()) {
				// Make a copy we can add add more properties to
				TreeMap<String, String> softMap = new TreeMap<String, String>(
						propMap);
				// Make sure this SelectionInformation contains the soft
				// properties
				for (Map.Entry<SelectionProperty, String> entry : soft
						.getPropertyMap().entrySet()) {
					softMap.put(entry.getKey().getName(), entry.getValue());
				}
				emitted.add(new SelectionInformation(strongName, soft.getId(),
						softMap));
			}
		}
		return emitted;
	}
}
