package i60r.activityrx;

import android.app.Activity;
import android.os.Bundle;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;


/***
 * Created by 160R on 21.04.17.
 */
public final class State<A extends Activity> {

    @NonNull
    public final String id;
    @NonNull
    public final On on;
    @Nullable
    public final A ui;
    @Nullable
    public final Bundle arg;


    public State(
            @NonNull final String id,
            @NonNull final On on,
            @Nullable final A ui,
            @Nullable final Bundle arg) {
        this.id = id;
        this.on = on;
        this.ui = ui;
        this.arg = arg;
    }
}
