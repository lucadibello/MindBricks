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
import ch.inf.usi.mindbricks.util.questionnaire.ProductivityQuestionnaireConfig;
import ch.inf.usi.mindbricks.util.questionnaire.ProductivityQuestionnaireResult;

public class ProductivityQuestionsDialogFragment extends DialogFragment {

    // FIXME: we should move these to a separate resources file!
    private static final String QUESTION_ENTHUSIASM = "I felt enthusiastic about the task I just completed.";
    private static final String QUESTION_ENERGY = "I experienced a sense of energy and alertness while working.";
    private static final String QUESTION_ENGAGEMENT = "I was absorbed and fully engaged in what I was doing.";
    private static final String QUESTION_SATISFACTION = "I felt satisfied with my performance in the last session.";
    private static final String QUESTION_ANTICIPATION = "I looked forward to the next Pomodoro interval.";
    // Rating scale labels
    private static final String LABEL_MIN = "Strongly Disagree";
    private static final String LABEL_MAX = "Strongly Agree";

    /**
     * The enthusiasm rating of the questionnaire in range {@link ProductivityQuestionnaireConfig#MIN_RATING} to
     * {@link ProductivityQuestionnaireConfig#MAX_RATING}.
     * By default set to {@link ProductivityQuestionnaireConfig#NEUTRAL_RATING}.
     */
    private int enthusiasmRating = ProductivityQuestionnaireConfig.NEUTRAL_RATING;
    /**
     * The energy rating of the questionnaire in range {@link ProductivityQuestionnaireConfig#MIN_RATING} to
     * {@link ProductivityQuestionnaireConfig#MAX_RATING}.
     * By default set to {@link ProductivityQuestionnaireConfig#NEUTRAL_RATING}.
     */
    private int energyRating = ProductivityQuestionnaireConfig.NEUTRAL_RATING;
    /**
     * The engagement rating of the questionnaire in range {@link ProductivityQuestionnaireConfig#MIN_RATING} to
     * {@link ProductivityQuestionnaireConfig#MAX_RATING}.
     * By default set to {@link ProductivityQuestionnaireConfig#NEUTRAL_RATING}.
     */
    private int engagementRating = ProductivityQuestionnaireConfig.NEUTRAL_RATING;
    /**
     * The satisfaction rating of the questionnaire in range {@link ProductivityQuestionnaireConfig#MIN_RATING} to
     * {@link ProductivityQuestionnaireConfig#MAX_RATING}.
     * By default set to {@link ProductivityQuestionnaireConfig#NEUTRAL_RATING}.
     */
    private int satisfactionRating = ProductivityQuestionnaireConfig.NEUTRAL_RATING;
    /**
     * The anticipation rating of the questionnaire in range {@link ProductivityQuestionnaireConfig#MIN_RATING} to
     * {@link ProductivityQuestionnaireConfig#MAX_RATING}.
     * By default set to {@link ProductivityQuestionnaireConfig#NEUTRAL_RATING}.
     */
    private int anticipationRating = ProductivityQuestionnaireConfig.NEUTRAL_RATING;
    private OnQuestionnaireActionListener listener;

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment ProductivityQuestionsDialogFragment.
     */
    public static ProductivityQuestionsDialogFragment create() {
        ProductivityQuestionsDialogFragment fragment = new ProductivityQuestionsDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set full-screen dialog style
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_detailed_questionnare, container, false);

        // setup each question
        LinearLayout questionsContainer = view.findViewById(R.id.questions_container);
        setupQuestion(questionsContainer, QUESTION_ENTHUSIASM, rating -> enthusiasmRating = rating);
        setupQuestion(questionsContainer, QUESTION_ENERGY, rating -> energyRating = rating);
        setupQuestion(questionsContainer, QUESTION_ENGAGEMENT, rating -> engagementRating = rating);
        setupQuestion(questionsContainer, QUESTION_SATISFACTION, rating -> satisfactionRating = rating);
        setupQuestion(questionsContainer, QUESTION_ANTICIPATION, rating -> anticipationRating = rating);

        // Set up submit button
        Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v -> onSubmitClicked());

        // Set up skip button
        Button skipButton = view.findViewById(R.id.skip_button);
        skipButton.setOnClickListener(v -> onSkipClicked());

        // Make dialog non-cancelable (prevent clicking outside)
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

    /**
     * Setup a single question.
     *
     * @param container    The container to add the question view to.
     * @param questionText The text of the question.
     * @param callback     The callback for rating changes.
     */
    private void setupQuestion(LinearLayout container, String questionText, RatingCallback callback) {
        View questionView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_questionnaire_question, container, false);

        // write question
        TextView questionTextView = questionView.findViewById(R.id.question_text);
        questionTextView.setText(questionText);

        // create + setup rating bar
        RatingBar ratingBar = questionView.findViewById(R.id.rating_bar);
        ratingBar.setNumStars(ProductivityQuestionnaireConfig.MAX_RATING);
        ratingBar.setStepSize(1f);

        // by default -> neutral
        ratingBar.setRating(ProductivityQuestionnaireConfig.NEUTRAL_RATING);

        // add labels
        TextView minLabel = questionView.findViewById(R.id.rating_min_label);
        TextView maxLabel = questionView.findViewById(R.id.rating_max_label);
        minLabel.setText(LABEL_MIN);
        maxLabel.setText(LABEL_MAX);

        // Set rating -> change listener
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser) {
                callback.onRatingChanged((int) rating);
            }
        });

        // add question in questionnaire container
        container.addView(questionView);
    }

    /**
     * Handle the submit button click.
     */
    private void onSubmitClicked() {
        if (listener != null) {
            listener.onQuestionnaireComplete(
                    new ProductivityQuestionnaireResult(
                            enthusiasmRating,
                            energyRating,
                            engagementRating,
                            satisfactionRating,
                            anticipationRating
                    )
            );
        }
        dismiss();
    }

    /**
     * Handle the skip button click.
     */
    private void onSkipClicked() {
        if (listener != null) {
            listener.onQuestionnaireSkipped();
        }
        dismiss();
    }

    /**
     * Get the listener for questionnaire actions.
     *
     * @return The listener.
     */
    public OnQuestionnaireActionListener getListener() {
        return listener;
    }

    /**
     * Set the listener for questionnaire actions.
     *
     * @param listener The listener to set.
     */
    public void setListener(OnQuestionnaireActionListener listener) {
        this.listener = listener;
    }


    public interface OnQuestionnaireActionListener {
        void onQuestionnaireComplete(ProductivityQuestionnaireResult result);

        void onQuestionnaireSkipped();
    }

    private interface RatingCallback {
        void onRatingChanged(int rating);
    }
}
