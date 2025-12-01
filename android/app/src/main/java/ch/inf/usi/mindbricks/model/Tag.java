package ch.inf.usi.mindbricks.model;

import org.json.JSONException;
import org.json.JSONObject;

public record Tag(String title, int color) {

    /**
     * Decode a JSON object into a Tag object.
     *
     * @param obj JSON object to decode
     * @return Tag object represented by the JSON object
     */
    public static Tag fromJson(JSONObject obj) {
        try {
            String title = obj.getString("title");
            int color = obj.getInt("color");
            return new Tag(title, color);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Encode this tag as a JSON object.
     *
     * @return JSON object representing this tag
     */
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("color", color);
        } catch (JSONException ignored) {
        }
        return obj;
    }
}
