package ch.inf.usi.mindbricks.ui.nav.shop;

public class ShopItem {

    // ADDED: A unique, non-changing ID for the item.
    // This is used to save the purchase in SharedPreferences.
    private final String id;

    private final String name;
    private final int price;
    private final int imageResourceId; // Using a drawable resource for the image

    // UPDATED: The constructor now accepts the 'id' as the first parameter.
    public ShopItem(String id, String name, int price, int imageResourceId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageResourceId = imageResourceId;
    }

    // ADDED: A getter method to access the item's unique ID.
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }
}
