package i60r.activityrx;


/***
 * Created by 160R on 21.04.17.
 */
public enum On {

    ABSENT    (false,           false),

    CREATE    (false,           true),

    START     (true,            true),
    RESUME    (true,            true),
    PAUSE     (true,            true),
    CHANGE    (true,            true),

    SAVE      (true,            true),

    STOP      (false,           false),

    DESTROY   (false,           false);

//              ^                 ^
//              |                 |
    On(boolean visible, boolean usable) {
        this.visible = visible;
        this.usable = usable;
    }

    public final boolean visible;
    public final boolean usable;

}
