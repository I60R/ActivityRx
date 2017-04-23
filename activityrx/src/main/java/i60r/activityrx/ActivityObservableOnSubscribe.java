package i60r.activityrx;

import android.app.Activity;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;


/***
 * Created by 160R on 23.04.17.
 */
class ActivityObservableOnSubscribe<A extends Activity> implements
        ObservableOnSubscribe<State<A>>,
        Consumer<State<A>>,
        Function<State, State<A>>,
        Predicate<State<A>>,
        Cancellable {

    private final Map<Class<? extends Activity>, State<? extends Activity>> states;
    private final Map<Class<? extends Activity>, Set<ObservableEmitter>> emitters;
    private final Class<A> key;

    private ObservableEmitter emitter = null;
    private boolean first = true;


    ActivityObservableOnSubscribe(
            final Map<Class<? extends Activity>, State<? extends Activity>> states,
            final Map<Class<? extends Activity>, Set<ObservableEmitter>> emitters,
            final Class<A> key) {
        this.emitters = emitters;
        this.states = states;
        this.key = key;
    }




    @Override
    public void subscribe(@NonNull ObservableEmitter<State<A>> emitter) throws Exception {
        State<? extends Activity> current = states.get(key);
        Set<ObservableEmitter> queue = emitters.get(key);

        if (current == null) {
            current = new State<>(key, On.ABSENT, null, null);
        }
        if (queue == null) {
            queue = new LinkedHashSet<>(5);
        }

        this.emitter = emitter;

        queue.add(emitter);
        emitters.put(key, queue);
        emitter.setCancellable(this);
        emitter.onNext(apply(current));
    }

    @Override
    public State<A> apply(@NonNull State state) throws Exception {
        return (State<A>) state;
    }

    @Override
    public void accept(@NonNull State<A> state) throws Exception {
        if (first) {
            first = false;
            if (state.on == On.ABSENT) {
                onAbsent();
            }
        }
    }

    @Override
    public boolean test(@NonNull State<A> state) throws Exception {
        return state.on == On.DESTROY;
    }

    @Override
    public void cancel() throws Exception {
        Set<ObservableEmitter> queue = emitters.get(key);
        if (queue != null && emitter != null) {
            queue.remove(emitter);
        }
    }

    protected void onAbsent() {
        /* Do nothing */
    }

}
