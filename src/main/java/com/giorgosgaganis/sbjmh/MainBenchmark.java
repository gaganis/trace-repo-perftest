package com.giorgosgaganis.sbjmh;

import java.util.Collections;
import java.util.Map;

import com.giorgosgaganis.sbjmh.noncontention.BlockingInMemoryNoContentionBenchmark;
import com.giorgosgaganis.sbjmh.noncontention.CircularBufferNoContentionBenchmark;
import com.giorgosgaganis.sbjmh.noncontention.LockFreeNoContentionBenchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Created by gaganis on 31/03/17.
 */
public class MainBenchmark {
	public static final int CAPACITY = 2000;
	public static final Map<String, Object> TRACE_INFO = Collections.<String, Object>singletonMap("foo", "bar");

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BlockingInMemoryBenchmark.class.getSimpleName())
				.include(LockFreeInMemoryBenchmark.class.getSimpleName())
				.include(CircularBufferInMemoryBenchmark.class.getSimpleName())
				.include(BlockingInMemoryNoContentionBenchmark.class.getSimpleName())
				.include(LockFreeNoContentionBenchmark.class.getSimpleName())
				.include(CircularBufferNoContentionBenchmark.class.getSimpleName())
				.warmupIterations(5)
				.measurementIterations(5)
				.threads(4)
				.forks(1)
				.build();

		new Runner(opt).run();
	}
}
