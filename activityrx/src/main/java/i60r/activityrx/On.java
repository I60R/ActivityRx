package i60r.activityrx;


/***
 * Created by 160R on 21.04.17.
 */
public enum On {
    ABSENT  (false, false),

    CREATE  (false, true),

    RESUME  (false, true),
    START   (true,  true),
    PAUSE   (false, true),

    SAVE    (false, true),

    STOP    (false, true),
    DESTROY (false, false);

    public final boolean visible;
    public final boolean usable;

    On(final boolean visible,
       final boolean usable) {
        this.visible = visible;
        this.usable = usable;
    }
}
