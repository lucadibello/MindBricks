package ch.inf.usi.mindbricks.model;

import org.json.JSONException;
import org.json.JSONObject;

public record Tag(String title, int color) {

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("color", color);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static Tag fromJson(JSONObject obj) {
        try {
            String title = obj.getString("title");
            int color = obj.getInt("color");
            return new Tag(title, color);
        } catch (JSONException e) {
            return null;
        }
    }
}
