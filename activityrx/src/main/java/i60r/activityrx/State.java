package i60r.activityrx;

import android.app.Activity;
import android.os.Bundle;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;


/***
 * Created by 160R on 21.04.17.
 */
public final class State<A extends Activity> {
    public final String component;
    public final Bundle args;
    public final On on;
    public final A ui;


    public State(
            @NonNull final String component,
            @NonNull final On on,
            @Nullable final A ui,
            @Nullable final Bundle args) {
        this.component = component;
        this.args = args;
        this.ui = ui;
        this.on = on;
    }
}
