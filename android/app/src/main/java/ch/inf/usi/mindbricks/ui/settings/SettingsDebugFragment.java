package ch.inf.usi.mindbricks.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.EmotionSelectDialogFragment;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.ProductivityQuestionsDialogFragment;
import ch.inf.usi.mindbricks.util.ProfileViewModel;
import ch.inf.usi.mindbricks.util.database.TestDataGenerator;
import ch.inf.usi.mindbricks.util.questionnaire.ProductivityQuestionnaireResult;

/**
 * Debug settings fragment for development and testing purposes.
 *
 * @author Marta Šafářová
 * <p>
 * Refactored by:
 * @author Luca Di Bello
 */
public class SettingsDebugFragment extends Fragment {

    private static final int COINS_AMOUNT = 1000;
    private ProfileViewModel profileViewModel;

    /**
     * Button for generating basic test data.
     */
    private MaterialButton btnGenerateBasic;

    /**
     * Button for generating large test data.
     */
    private MaterialButton btnGenerateLarge;

    /**
     * Button for testing the questionnaire dialog flow.
     */
    private MaterialButton btnTestQuestionnaire;

    /**
     * Button for clearing the database.
     */
    private MaterialButton btnClearDatabase;

    /**
     * Button for adding coins to the balance.
     */
    private MaterialButton btnAddCoins;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_debug, container, false);

        // Initialize ViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Initialize buttons
        btnGenerateBasic = view.findViewById(R.id.btn_generate_basic);
        btnGenerateLarge = view.findViewById(R.id.btn_generate_large);
        btnTestQuestionnaire = view.findViewById(R.id.btn_test_questionnaire);
        btnClearDatabase = view.findViewById(R.id.btn_clear_database);
        btnAddCoins = view.findViewById(R.id.btn_add_coins);

        setupClickListeners();

        return view;
    }

    /**
     * Sets up click listeners for all buttons.
     */
    private void setupClickListeners() {
        // Add coins to balance
        btnAddCoins.setOnClickListener(v -> {
            profileViewModel.addCoins(COINS_AMOUNT);
            showToast(getString(R.string.settings_tab_debug_balance_result_message, COINS_AMOUNT));
        });

        // Test questionnaire dialogs
        btnTestQuestionnaire.setOnClickListener(v -> showQuestionnairesTest());

        // Generate basic test data (50 sessions)
        btnGenerateBasic.setOnClickListener(v -> {
            showToast("Generating 50 test sessions...");
            TestDataGenerator.addTestSessions(requireContext(), 50);
            showCompletionToast("50 sessions generated!");
        });

        // Generate large dataset (500 sessions)
        btnGenerateLarge.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Generate Large Dataset")
                    .setMessage("This will generate 500 sessions with full data. This may take a few seconds.")
                    .setPositiveButton("Generate", (dialog, which) -> {
                        showToast("Generating 500 sessions... Please wait.");
                        TestDataGenerator.addTestSessions(requireContext(), 500);
                        showCompletionToast("500 sessions generated!");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Clear database with confirmation
        btnClearDatabase.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Clear Database")
                    .setMessage("Are you sure you want to delete ALL sessions?\n\n" +
                            "This includes:\n" +
                            "• All study sessions\n" +
                            "• All sensor logs\n" +
                            "• All questionnaire responses\n\n" +
                            "This cannot be undone!")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        TestDataGenerator.clearAllSessions(requireContext());
                        showToast("All sessions cleared");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Shows the questionnaire dialog flow for testing.
     */
    private void showQuestionnairesTest() {
        EmotionSelectDialogFragment emotionDialog = new EmotionSelectDialogFragment();
        emotionDialog.setOnEmotionSelectedListener((emotionRating) -> {
            ProductivityQuestionsDialogFragment productivityQuestionnaire = ProductivityQuestionsDialogFragment.create();

            productivityQuestionnaire.setListener(new ProductivityQuestionsDialogFragment.OnQuestionnaireActionListener() {
                @Override
                public void onQuestionnaireComplete(ProductivityQuestionnaireResult result) {
                    showToast("Detailed questionnaire completed!");
                }

                @Override
                public void onQuestionnaireSkipped() {
                    showToast("Quick questionnaire saved.");
                }
            });

            productivityQuestionnaire.show(getChildFragmentManager(), "detailed_questionnaire");
        });

        emotionDialog.show(getChildFragmentManager(), "emotion_dialog");
    }

    /**
     * Shows a short toast message.
     *
     * @param message the message to display
     */
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a delayed completion toast message with a checkmark.
     *
     * @param message the message to display
     */
    private void showCompletionToast(String message) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getContext() != null) {
                Toast.makeText(requireContext(), "✓ " + message, Toast.LENGTH_LONG).show();
            }
        }, 2500);
    }
}
