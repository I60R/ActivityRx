package i60r.activityrx;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.internal.functions.ObjectHelper;

/***
 * Created by 160R on 01.06.17.
 */

public final class ActivityRx {

    static ObservableTransformer<State<? extends Activity>, State<? extends Activity>> hook = new DefaultOnEventsTransformer();
    static Context context = null;

    public static void modify(@NonNull final ObservableTransformer<State<? extends Activity>, State<? extends Activity>> hook) {
        ObjectHelper.requireNonNull(hook, "can't be null");
        if (!(ActivityRx.hook instanceof DefaultOnEventsTransformer)) { throw new IllegalAccessError("hook can't be changed"); }
        ActivityRx.hook = hook;
    }

    public static void init(@NonNull final Context anyContext) {
        ObjectHelper.requireNonNull(anyContext, "can't be null");
        if (ActivityRx.context != null) { throw new IllegalAccessError("already initialized"); }
        Application application = (Application) anyContext.getApplicationContext();
        ObjectHelper.requireNonNull(application, "failed to get Application context");
        ActivityRx.context = application;
        Activities.init(application);
    }
}
