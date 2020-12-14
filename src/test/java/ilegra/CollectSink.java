package ilegra;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CollectSink<T> extends RichSinkFunction<T> implements Serializable {

    private final String id = UUID.randomUUID().toString();
    private static final Map<String, CompletableFuture<List<?>>> RESULTS = new HashMap<>();
    private static final Map<String, List<Object>> VALUES = new HashMap<>();

    @Override
    public void open(Configuration parameters) {
        RESULTS.put(id, new CompletableFuture<>());
    }

    @Override
    public void invoke(T value, Context context) {
        final CompletableFuture<List<?>> fb = RESULTS.get(this.id);

        VALUES.computeIfAbsent(this.id, __ -> new ArrayList<>());

        VALUES.computeIfPresent(this.id, (k, v) -> {
            v.add(value);
            return v;
        });

        final List<?> values = VALUES.getOrDefault(this.id, new ArrayList<>());

        fb.complete(values);
    }

    public CompletableFuture<List<T>> future() {
        return (CompletableFuture<List<T>>) (Object) RESULTS.get(id);
    }

    public void cleanup() {
        RESULTS.remove(this.id);
        VALUES.remove(this.id);
    }
}