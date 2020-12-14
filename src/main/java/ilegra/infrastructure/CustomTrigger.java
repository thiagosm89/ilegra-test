package ilegra.infrastructure;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.Window;

public class CustomTrigger<W extends Window> extends Trigger<Object, W> {
	private static final long serialVersionUID = 1L;

	private final long maxCount;

	private final ReducingStateDescriptor<Long> stateDesc =
			new ReducingStateDescriptor<>("count", new Sum(), LongSerializer.INSTANCE);

	private CustomTrigger(long maxCount) {
		this.maxCount = maxCount;
	}

	@Override
	public TriggerResult onElement(Object element, long timestamp, W window, TriggerContext ctx) throws Exception {
		ctx.registerProcessingTimeTimer(window.maxTimestamp());
		ReducingState<Long> count = ctx.getPartitionedState(stateDesc);
		count.add(1L);
		if (count.get() >= maxCount) {
			count.clear();
			return TriggerResult.FIRE_AND_PURGE;
		}
		return TriggerResult.CONTINUE;
	}

	@Override
	public TriggerResult onEventTime(long time, W window, TriggerContext ctx) {
		return TriggerResult.CONTINUE;
	}

	@Override
	public TriggerResult onProcessingTime(long time, W window, TriggerContext ctx) throws Exception {
		return TriggerResult.FIRE;
	}

	@Override
	public void clear(W window, TriggerContext ctx) throws Exception {
		ctx.deleteProcessingTimeTimer(window.maxTimestamp());
		ctx.getPartitionedState(stateDesc).clear();
	}

	@Override
	public boolean canMerge() {
		return true;
	}

	@Override
	public void onMerge(W window, OnMergeContext ctx) throws Exception {
		ctx.mergePartitionedState(stateDesc);

		// only register a timer if the time is not yet past the end of the merged window
		// this is in line with the logic in onElement(). If the time is past the end of
		// the window onElement() will fire and setting a timer here would fire the window twice.
		long windowMaxTimestamp = window.maxTimestamp();
		if (windowMaxTimestamp > ctx.getCurrentProcessingTime()) {
			ctx.registerProcessingTimeTimer(windowMaxTimestamp);
		}
	}

	@Override
	public String toString() {
		return "CountTrigger(" +  maxCount + ")";
	}

	/**
	 * Creates a trigger that fires once the number of elements in a pane reaches the given count.
	 *
	 * @param maxCount The count of elements at which to fire.
	 * @param <W> The type of {@link Window Windows} on which this trigger can operate.
	 */
	public static <W extends Window> CustomTrigger<W> of(long maxCount) {
		return new CustomTrigger<>(maxCount);
	}

	private static class Sum implements ReduceFunction<Long> {
		private static final long serialVersionUID = 1L;

		@Override
		public Long reduce(Long value1, Long value2) throws Exception {
			return value1 + value2;
		}

	}
}