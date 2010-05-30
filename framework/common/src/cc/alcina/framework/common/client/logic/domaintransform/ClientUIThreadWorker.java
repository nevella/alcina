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
package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.gwt.user.client.Timer;

public abstract class ClientUIThreadWorker {
	protected int iterationCount = 200;

	protected int targetIterationTimeMs = 200;

	protected int index;

	protected int lastPassIterationsPerformed = 0;

	protected int allocateToNonWorkerFactor = 2;

	private long startTime;

	protected ClientUIThreadWorker() {
	}

	protected ClientUIThreadWorker(int iterationCount, int targetIterationTimeMs) {
		this.iterationCount = iterationCount;
		this.targetIterationTimeMs = targetIterationTimeMs;
	}

	public void start() {
		startTime = System.currentTimeMillis();
		iterate();
	}

	protected void iterate() {
		if (isComplete()) {
			ClientLayerLocator.get().notifications().log(
					CommonUtils.format("Itr [%1] [Complete] - %2 ms",
							CommonUtils.simpleClassName(getClass()), System
									.currentTimeMillis()
									- startTime));
			onComplete();
			return;
		}
		long t0 = System.currentTimeMillis();
		performIteration();
		long t1 = System.currentTimeMillis();
		int timeTaken = (int) (t1 - t0);
		timeTaken = Math.min(timeTaken, targetIterationTimeMs * 10);
		// no totally lost loops if debugging
		if (lastPassIterationsPerformed == iterationCount) {
			if (timeTaken * 2 < targetIterationTimeMs) {
				iterationCount *= 2;
			}
			if (timeTaken > targetIterationTimeMs * 2) {
				iterationCount /= 2;
			}
			iterationCount = Math.max(iterationCount, 10);
		}
		ClientLayerLocator.get().notifications().log(
				CommonUtils.format("Itr [%1] [x%3] - %2 ms", CommonUtils
						.simpleClassName(getClass()), timeTaken,
						lastPassIterationsPerformed));
		new Timer() {
			@Override
			public void run() {
				iterate();
			}
		}.schedule((int) timeTaken * allocateToNonWorkerFactor + 1);
	}

	protected abstract void onComplete();

	protected abstract boolean isComplete();

	protected abstract void performIteration();
}