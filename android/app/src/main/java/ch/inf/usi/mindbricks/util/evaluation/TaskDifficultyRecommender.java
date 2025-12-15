package ch.inf.usi.mindbricks.util.evaluation;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;


/**
 * Analyzes PAM score trends to detect cognitive overload and recommend
 * task difficulty adjustments.
 *
 * Evidence base:
 * - After 3+ consecutive low-scoring sessions (≤25), users experience 31% higher
 *   mental effort and are 40% more likely to abandon tasks
 * - Dynamic task scaffolding reduces task abandonment by 18%
 * - Systematic breaks study: Fixed intervals with high difficulty lead to burnout
 *
 * Papers on the theme:
 * https://pubmed.ncbi.nlm.nih.gov/36859717/
 * https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2022.858411/pdf
 * https://www.grafiati.com/en/literature-selections/pomodoro-technique/
 * https://pubmed.ncbi.nlm.nih.gov/36859717/
 *
 */
public class TaskDifficultyRecommender {

    private static final String TAG = "TaskDifficultyRecommender";

    // Threshold for "low" score that indicates struggle
    private static final int LOW_SCORE_THRESHOLD = 25;

    // Number of consecutive low scores that trigger intervention
    private static final int CONSECUTIVE_THRESHOLD = 3;

    public enum RecommendationAction {
        MAINTAIN_CURRENT("Continue with current approach"),
        REDUCE_DIFFICULTY("Switch to an easier sub-task"),
        ADD_SCAFFOLDING("Break task into smaller steps"),
        SWITCH_TASK("Try a different task for now"),
        TAKE_LONGER_BREAK("Take an extended 15-20 min break");

        private final String description;

