package ch.inf.usi.mindbricks.ui.charts;

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
import ch.inf.usi.mindbricks.model.StudySession;

/**
 * Adapter for displaying study session history in a RecyclerView
 */
public class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder> {

    private List<StudySession> sessions;
    private final OnSessionClickListener clickListener;

    public SessionHistoryAdapter(OnSessionClickListener listener) {
        this.sessions = new ArrayList<>();
        this.clickListener = listener;
    }

    public void setData(List<StudySession> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_history, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        StudySession session = sessions.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public interface OnSessionClickListener {
        void onSessionClick(StudySession session);
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView dateText;
        private final TextView durationText;
        private final TextView focusScoreText;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.sessionCard);
            dateText = itemView.findViewById(R.id.sessionDate);
            durationText = itemView.findViewById(R.id.sessionDuration);
            focusScoreText = itemView.findViewById(R.id.sessionFocusScore);
        }

        void bind(StudySession session) {
            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            dateText.setText(sdf.format(new Date(session.getTimestamp())));

            // Format duration
            int hours = session.getDurationMinutes() / 60;
            int minutes = session.getDurationMinutes() % 60;
            if (hours > 0) {
                durationText.setText(String.format(Locale.getDefault(), "%dh %dm", hours, minutes));
            } else {
                durationText.setText(String.format(Locale.getDefault(), "%dm", minutes));
            }

            // Format focus score
            focusScoreText.setText(String.format(Locale.getDefault(), "%.0f%% focus", session.getFocusScore()));

            // Click listener
            cardView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSessionClick(session);
                }
            });
        }
    }
}