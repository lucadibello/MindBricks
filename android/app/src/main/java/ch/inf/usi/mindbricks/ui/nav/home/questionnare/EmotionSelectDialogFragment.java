package ch.inf.usi.mindbricks.ui.nav.home.questionnare;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import ch.inf.usi.mindbricks.R;

public class EmotionSelectDialogFragment extends DialogFragment {

    private OnEmotionSelectedListener listener;

    public interface OnEmotionSelectedListener {
        void onEmotionSelected(int emotionIndex);
    }

    private final int[] emotionImages = {
            R.drawable.sentiment_very_dissatisfied_24px,
            R.drawable.sentiment_dissatisfied_24px,
            R.drawable.mood_bad_24px,
            R.drawable.sentiment_neutral_24px,
            R.drawable.mood_24px,
            R.drawable.sentiment_satisfied_24px,
            R.drawable.sentiment_very_satisfied_24px,
    };

    private final String[] emotionLabels = {
            "Happy",
            "Excited",
            "Calm",
            "Neutral",
            "Tired",
            "Frustrated",
            "Stressed"
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_emotion_selection, null);

        setupEmotionGrid(view);

        return new AlertDialog.Builder(requireContext())
                .setTitle("How did you feel during this session?")
                .setView(view)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void setupEmotionGrid(View view) {
        GridLayout emotionGrid = view.findViewById(R.id.emotion_grid);
        emotionGrid.setColumnCount(4);

        for (int i = 0; i < emotionImages.length; i++) {
            final int emotionIndex = i;
            ImageView emotionView = createEmotionImageView(emotionIndex);
            emotionView.setOnClickListener(v -> onEmotionClicked(emotionIndex));
            emotionGrid.addView(emotionView);
        }
    }

    private void onEmotionClicked(int emotionIndex) {
        if (listener != null) {
            listener.onEmotionSelected(emotionIndex);
        }
        dismiss();
    }

    private ImageView createEmotionImageView(int emotionIndex) {
        ImageView emotionView = new ImageView(getContext());
        emotionView.setImageResource(emotionImages[emotionIndex]);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = (int) (80 * getResources().getDisplayMetrics().density);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        emotionView.setLayoutParams(params);

        emotionView.setClickable(true);
        emotionView.setFocusable(true);
        emotionView.setBackgroundResource(android.R.drawable.list_selector_background);
        emotionView.setContentDescription(emotionLabels[emotionIndex]);
        emotionView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        emotionView.setPadding(padding, padding, padding, padding);

        return emotionView;
    }

    public void setOnEmotionSelectedListener(OnEmotionSelectedListener listener) {
        this.listener = listener;
    }

    public OnEmotionSelectedListener getOnEmotionSelectedListener() {
        return listener;
    }
}
