package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.media.MediaPlayer;

public final class SoundPlayer {

    private static MediaPlayer mediaPlayer;

    private SoundPlayer() {}

    public static void playSound(Context context, int soundResourceId) {
        if (context == null) {
            return;
        }

        // If a sound is already playing, stops and it before starting a new one
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try {
            mediaPlayer = MediaPlayer.create(context, soundResourceId);
            // The listener manages the static instance
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }
}
