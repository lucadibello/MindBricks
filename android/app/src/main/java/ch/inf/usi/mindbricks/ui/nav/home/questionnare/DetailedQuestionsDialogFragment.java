package ch.inf.usi.mindbricks.ui.nav.home.questionnare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import ch.inf.usi.mindbricks.R;

public class DetailedQuestionsDialogFragment extends DialogFragment {

    private int emotionIndex;
    private OnQuestionnaireCompleteListener listener;

    // Init at 4 -> neutral
    private int enthusiasmRating = 4;
    private int energyRating = 4;
    private int engagementRating = 4;
    private int satisfactionRating = 4;
    private int anticipationRating = 4;

    // Question texts
    private static final String QUESTION_ENTHUSIASM = "I felt enthusiastic about the task I just completed.";
    private static final String QUESTION_ENERGY = "I experienced a sense of energy and alertness while working.";
    private static final String QUESTION_ENGAGEMENT = "I was absorbed and fully engaged in what I was doing.";
    private static final String QUESTION_SATISFACTION = "I felt satisfied with my performance in the last session.";
    private static final String QUESTION_ANTICIPATION = "I looked forward to the next Pomodoro interval.";

    // Rating scale labels
    private static final String LABEL_MIN = "Strongly Disagree";
    private static final String LABEL_MAX = "Strongly Agree";

    public interface OnQuestionnaireCompleteListener {
        void onQuestionnaireComplete(
                int emotionIndex,
                int enthusiasm,
                int energy,
                int engagement,
                int satisfaction,
                int anticipation
        );

        void onQuestionnaireSkipped(int emotionIndex);
    }

    private interface RatingCallback {
        void onRatingChanged(int rating);
    }

    public static DetailedQuestionsDialogFragment newInstance(int emotionIndex) {
        DetailedQuestionsDialogFragment fragment = new DetailedQuestionsDialogFragment();
        Bundle args = new Bundle();
        args.putInt("emotionIndex", emotionIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set full-screen dialog style
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);

        if (getArguments() != null) {
            emotionIndex = getArguments().getInt("emotionIndex", 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_detailed_questionnare, container, false);

        LinearLayout questionsContainer = view.findViewById(R.id.questions_container);

        setupQuestion(questionsContainer, QUESTION_ENTHUSIASM,
                rating -> enthusiasmRating = rating);

        setupQuestion(questionsContainer, QUESTION_ENERGY,
                rating -> energyRating = rating);

        setupQuestion(questionsContainer, QUESTION_ENGAGEMENT,
                rating -> engagementRating = rating);

        setupQuestion(questionsContainer, QUESTION_SATISFACTION,
                rating -> satisfactionRating = rating);

        setupQuestion(questionsContainer, QUESTION_ANTICIPATION,
                rating -> anticipationRating = rating);

        // Set up submit button
        Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v -> onSubmitClicked());

        // Set up skip button
        Button skipButton = view.findViewById(R.id.skip_button);
        skipButton.setOnClickListener(v -> onSkipClicked());

        // Make dialog non-cancelable
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(false);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make dialog fill screen
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void setupQuestion(LinearLayout container, String questionText, RatingCallback callback) {
        View questionView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_questionnaire_question, container, false);

        TextView questionTextView = questionView.findViewById(R.id.question_text);
        questionTextView.setText(questionText);

        RatingBar ratingBar = questionView.findViewById(R.id.rating_bar);
        ratingBar.setNumStars(7);
        ratingBar.setStepSize(1f);
        // Default to middle value
        ratingBar.setRating(4f);

        TextView minLabel = questionView.findViewById(R.id.rating_min_label);
        TextView maxLabel = questionView.findViewById(R.id.rating_max_label);
        minLabel.setText(LABEL_MIN);
        maxLabel.setText(LABEL_MAX);

        // Set rating -> change listener
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser && rating > 0) {
                callback.onRatingChanged((int) rating);
            }
        });

        container.addView(questionView);
    }

    private void onSubmitClicked() {
        if (listener != null) {
            listener.onQuestionnaireComplete(
                    emotionIndex,
                    enthusiasmRating,
                    energyRating,
                    engagementRating,
                    satisfactionRating,
                    anticipationRating
            );
        }
        dismiss();
    }

    private void onSkipClicked() {
        if (listener != null) {
            listener.onQuestionnaireSkipped(emotionIndex);
        }
        dismiss();
    }


    // getters and setters
    public void setListener(OnQuestionnaireCompleteListener listener) {
        this.listener = listener;
    }

    public OnQuestionnaireCompleteListener getListener() {
        return listener;
    }
}
