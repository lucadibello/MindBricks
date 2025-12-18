package ch.inf.usi.mindbricks.ui.nav.home.adapters;

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

/**
 * Spinner adapter for tags.
 *
 * @author Luca Di Bello
 */
public class TagSpinnerAdapter extends ArrayAdapter<Tag> {

    /**
     * Constructor for the adapter.
     *
     * @param context context of the activity
     * @param tags list of tags to display
     */
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

    /**
     * Creates a view for a given tag.
     *
     * @param position index of the tag
     * @param convertView recycled view
     * @param parent parent view group
     *
     * @return view for the given tag
     */
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
