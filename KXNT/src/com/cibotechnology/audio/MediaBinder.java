package com.cibotechnology.audio;

import android.media.MediaPlayer;
import android.os.Binder;

public class MediaBinder extends Binder {
    MediaPlayer mPlayer;

    public MediaBinder(MediaPlayer player) {
        mPlayer = player;
    }

    public MediaPlayer getMediaPlayer() {
        return mPlayer;
    }
}
