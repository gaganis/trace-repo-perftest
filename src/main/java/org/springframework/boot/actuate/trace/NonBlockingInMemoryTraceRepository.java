/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gaganis on 30/03/17.
 * @author Giorgos Gaganis
 */
public class NonBlockingInMemoryTraceRepository implements TraceRepository {

	private volatile int capacity = 100;
	private volatile boolean reverse;

	private final ConcurrentLinkedDeque<Trace> traces = new ConcurrentLinkedDeque<>();

	private final AtomicInteger storedCount = new AtomicInteger();
	private final AtomicBoolean headAccessClaimLock = new AtomicBoolean();

	/**
	 * Flag to say that the repository lists traces in reverse order.
	 *
	 * @param reverse flag value (default true)
	 */
	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	/**
	 * Set the capacity of the in-memory repository.
	 *
	 * @param capacity the capacity
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public List<Trace> findAll() {
		int localCapacity = this.capacity;
		ArrayList<Trace> result = new ArrayList<>(localCapacity);

		while(!headAccessClaimLock.compareAndSet(false, true));

		Iterator<Trace> descendingIterator = this.traces.descendingIterator();
		int count = 0;
		while (descendingIterator.hasNext() && count < localCapacity) {
			result.add(descendingIterator.next());
		}

		//Re-Read latest capacity from field
		localCapacity = this.capacity;
		int localStoredCount = this.storedCount.get();
		if(localStoredCount > localCapacity) {
			removeOverCapacityEntries(localCapacity);
		}
		headAccessClaimLock.set(false);
		if (this.reverse) {
			Collections.reverse(result);
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public void add(Map<String, Object> traceInfo) {
		Trace trace = new Trace(new Date(), traceInfo);
		this.traces.add(trace);

		int localStoredCount = this.storedCount.incrementAndGet();
		int localCapacity = this.capacity;

		if(localStoredCount > localCapacity) {
			if (headAccessClaimLock.compareAndSet(false, true)) {
				removeOverCapacityEntries(localCapacity);
				headAccessClaimLock.set(false);
			}
		}
	}

	private void removeOverCapacityEntries(int localCapacity) {
		int localStoredCount = this.storedCount.getAndSet(localCapacity);

		int overCapacity = localStoredCount - localCapacity;

		for (; overCapacity > 0; overCapacity--) {
			this.traces.remove();
		}
	}
}