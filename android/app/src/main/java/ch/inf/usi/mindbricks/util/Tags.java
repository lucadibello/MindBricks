package ch.inf.usi.mindbricks.util;

import android.content.Context;

import ch.inf.usi.mindbricks.R;

public final class Tags {
    public static int[] getTagColorPalette(Context ctx) {
        return new int[]{
                ctx.getColor(R.color.tag_color_red),
                ctx.getColor(R.color.tag_color_orange),
                ctx.getColor(R.color.tag_color_yellow),
                ctx.getColor(R.color.tag_color_green),
                ctx.getColor(R.color.tag_color_blue),
                ctx.getColor(R.color.tag_color_purple),
                ctx.getColor(R.color.tag_color_pink),
                ctx.getColor(R.color.tag_color_indigo),
                ctx.getColor(R.color.tag_color_cyan),
                ctx.getColor(R.color.tag_color_teal),
                ctx.getColor(R.color.tag_color_lime),
                ctx.getColor(R.color.tag_color_brown)
        };
    }
}
