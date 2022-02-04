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

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;

/**
 * Calculates reachable reflective classes
 */
@LinkerOrder(Order.POST)
@Shardable
public class ReflectionReachabilityLinker extends Linker {
	@Override
	public String getDescription() {
		return "Calculates reachable reflective classes";
	}

	@Override
	public ArtifactSet link(TreeLogger logger, LinkerContext context,
			ArtifactSet artifacts, boolean onePermutation) {
		// boolean reportFilesPresent = anyReportFilesPresent(artifacts);
		// boolean metricsPresent = anyCompilerMetricsPresent(artifacts);
		// if (!reportFilesPresent && !metricsPresent) {
		// return artifacts;
		// }
		// artifacts = new ArtifactSet(artifacts);
		// if (!onePermutation) {
		// buildCompilerMetricsXml(artifacts);
		// }
		// if (reportFilesPresent) {
		// if (onePermutation) {
		// emitPermutationDescriptions(artifacts);
		// } else {
		// buildTopLevelFiles(logger, artifacts);
		// }
		// }
		return artifacts;
	}
}
