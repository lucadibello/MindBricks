// In a suitable package, e.g., ch.inf.usi.mindbricks.model
package ch.inf.usi.mindbricks.ui.nav.profile;

public class PurchasedItem {
    private final String id;
    private final String name;
    private final int imageResId; // The drawable resource ID for the item's image

    public PurchasedItem(String id, String name, int imageResId) {
        this.id = id;
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}
