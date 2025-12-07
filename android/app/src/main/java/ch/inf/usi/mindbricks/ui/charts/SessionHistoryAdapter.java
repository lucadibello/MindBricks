package ch.inf.usi.mindbricks.ui.charts;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;

/**
 * Adapter for displaying study session history in a RecyclerView.
 */
public class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder> {

    private List<StudySessionWithStats> sessions;
    private final OnSessionClickListener clickListener;

    // Date formatter for displaying timestamps
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public interface OnSessionClickListener {
        void onSessionClick(StudySessionWithStats session);
        void onSessionLongClick(StudySessionWithStats session);
    }

    public SessionHistoryAdapter(OnSessionClickListener listener) {
        this.sessions = new ArrayList<>();
        this.clickListener = listener;
    }

    public void setData(List<StudySessionWithStats> sessions) {
        this.sessions = sessions != null ? new ArrayList<>(sessions) : new ArrayList<>();
        // Refresh entire list
        notifyDataSetChanged();
    }

    public void addSession(StudySessionWithStats session) {
        // Add at beginning -> sorted list
        sessions.add(0, session);
        notifyItemInserted(0);
    }

    public void removeSession(int position) {
        if (position >= 0 && position < sessions.size()) {
            sessions.remove(position);
            notifyItemRemoved(position);
        }
    }

    public StudySessionWithStats getSessionAt(int position) {
        return sessions.get(position);
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_history, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        StudySessionWithStats session = sessions.get(position);
        holder.bind(session, clickListener);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }


    class SessionViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final View colorIndicator;
        private final TextView dateText;
        private final TextView timeText;
        private final TextView tagText;
        private final TextView durationText;
        private final TextView focusScoreText;
        private final TextView statsText;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find all views once during ViewHolder creation
            cardView = itemView.findViewById(R.id.sessionCard);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            dateText = itemView.findViewById(R.id.sessionDate);
            timeText = itemView.findViewById(R.id.sessionTime);
            tagText = itemView.findViewById(R.id.sessionTag);
            durationText = itemView.findViewById(R.id.sessionDuration);
            focusScoreText = itemView.findViewById(R.id.sessionFocusScore);
            statsText = itemView.findViewById(R.id.sessionStats);
        }

        void bind(StudySessionWithStats session, OnSessionClickListener listener) {
            // Format and set date/time
            Date sessionDate = new Date(session.getTimestamp());
            dateText.setText(dateFormat.format(sessionDate));
            timeText.setText(timeFormat.format(sessionDate));

            // Set tag title and color
            tagText.setText(session.getTagTitle());
            colorIndicator.setBackgroundColor(session.getTagColor());

            // Format and set duration
            int totalMinutes = session.getDurationMinutes();
            String durationStr = formatDuration(totalMinutes);
            durationText.setText(durationStr);

            // Set focus score with color coding
            float focusScore = session.getFocusScore();
            focusScoreText.setText(String.format(Locale.getDefault(), "%.0f%%", focusScore));
            focusScoreText.setTextColor(getFocusScoreColor(focusScore));

            // Set additional stats (noise, light, pickups)
            String stats = formatStats(session);
            statsText.setText(stats);

            // Set click listeners
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionClick(session);
                }
            });

            cardView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onSessionLongClick(session);
                    return true;
                }
                return false;
            });

            // Add ripple effect feedback
            cardView.setClickable(true);
            cardView.setFocusable(true);
        }

        private String formatDuration(int totalMinutes) {
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;

            if (hours > 0) {
                return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
            } else {
                return String.format(Locale.getDefault(), "%dm", minutes);
            }
        }


        private String formatStats(StudySessionWithStats session) {
            List<String> stats = new ArrayList<>();

            // Add noise level if available
            if (session.getAvgNoiseLevel() > 0) {
                stats.add(String.format(Locale.getDefault(),
                        "Noise RMS: %.0f", session.getAvgNoiseLevel()));
            }

            // Add light level if available
            if (session.getAvgLightLevel() > 0) {
                stats.add(String.format(Locale.getDefault(),
                        "Light: %.0f%%", session.getAvgLightLevel()));
            }

            // Add phone pickups
            if (session.getPhonePickupCount() > 0) {
                stats.add(String.format(Locale.getDefault(),
                        "%d pickups", session.getPhonePickupCount()));
            }

            // Join with separator
            if (stats.isEmpty()) {
                return "No additional stats";
            }

            return String.join(" • ", stats);
        }

        private int getFocusScoreColor(float score) {
            if (score >= 70) {
                return Color.rgb(76, 175, 80); // Green
            } else if (score >= 40) {
                return Color.rgb(255, 152, 0); // Orange
            } else {
                return Color.rgb(244, 67, 54); // Red
            }
        }
    }
}
