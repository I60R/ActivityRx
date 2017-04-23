package i60r.activityrx;

import android.os.Looper;

import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.plugins.RxJavaPlugins;


/***
 * Created by 160R on 23.04.17.
 */

final class ImmediateIfOnMainThreadScheduler extends Scheduler {


    @Override
    public Worker createWorker() {
        return new ImmediateIfOnMainThreadWorker();
    }

    private static final class ImmediateIfOnMainThreadWorker extends Worker {
        private final Worker delegate = AndroidSchedulers.mainThread().createWorker();
        private volatile boolean disposed = false;


        @Override
        public Disposable schedule(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
            if ((delay == 0L) && (Thread.currentThread() == Looper.getMainLooper().getThread())) {

                if (run == null) throw new NullPointerException("run == null");
                if (unit == null) throw new NullPointerException("unit == null");

                if (disposed) {
                    return Disposables.disposed();
                }

                run = RxJavaPlugins.onSchedule(run);

                try {
                    run.run();
                } catch (Throwable t) {
                    IllegalStateException ie = new IllegalStateException("Fatal Exception thrown on Scheduler.", t);
                    RxJavaPlugins.onError(ie);
                    Thread thread = Thread.currentThread();
                    thread.getUncaughtExceptionHandler().uncaughtException(thread, ie);
                }

                return Disposables.disposed();
            } else {
                return delegate.schedule(run, delay, unit);
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            delegate.dispose();
        }

        @Override
        public boolean isDisposed() {
            return delegate.isDisposed();
        }
    }
}
