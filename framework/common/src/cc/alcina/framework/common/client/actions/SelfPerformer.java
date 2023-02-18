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

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

/**
 *
 * <p>
 * SelfPerformer exposes methods for processing a task outside a JobContext -
 * call run() (or performAction()) directly on them to bypass the Job system -
 * perform() will execute in a job context
 *
 * @author Nick Reddel
 */
public interface SelfPerformer extends Task, TaskPerformer, ThrowingRunnable {
	@Override
	default void performAction(Task task) throws Exception {
		run();
	}
}
