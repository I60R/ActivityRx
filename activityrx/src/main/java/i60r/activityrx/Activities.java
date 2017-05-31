package i60r.activityrx;


import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import com.github.kubode.rx.android.schedulers.ImmediateLooperScheduler;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.subjects.PublishSubject;


/***
 * Created by 160R on 21.04.17.
 */
public final class Activities {
    private static final PublishSubject<State<? extends Activity>> EVENTS = PublishSubject.create();
    private static final LinkedHashSet <State<? extends Activity>> STATES = new LinkedHashSet<>(10);

    private static final LinkedHashMap<String, LinkedHashSet<ObservableEmitter>> EMITTERS = new LinkedHashMap<>(10);
    private static final LinkedHashSet                      <ObservableEmitter>  BUFFER = new LinkedHashSet<>(10);

    private static ObservableTransformer<State<? extends Activity>, State<? extends Activity>> hook = new DefaultOnEventsTransformer();

    private static Application context = null;


    public static void modify(@NonNull final ObservableTransformer<State<? extends Activity>, State<? extends Activity>> hook) {
        ObjectHelper.requireNonNull(hook, "can't be null");
        if (!(Activities.hook instanceof DefaultOnEventsTransformer)) { throw new IllegalAccessError("hook can't be changed"); }
        Activities.hook = hook;
    }

    public static void init(Application application) {
        ObjectHelper.requireNonNull(application, "can't be null");
        if (Activities.context != null) { throw new IllegalAccessError("already initialized"); }
        Activities.context = application;

        application.registerComponentCallbacks(new ComponentCallbacks() {

            @Override
            @SuppressWarnings("unchecked")
            public void onConfigurationChanged(Configuration newConfig) {
                for (State<? extends Activity> state : STATES) {
                    State<Activity> newState = new State<>(state.id, On.CHANGE, state.ui, null);
                    EVENTS.onNext(newState);

                    LinkedHashSet<ObservableEmitter> emitters = EMITTERS.get(state.id);
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


    @SuppressWarnings("unchecked")
    private static void setState(
            @NonNull String id,
            @NonNull On on,
            @Nullable Activity activity,
            @Nullable Bundle bundle) {

        State<? extends Activity> newState = new State<>(id, on, activity, bundle);
        EVENTS.onNext(newState);

        Iterator<State<? extends Activity>> iterator = STATES.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().id.equals(id)) {
                iterator.remove();
                break;
            }
        }
        if (newState.on != On.DESTROY) {
            STATES.add(newState);
        }

        for (Map.Entry<String, LinkedHashSet<ObservableEmitter>> subscription : EMITTERS.entrySet()) {
            if (subscription.getKey().equals(id)) {
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
                .subscribeOn(ImmediateLooperScheduler.MAIN)
                .unsubscribeOn(ImmediateLooperScheduler.MAIN)
                .observeOn(ImmediateLooperScheduler.MAIN)
                .compose(hook)
                .map(behavior)
                .doOnNext(behavior);
    }


    public static Observable<State<? extends Activity>> events() {
        return EVENTS
                .subscribeOn(ImmediateLooperScheduler.MAIN);
    }

    public static Observable<State<? extends Activity>> current() {
        return Observable
                .fromIterable(new LinkedHashSet<>(STATES))
                .subscribeOn(ImmediateLooperScheduler.MAIN);
    }

    public static <A extends Activity> Observable<State<A>> observe(final Class<A> activityClass) {
        return subscribe(new ActivityObservableBehavior<>(STATES, EMITTERS, activityClass));
    }

    public static <A extends Activity> Observable<State<A>> observe(final A activity) {
        return subscribe(new ActivityObservableBehavior<A>(STATES, EMITTERS, activity.getComponentName().getClassName()));
    }

    public static <A extends Activity> Observable<State<A>> bind(final Class<A> activityClass) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<>(STATES, EMITTERS, activityClass));
    }

    public static <A extends Activity> Observable<State<A>> bind(final A activity) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATES, EMITTERS, activity.getComponentName().getClassName()));
    }


    public static <A extends Activity> Observable<State<A>> start(final Class<A> activityClass) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATES, EMITTERS, activityClass) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(context, activityClass));
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Class<A> activityClass, final Bundle extras) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATES, EMITTERS, activityClass) {

            @Override
            protected void onAbsent() {
                context.startActivity(new Intent(context, activityClass).putExtras(extras));
            }
        });
    }

    public static <A extends Activity> Observable<State<A>> start(final Intent intent) {
        return subscribeUntilDestroy(new ActivityObservableBehavior<A>(STATES, EMITTERS, intent.getComponent().getClassName()) {

            @Override
            protected void onAbsent() {
                context.startActivity(intent);
            }
        });
    }

}