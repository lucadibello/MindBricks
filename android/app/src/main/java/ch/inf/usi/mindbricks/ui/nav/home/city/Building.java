package ch.inf.usi.mindbricks.ui.nav.home.city;

public class Building {

    public final int id;
    public final String name;
    public final int drawableRes;
    public final boolean purchased;

    public Building(int id, String name, int drawableRes, boolean purchased) {
        this.id = id;
        this.name = name;
        this.drawableRes = drawableRes;
        this.purchased = purchased;
    }
}
