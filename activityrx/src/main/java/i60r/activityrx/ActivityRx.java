package i60r.activityrx;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;


/***
 * Created by 160R on 21.04.17.
 */
public final class ActivityRx {
    private static final Map<Class<? extends Activity>, State<? extends Activity>> STATE = new HashMap<>(5);
    private static final Map<Class<? extends Activity>, Set<ObservableEmitter>> EMITTERS = new HashMap<>(20);

    private static final ImmediateIfOnMainThreadScheduler SCHEDULER = new ImmediateIfOnMainThreadScheduler();

    private static Context context = null;

    private static ObservableTransformer<State<? extends Activity>, State<? extends Activity>> hook = new ObservableTransformer<State<? extends Activity>, State<? extends Activity>>() {

                @Override
                public ObservableSource<State<? extends Activity>> apply(@NonNull Observable<State<? extends Activity>> upstream) {
                    return upstream;
                }
            };

    public static void hook(@NonNull final ObservableTransformer<State<? extends Activity>, State<? extends Activity>> hook) {
        ActivityRx.hook = hook;
    }

    public static void init(Application application) {
        if (context == null) {
            context = application;

            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

                @Override
                public void onActivityCreated(Activity a, Bundle in) {
                    setState(a, On.CREATE, in);
                }

                @Override
                public void onActivityStarted(Activity a) {
                    setState(a, On.START, null);
                }

                @Override
                public void onActivityResumed(Activity a) {
                    setState(a, On.RESUME, null);
                }

                @Override
                public void onActivityPaused(Activity a) {
                    setState(a, On.PAUSE, null);
                }

                @Override
                public void onActivityStopped(Activity a) {
                    setState(a, On.STOP, null);
                }

                @Override
                public void onActivityDestroyed(Activity a) {
                    setState(a, On.DESTROY, null);
                }

                @Override
                public void onActivitySaveInstanceState(Activity a, Bundle to) {
                    setState(a, On.SAVE, to);
                }


            });
        } else {
            throw new ExceptionInInitializerError("already initialized");
        }
    }

    private static void setState(Activity a, On on, @Nullable Bundle bundle) {
        Class<? extends Activity> key = a.getClass();

        State<? extends Activity> current;
        if (on == On.STOP) {
            current = new State<>(key, on, null, null);
        } else {
            current = new State<>(key, on, a, bundle);
        }

        if (on == On.DESTROY) {
            STATE.remove(key);
        } else {
            STATE.put(key, current);
        }

        for (Map.Entry<Class<? extends Activity>, Set<ObservableEmitter>> entry : EMITTERS.entrySet()) {
            if (key == entry.getKey()) {
                for (ObservableEmitter emitter : entry.getValue()) {
                    emitter.onNext(current);
                }
            }
        }
    }


    private static <A extends Activity> Observable<State<A>> subscribeUntilDestroy(ActivityObservableOnSubscribe<A> behavior) {
        return subscribe(behavior)
                .takeUntil(behavior);
    }

    private static <A extends Activity> Observable<State<A>> subscribe(ActivityObservableOnSubscribe<A> behavior) {
        return Observable
                .create(behavior)
                .subscribeOn(SCHEDULER)
                .unsubscribeOn(SCHEDULER)
                .observeOn(SCHEDULER)
                .compose(hook)
                .map(behavior)
                .doOnNext(behavior);
    }


    public static <A extends Activity> Observable<State<A>> observe(final Class<A> key) {
        return subscribe(new ActivityObservableOnSubscribe<>(STATE, EMITTERS, key));
    }

    public static <A extends Activity> Observable<State<A>> bind(final Class<A> key) {
        return subscribeUntilDestroy(new ActivityObservableOnSubscribe<>(STATE, EMITTERS, key));
    }

    public static <A extends Activity> Observable<State<A>> swap(final Activity current, final Class<A> key) {
        return subscribeUntilDestroy(new ActivityObservableOnSubscribe<A>(STATE, EMITTERS, key){

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(current, key));
                current.finish();
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Class<A> key) {
        return subscribeUntilDestroy(new ActivityObservableOnSubscribe<A>(STATE, EMITTERS, key) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(context, key));
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Class<A> key, final Bundle extras) {
        return subscribeUntilDestroy(new ActivityObservableOnSubscribe<A>(STATE, EMITTERS, key) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(context, key).putExtras(extras));
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Class<A> key, final Intent intent) {
        return subscribeUntilDestroy(new ActivityObservableOnSubscribe<A>(STATE, EMITTERS, key) {

            @Override
            protected void onAbsent() {
                context.startActivity(intent.setClass(context, key));
            }
        });
    }

}
