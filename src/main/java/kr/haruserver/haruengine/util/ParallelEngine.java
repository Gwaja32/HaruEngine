package kr.haruserver.haruengine.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ParallelEngine {
    // [Spark 최적화] 메인 틱 스레드를 방해하지 않도록 스레드 개수를 조정하고 우선순위를 낮춤
    private static final int THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors());

    public static final ExecutorService WORKER_POOL = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
        Thread t = new Thread(r);
        // [핵심] 우선순위를 최저(1)로 설정하여 메인 서버 틱 스레드(5)의 자원을 뺏지 않음
        t.setPriority(Thread.MIN_PRIORITY);
        t.setName("Haru-Worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * 리스트 데이터를 병렬로 처리하고 결과를 합치는 범용 엔진
     */
    public static <T, R> R computeParallel(List<T> data, Function<List<T>, R> task, BiFunction<R, R, R> reducer) {
        int size = data.size();

        // [Spark 최적화] 임계값을 16 -> 64로 상향.
        // 너무 적은 양을 병렬화하면 스레드 분배 비용이 더 큼.
        if (size < 64) return task.apply(data);

        int chunks = THREAD_COUNT;
        int chunkSize = (size + chunks - 1) / chunks;
        List<CompletableFuture<R>> futures = new ArrayList<>();

        for (int i = 0; i < chunks; i++) {
            int start = i * chunkSize;
            if (start >= size) break;
            int end = Math.min(start + chunkSize, size);

            List<T> subList = data.subList(start, end);
            futures.add(CompletableFuture.supplyAsync(() -> task.apply(subList), WORKER_POOL));
        }

        // [최적화] Stream.reduce 대신 일반 루프로 결과 병합 (가비지 생성 감소)
        R finalResult = null;
        for (CompletableFuture<R> future : futures) {
            R partialResult = future.join();
            if (finalResult == null) {
                finalResult = partialResult;
            } else {
                finalResult = reducer.apply(finalResult, partialResult);
            }
        }

        return finalResult;
    }
}