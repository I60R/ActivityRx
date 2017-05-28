package i60r.activityrx;


import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;


/***
 * Created by 160R on 21.04.17.
 */
public final class ActivityRx {
    private static final Set<State<? extends Activity>> STATES = new HashSet<>(5);
    private static final Map<String, Set<ObservableEmitter>> SUBSCRIPTIONS = new HashMap<>(20);
    private static final Set<ObservableEmitter> BUFFER = new HashSet<>(10);
    private static final Subject<State<? extends Activity>> EVENTS = PublishSubject.create();

    private static final ImmediateIfOnMainThreadScheduler SCHEDULER = new ImmediateIfOnMainThreadScheduler();

    private static ObservableTransformer<State<? extends Activity>, State<? extends Activity>> hook = new DefaultOnEventsTransformer();

    private static Application context = null;


    public static void modify(@NonNull final ObservableTransformer<State<? extends Activity>, State<? extends Activity>> hook) {
        ObjectHelper.requireNonNull(hook, "can't be null");
        if (!(ActivityRx.hook instanceof DefaultOnEventsTransformer)) throw new IllegalAccessError("hook can't be changed");
        ActivityRx.hook = hook;
    }

    public static void init(Application application) {
        ObjectHelper.requireNonNull(application, "can't be null");
        ActivityRx.context = application;

        application.registerComponentCallbacks(new ComponentCallbacks() {

            @Override
            public void onConfigurationChanged(Configuration newConfig) {
                for (State<? extends Activity> state : STATES) {
                    State<Activity> newState = new State<>(state.component, On.CHANGE, state.ui, null);
                    EVENTS.onNext(newState);

                    Set<ObservableEmitter> emitters = SUBSCRIPTIONS.get(state.component);
                    if (emitters != null) {
                        BUFFER.addAll(emitters);
                        for (ObservableEmitter emitter : BUFFER) {
                            emitter.onNext(newState);
                        }
                        BUFFER.clear();
                    }
                }
            }

            @Override
            public void onLowMemory() {
            }
        });

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle in) {
                setState(activity.getComponentName().getClassName(), On.CREATE, activity, in);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                setState(activity.getComponentName().getClassName(), On.START, activity, null);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                setState(activity.getComponentName().getClassName(), On.RESUME, activity, null);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                setState(activity.getComponentName().getClassName(), On.PAUSE, activity, null);
            }

            @Override
            public void onActivityStopped(Activity activity) {
                setState(activity.getComponentName().getClassName(), On.STOP, null, null);
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                setState(activity.getComponentName().getClassName(), On.DESTROY, activity, null);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle to) {
                setState(activity.getComponentName().getClassName(), On.SAVE, activity, to);
            }
        });
    }

    private static void setState(@NonNull String component, On on, Activity activity, Bundle bundle) {
        State<? extends Activity> newState = new State<>(component, on, activity, bundle);
        EVENTS.onNext(newState);

        Iterator<State<? extends Activity>> iterator = STATES.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().component.equals(component)) {
                iterator.remove();
                break;
            }
        }
        if (newState.on != On.DESTROY) {
            STATES.add(newState);
        }

        for (Map.Entry<String, Set<ObservableEmitter>> subscription : SUBSCRIPTIONS.entrySet()) {
            if (subscription.getKey().equals(component)) {
                BUFFER.addAll(subscription.getValue());
            }
        }
        for (ObservableEmitter emitter : BUFFER) {
            emitter.onNext(newState);
        }
        BUFFER.clear();
    }


    private static <A extends Activity> Observable<State<A>> subscribeUntilDestroy(ActivityObservableBehavior<A> behavior) {
        return subscribe(behavior)
                .takeUntil(behavior);
    }

    private static <A extends Activity> Observable<State<A>> subscribe(ActivityObservableBehavior<A> behavior) {
        ObjectHelper.requireNonNull(context, "must be initialized");
        return Observable
                .create(behavior)
                .doOnDispose(behavior)
                .subscribeOn(SCHEDULER)
                .unsubscribeOn(SCHEDULER)
                .observeOn(SCHEDULER)
                .compose(hook)
                .map(behavior)
                .doOnNext(behavior);
    }


    public static Observable<State<? extends Activity>> events() {
        return EVENTS
                .compose(hook);
    }

    public static <A extends Activity> Observable<State<A>> observe(final Class<A> component) {
        return subscribe(new ActivityObservableBehavior<>(STATES, SUBSCRIPTIONS, component));
    }

    public static <A extends Activity> Observable<State<A>> observe(final A activity) {
        return subscribe(new ActivityObservableBehavior<A>(STATES, SUBSCRIPTIONS, activity.getComponentName().getClassName()));
    }

    public static <A extends Activity> Observable<State<A>> bind(final Class<A> component) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<>(STATES, SUBSCRIPTIONS, component));
    }

    public static <A extends Activity> Observable<State<A>> bind(final A activity) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATES, SUBSCRIPTIONS, activity.getComponentName().getClassName()));
    }


    public static <A extends Activity> Observable<State<A>> start(final Class<A> component) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATES, SUBSCRIPTIONS, component) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(context, component));
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Class<A> component, final Bundle extras) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATES, SUBSCRIPTIONS, component) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(context, component).putExtras(extras));
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Intent intent) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATES, SUBSCRIPTIONS, intent.getComponent().getClassName()) {

            @Override
            protected void onAbsent() {
                context.startActivity(intent);
            }
        });
    }

}
