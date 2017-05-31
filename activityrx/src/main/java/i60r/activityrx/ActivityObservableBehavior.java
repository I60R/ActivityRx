package i60r.activityrx;

import android.app.Activity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;


/***
 * Created by 160R on 23.04.17.
 */
class ActivityObservableBehavior<A extends Activity> implements
        ObservableOnSubscribe<State<A>>,
        Consumer<State<A>>,
        Function<State, State<A>>,
        Predicate<State<A>>,
        Action {

    private final LinkedHashMap<String, LinkedHashSet<ObservableEmitter>> emitters;
    private final LinkedHashSet<State<? extends Activity>> states;
    private final String component;

    private ObservableEmitter emitter = null;
    private boolean first = true;


    ActivityObservableBehavior(
            final LinkedHashSet<State<? extends Activity>> states,
            final LinkedHashMap<String, LinkedHashSet<ObservableEmitter>> emitters,
            final String component) {
        this.emitters = emitters;
        this.states = states;
        this.component = component;
    }

    ActivityObservableBehavior(
            final LinkedHashSet<State<? extends Activity>> states,
            final LinkedHashMap<String, LinkedHashSet<ObservableEmitter>> emitters,
            final Class<A> activityClass) {
        this(states, emitters, activityClass.getName());
    }


    /**
     * Called when some subscriber subscribes to A events.
     */
    @Override
    public void subscribe(@NonNull ObservableEmitter<State<A>> emitter) throws Exception {
        State<? extends Activity> current = null;
        for (State<? extends Activity> state : states) {
            if (state.id.equals(component)) {
                current = state;
                break;
            }
        }
        if (current == null) {
            current = new State<>(component, On.ABSENT, null, null);
        }

        LinkedHashSet<ObservableEmitter> queue = emitters.get(component);
        if (queue == null) {
            queue = new LinkedHashSet<>(5);
        }

        this.emitter = emitter;

        queue.add(emitter);
        emitters.put(component, queue);
        emitter.onNext(apply(current));
    }


    /**
     * Called to cast State of any Activity to concrete Activity
     */
    @Override
    @SuppressWarnings("unchecked")
    public State<A> apply(@NonNull State state) throws Exception {
        return (State<A>) state;
    }


    /**
     * Invokes onAbsent() when Activity subscribed to don't exists
     */
    @Override
    public void accept(@NonNull State<A> state) throws Exception {
        if (first) {
            first = false;
            if (state.on == On.ABSENT) {
                onAbsent();
            }
        }
    }


    /**
     * Used to dispose subscription after Activity subscribed to destroys
     */
    @Override
    public boolean test(@NonNull State<A> state) throws Exception {
        return state.on == On.DESTROY;
    }


    /**
     * Used to clean up on subscription disposed
     */
    @Override
    public void run() throws Exception {
        LinkedHashSet<ObservableEmitter> queue = emitters.get(component);
        if (queue != null && emitter != null) {
            queue.remove(emitter);
        }
    }


    /**
     * Method invoked when subscribed to non existent Activity
     */
    protected void onAbsent() {
        /* Do nothing by default*/
    }

}
