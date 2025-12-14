package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.Tag;

class TagSpinnerAdapter extends ArrayAdapter<Tag> {

    public TagSpinnerAdapter(Context context, List<Tag> tags) {
        super(context, 0, tags);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        // inflate the layout for each tag
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_spinner_tag, parent, false);
        }

        // load data from the tag into the view
        Tag tag = getItem(position);
        if (tag != null) {
            TextView tagName = convertView.findViewById(R.id.tag_name_text);
            ImageView tagColor = convertView.findViewById(R.id.tag_color_circle);

            tagName.setText(tag.getTitle());
            tagColor.setColorFilter(tag.getColor(), PorterDuff.Mode.SRC_IN);
        }

        // return filled component
        return convertView;
    }
}
