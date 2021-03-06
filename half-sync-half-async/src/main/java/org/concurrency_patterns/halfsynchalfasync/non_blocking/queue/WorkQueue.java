package org.concurrency_patterns.halfsynchalfasync.non_blocking.queue;

import org.concurrency_patterns.halfsynchalfasync.service.AsciiArtGenerationService;
import org.concurrency_patterns.halfsynchalfasync.non_blocking.dispatcher.NonBlockingDispatcher;

import java.util.concurrent.*;

/**
 * Queues incoming requests and notifies the dispatcher when the response is ready.
 *
 * @author: Tomas Zezula
 * Date: 24/08/2014
 */
public class WorkQueue {

    // Activation List: incoming requests (tasks) are put into a queue
    private volatile BlockingQueue<Callable<Boolean>> taskQueue = new LinkedBlockingQueue<>();

    public WorkQueue(final NonBlockingDispatcher dispatcher) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(taskQueue.isEmpty())
                    ;   // await tasks

                // This is the actual task scheduler. It only allows for a single task at a time.
                ExecutorService executorService = Executors.newSingleThreadExecutor();

                try {
                    while (true) {
                        if (taskQueue.isEmpty()) break;

                        // at some point in the future the calculated value will be available
                        Future<Boolean> future = executorService.submit(taskQueue.take());
                        while (!future.isDone())
                            ;   // wait until the calculation is complete

                        // publish the result
                        dispatcher.onResult(future.get());

                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Task execution was failed!");
                }
                finally {
                    executorService.shutdown();
                }
            }
        }).start();

    }

    public void submit(String imgPath, String outPath) {
        submit(createTask(imgPath, outPath));
    }

    private void submit(Callable<Boolean> task) {
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            throw new RuntimeException("Task scheduling was interrupted!");
        }
    }

    private Callable<Boolean> createTask(final String imgPath, final String outPath) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return new AsciiArtGenerationService().convertToAscii(imgPath, outPath);
            }
        };
    }
}