        RecommendationAction(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class TaskRecommendation {
        private RecommendationAction action;
        private String reason;
        private List<String> specificSuggestions;
        private int confidenceLevel; // 0-100

        public TaskRecommendation(RecommendationAction action, String reason) {
            this.action = action;
            this.reason = reason;
            this.specificSuggestions = new ArrayList<>();
            this.confidenceLevel = 50;
        }

        public RecommendationAction getAction() { return action; }
        public String getReason() { return reason; }
        public List<String> getSpecificSuggestions() { return specificSuggestions; }
        public int getConfidenceLevel() { return confidenceLevel; }

        public void addSuggestion(String suggestion) {
            this.specificSuggestions.add(suggestion);
        }

        public void setConfidenceLevel(int level) {
            this.confidenceLevel = Math.max(0, Math.min(100, level));
        }

        @Override
        public String toString() {
            return String.format("TaskRecommendation{action=%s, confidence=%d%%, reason='%s'}",
                    action, confidenceLevel, reason);
        }
    }

    /**
     * Main analysis method: Analyzes recent PAM scores and generates recommendations
     *
     * @param recentScores List of recent PAM scores (ideally last 5-10 sessions)
     * @param thresholds User's PAM thresholds from preferences
     * @return TaskRecommendation object with actionable advice
     */
    public static TaskRecommendation analyzeRecentSessions(List<PAMScore> recentScores,
                                                           UserPreferences.PAMThresholds thresholds) {

        if (recentScores == null || recentScores.isEmpty()) {
            return new TaskRecommendation(
                    RecommendationAction.MAINTAIN_CURRENT,
                    "Not enough data for analysis"
            );
        }

        // Count consecutive low scores (from most recent backward)
        int consecutiveLowCount = countConsecutiveLowScores(recentScores, LOW_SCORE_THRESHOLD);

        // Detect declining trend
        boolean decliningTrend = detectDecliningTrend(recentScores);

        // Identify which PAM dimension is most problematic
        String problematicDimension = identifyProblematicDimension(recentScores);

        Log.i(TAG, String.format("Analysis: %d consecutive low scores, declining=%b, problem=%s",
                consecutiveLowCount, decliningTrend, problematicDimension));

        // Generate recommendation based on analysis

        // CRITICAL:
        // 3+ consecutive low scores
        if (consecutiveLowCount >= thresholds.getConsecutiveLowSessionsAlert()) {
            return generateCriticalRecommendation(consecutiveLowCount, problematicDimension);
        }

        // WARNING:
        // Declining trend over 5+ sessions
        if (decliningTrend && recentScores.size() >= 5) {
            return generateDecliningTrendRecommendation(problematicDimension);
        }

        // ATTENTION:
        // Last session was very low but not part of a pattern
        if (!recentScores.isEmpty() && recentScores.get(0).getTotalScore() <= 15) {
            return generateSingleLowScoreRecommendation(recentScores.get(0));
        }

        // ALL GOOD:
        // No intervention needed
        return new TaskRecommendation(
                RecommendationAction.MAINTAIN_CURRENT,
                "You're doing well! Keep up the current approach."
        );
    }

    private static int countConsecutiveLowScores(List<PAMScore> scores, int threshold) {
        int count = 0;
        for (PAMScore score : scores) {
            if (score.getTotalScore() <= threshold) {
                count++;
            } else {
                // Stop at first non-low score
                break;
            }
        }
        return count;
    }

    private static boolean detectDecliningTrend(List<PAMScore> scores) {
        if (scores.size() < 3) return false;

        // Simple trend detection: compare first half average to second half average
        int halfPoint = scores.size() / 2;

        float recentAvg = 0;
        for (int i = 0; i < halfPoint; i++) {
            recentAvg += scores.get(i).getTotalScore();
        }
        recentAvg /= halfPoint;

        float olderAvg = 0;
        for (int i = halfPoint; i < scores.size(); i++) {
            olderAvg += scores.get(i).getTotalScore();
        }
        olderAvg /= (scores.size() - halfPoint);

        // Declining if recent average is 10+ points lower than older average
        return recentAvg < (olderAvg - 10);
    }

    private static String identifyProblematicDimension(List<PAMScore> scores) {
        if (scores.isEmpty()) return "unknown";

        float avgPleasure = 0, avgArousal = 0, avgMotivation = 0;

        for (PAMScore score : scores) {
            avgPleasure += score.getPleasureScore();
            avgArousal += score.getArousalScore();
            avgMotivation += score.getMotivationScore();
        }

        int n = scores.size();
        avgPleasure /= n;
        avgArousal /= n;
        avgMotivation /= n;

        // Find lowest dimension
        float min = Math.min(avgPleasure, Math.min(avgArousal, avgMotivation));

        if (avgPleasure == min) return "pleasure";
        if (avgArousal == min) return "arousal";
        return "motivation";
    }

    @SuppressLint("DefaultLocale")
    private static TaskRecommendation generateCriticalRecommendation(int consecutiveCount,
                                                                     String problematicDimension) {
        TaskRecommendation rec;

        switch (problematicDimension) {
            case "pleasure":
                // Low satisfaction/enjoyment → Switch to more engaging task
                rec = new TaskRecommendation(
                        RecommendationAction.SWITCH_TASK,
                        String.format("You've had %d tough sessions. This task may not be engaging enough.",
                                consecutiveCount)
                );
                rec.addSuggestion("Switch to a task you find more interesting or meaningful");
                rec.addSuggestion("Pair this task with something you enjoy (music, good environment)");
                rec.addSuggestion("Reward yourself after completing small milestones");
                break;

            case "arousal":
                // Low energy/alertness → Need break or physical activity
                rec = new TaskRecommendation(
                        RecommendationAction.TAKE_LONGER_BREAK,
                        String.format("You've been fatigued for %d sessions. Your mind needs rest.",
                                consecutiveCount)
                );
                rec.addSuggestion("Take a 15-20 minute walk or exercise break");
                rec.addSuggestion("Change your study environment (different room, cafe, outdoors)");
                rec.addSuggestion("Ensure you're well-hydrated and have eaten recently");
                rec.addSuggestion("Consider if you need more sleep or a full rest day");
                break;

            case "motivation":
                // Low willingness to continue → Need scaffolding
                rec = new TaskRecommendation(
                        RecommendationAction.ADD_SCAFFOLDING,
                        String.format("You've struggled with motivation for %d sessions. Break it down!",
                                consecutiveCount)
                );
                rec.addSuggestion("Break the task into smaller 10-15 minute micro-tasks");
                rec.addSuggestion("Watch a 5-minute tutorial video for guidance");
                rec.addSuggestion("Start with the easiest part to build momentum");
                rec.addSuggestion("Find a study partner or accountability buddy");
                break;

            default:
                // General low scores → Reduce difficulty
                rec = new TaskRecommendation(
                        RecommendationAction.REDUCE_DIFFICULTY,
                        String.format("You've had %d challenging sessions. Time to adjust the difficulty.",
                                consecutiveCount)
                );
                rec.addSuggestion("Switch to a simpler sub-task within your larger goal");
                rec.addSuggestion("Use more scaffolding (templates, examples, guides)");
                rec.addSuggestion("Lower your standards temporarily - done is better than perfect");
                break;
        }

        rec.setConfidenceLevel(80 + Math.min(20, consecutiveCount * 5)); // Higher confidence with more evidence
        return rec;
    }

    private static TaskRecommendation generateDecliningTrendRecommendation(String problematicDimension) {
        TaskRecommendation rec = new TaskRecommendation(
                RecommendationAction.ADD_SCAFFOLDING,
                "Your scores are trending downward. Consider adjusting your approach before it gets harder."
        );

        rec.addSuggestion("Break your work into smaller, more manageable chunks");
        rec.addSuggestion("Add more frequent micro-breaks (2-3 minutes every 20 min)");
        rec.addSuggestion("Review your goals - are they realistic for this week?");

        if ("arousal".equals(problematicDimension)) {
            rec.addSuggestion("Your energy is dropping - consider changing study times");
        } else if ("motivation".equals(problematicDimension)) {
            rec.addSuggestion("Your motivation is waning - remind yourself why this matters");
        }

        rec.setConfidenceLevel(65);
        return rec;
    }

    private static TaskRecommendation generateSingleLowScoreRecommendation(PAMScore score) {
        @SuppressLint("DefaultLocale") TaskRecommendation rec = new TaskRecommendation(
                RecommendationAction.TAKE_LONGER_BREAK,
                String.format("That last session was tough (PAM: %d). Take a longer break.",
                        score.getTotalScore())
        );

        rec.addSuggestion("Take a 10-15 minute break before continuing");
        rec.addSuggestion("Consider if you need to switch tasks temporarily");

        String lowestDim = score.getLowestDimension();
        if ("arousal".equals(lowestDim)) {
            rec.addSuggestion("Your energy is low - try physical activity");
        } else if ("pleasure".equals(lowestDim)) {
            rec.addSuggestion("That task wasn't enjoyable - try something else for variety");
        } else {
            rec.addSuggestion("Your motivation dipped - reconnect with your goals");
        }

        rec.setConfidenceLevel(50);
        return rec;
    }
}