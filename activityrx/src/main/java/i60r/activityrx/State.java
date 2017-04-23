package i60r.activityrx;

import android.app.Activity;
import android.os.Bundle;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;


/***
 * Created by 160R on 21.04.17.
 */
public final class State<A extends Activity> {
    public final Class<A> requested;
    public final On on;
    public final A ui;

    private Bundle extras;


    public State(
            @NonNull Class<? extends Activity> requested,
            @NonNull final On on,
            @Nullable final A ui,
            @Nullable final Bundle extras) {
        this.requested = (Class<A>) requested;
        this.ui = ui;
        this.extras = extras;
        this.on = on;
    }

    public final Bundle extras() {
        if (extras == null) {
            if (ui == null) {
                extras = new Bundle();
            } else {
                extras = ui.getIntent().getExtras();
            }
        }
        return extras;
    }


}
