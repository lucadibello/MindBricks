package ch.inf.usi.mindbricks.ui.nav.analytics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.DailyRings;
import ch.inf.usi.mindbricks.ui.charts.GoalRingsView;

/**
 * Adapter for horizontal scrolling list of previous days (excludes today)
 */
public class DailyRingsAdapter extends ListAdapter<DailyRings, DailyRingsAdapter.CompactViewHolder> {

    private OnDayClickListener clickListener;

    public DailyRingsAdapter() {
        super(new DiffUtil.ItemCallback<DailyRings>() {
            @Override
            public boolean areItemsTheSame(@NonNull DailyRings oldItem, @NonNull DailyRings newItem) {
                return oldItem.getDate().equals(newItem.getDate());
            }

            @Override
            public boolean areContentsTheSame(@NonNull DailyRings oldItem, @NonNull DailyRings newItem) {
                return oldItem.getDisplayDate().equals(newItem.getDisplayDate()) &&
                        oldItem.isEmpty() == newItem.isEmpty() &&
                        oldItem.getSessionCount() == newItem.getSessionCount();
            }
        });
    }

    @NonNull
    @Override
    public CompactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_ring_card_horizontal, parent, false);
        return new CompactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompactViewHolder holder, int position) {
        DailyRings data = getItem(position);
        holder.bind(data);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDayClick(position, data);
            }
        });
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.clickListener = listener;
    }

    // ViewHolder for horizontal compact cards
    static class CompactViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;
        TextView summaryText;
        GoalRingsView goalRingsView;

        public CompactViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            summaryText = itemView.findViewById(R.id.summaryText);
            goalRingsView = itemView.findViewById(R.id.goalRingsView);
        }

        public void bind(DailyRings data) {
            String shortDate = formatShortDate(data.getDisplayDate());
            dateText.setText(shortDate);

            String shortSummary = formatShortSummary(data);
            summaryText.setText(shortSummary);

            goalRingsView.setData(data.getRings(), true);

            if (data.isEmpty()) {
                summaryText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.empty_state_text));
                itemView.setAlpha(0.6f);
            } else {
                summaryText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.analytics_text_secondary));
                itemView.setAlpha(1.0f);
            }
        }

        private String formatShortDate(String displayDate) {
            if (displayDate.contains(",")) {
                return displayDate.substring(0, displayDate.indexOf(","));
            }
            return displayDate;
        }

        private String formatShortSummary(DailyRings data) {
            if (data.isEmpty()) {
                return "No data";
            }
            // Just show minutes, no session count
            int minutes = (int) (data.getRings().isEmpty() ? 0 : data.getRings().get(0).getCurrent());
            return minutes + " min";
        }
    }

    public interface OnDayClickListener {
        void onDayClick(int position, DailyRings data);
    }
}