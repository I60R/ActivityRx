package i60r.activityrx;

import android.app.Activity;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;

/***
 * Created by 160R on 08.05.17.
 */
class DefaultOnEventsTransformer implements ObservableTransformer<State<? extends Activity>, State<? extends Activity>> {

    @Override
    public ObservableSource<State<? extends Activity>> apply(@NonNull Observable<State<? extends Activity>> upstream) {
        return upstream;
    }
}
