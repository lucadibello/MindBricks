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
        void onEmotionSelected(int emotionIndex, boolean wantsDetailedQuestions);
    }

    private final int[] emotionImages = {
            R.drawable.emotion_1,
            R.drawable.emotion_2,
            R.drawable.emotion_3,
            R.drawable.emotion_4,
            R.drawable.emotion_5,
            R.drawable.emotion_6,
            R.drawable.emotion_7
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

        for (int i = 0; i < emotionImages.length; i++) {
            final int emotionIndex = i;
            ImageView emotionView = new ImageView(getContext());
            emotionView.setImageResource(emotionImages[i]);

            emotionView.setClickable(true);
            emotionView.setFocusable(true);

            emotionView.setOnClickListener(v -> onEmotionClicked(emotionIndex));

            emotionGrid.addView(emotionView);
        }
    }


    private void onEmotionClicked(int emotionIndex) {
        // Directly proceed to detailed questions
        if (listener != null) {
            listener.onEmotionSelected(emotionIndex, true);
        }
        dismiss();
    }

    private ImageView createEmotionImageView(int emotionIndex) {
        ImageView emotionView = new ImageView(getContext());
        emotionView.setImageResource(emotionImages[emotionIndex]);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = (int) (80 * getResources().getDisplayMetrics().density); // 80dp height
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Equal column weight
        params.setMargins(8, 8, 8, 8);
        emotionView.setLayoutParams(params);

        emotionView.setClickable(true);
        emotionView.setFocusable(true);
        emotionView.setBackgroundResource(android.R.drawable.list_selector_background);

        emotionView.setContentDescription(emotionLabels[emotionIndex]);

        emotionView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        emotionView.setPadding(12, 12, 12, 12);

        return emotionView;
    }

    // getters and setters
    public void setListener(OnEmotionSelectedListener listener) {
        this.listener = listener;
    }

    public OnEmotionSelectedListener getListener() {
        return listener;
    }

    public int[] getEmotionImages() {
        return emotionImages;
    }

    public String[] getEmotionLabels() {
        return emotionLabels;
    }

    public String getEmotionLabel(int index) {
        if (index >= 0 && index < emotionLabels.length) {
            return emotionLabels[index];
        }
        return "Unknown";
    }
}
