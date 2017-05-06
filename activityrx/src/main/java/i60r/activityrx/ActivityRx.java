package i60r.activityrx;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;
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
    private static final Map<String, State<? extends Activity>> STATE = new HashMap<>(5);
    private static final Map<String, Set<ObservableEmitter>> EMITTERS = new HashMap<>(20);

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
        String component = a.getComponentName().getClassName();

        State<? extends Activity> current;
        if (on == On.STOP) {
            current = new State<>(component, on, null, null);
        } else {
            current = new State<>(component, on, a, bundle);
        }

        if (on == On.DESTROY) {
            STATE.remove(component);
        } else {
            STATE.put(component, current);
        }

        for (Map.Entry<String, Set<ObservableEmitter>> entry : EMITTERS.entrySet()) {
            if (component.equals(entry.getKey())) {
                for (ObservableEmitter emitter : entry.getValue()) {
                    emitter.onNext(current);
                }
            }
        }
    }


    private static <A extends Activity> Observable<State<A>> subscribeUntilDestroy(ActivityObservableBehavior<A> behavior) {
        return subscribe(behavior)
                .takeUntil(behavior);
    }

    private static <A extends Activity> Observable<State<A>> subscribe(ActivityObservableBehavior<A> behavior) {
        return Observable
                .create(behavior)
                .subscribeOn(SCHEDULER)
                .unsubscribeOn(SCHEDULER)
                .observeOn(SCHEDULER)
                .compose(hook)
                .map(behavior)
                .doOnNext(behavior);
    }


    public static <A extends Activity> Observable<State<A>> observe(final Class<A> component) {
        return subscribe(new ActivityObservableBehavior<>(STATE, EMITTERS, component));
    }

    public static <A extends Activity> Observable<State<A>> observe(final A activity) {
        return subscribe(new ActivityObservableBehavior<A>(STATE, EMITTERS, activity.getComponentName().getClassName()));
    }

    public static <A extends Activity> Observable<State<A>> bind(final Class<A> component) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<>(STATE, EMITTERS, component));
    }

    public static <A extends Activity> Observable<State<A>> bind(final A activity) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATE, EMITTERS, activity.getComponentName().getClassName()));
    }

    public static <A extends Activity> Observable<State<A>> swap(final Activity current, final Class<A> component) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATE, EMITTERS, component) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(current, component));
                current.finish();
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Class<A> component) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATE, EMITTERS, component) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(context, component));
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Class<A> component, final Bundle extras) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATE, EMITTERS, component) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(context, component).putExtras(extras));
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Class<A> component, final Intent intent) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATE, EMITTERS, component) {

            @Override
            protected void onAbsent() {
                context.startActivity(intent.setClass(context, component));
            }
        });
    }

}
