/*
 * Copyright (C) 2015-2018 SoftIndex LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datakernel.examples;

import io.datakernel.di.Injector;
import io.datakernel.di.module.AbstractModule;
import io.datakernel.di.module.Provides;
import io.datakernel.worker.*;

public class WorkerPoolModuleExample extends AbstractModule {
	@Provides
	WorkerPool workerPool(WorkerPools workerPools) {
		return workerPools.createPool(4);
	}

	@Provides
	@Worker
	String string(@WorkerId int workerId) {
		return "Hello from worker #" + workerId;
	}

	public static void main(String[] args) {
		Injector injector = Injector.of(new WorkerPoolModule(), new WorkerPoolModuleExample());
		WorkerPool workerPool = injector.getInstance(WorkerPool.class);
		WorkerPool.Instances<String> strings = workerPool.getInstances(String.class);
		strings.forEach(System.out::println);
	}
}
