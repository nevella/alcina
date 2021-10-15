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
package cc.alcina.framework.common.client.actions;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.lock.JobResource;

/**
 * 
 * @author Nick Reddel
 */
public interface TaskPerformer<T extends Task> {
	default boolean canAbort(Task task) {
		return false;
	}

	default boolean checkCanPerformConcurrently(Task task) {
		return true;
	}

	default boolean deferMetadataPersistence(Job job) {
		return false;
	}

	default boolean endInLockedSection() {
		return false;
	}

	@JsonIgnore
	default List<JobResource> getResources() {
		return Collections.emptyList();
	}

	default int getVersionNumber() {
		return 0;
	}

	default void onAfterEnd() {
	}

	default void onBeforeEnd() {
	}

	default void onChildCompletion() {
	}

	void performAction(T task) throws Exception;
}
