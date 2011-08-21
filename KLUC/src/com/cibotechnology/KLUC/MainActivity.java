/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cibotechnology.KLUC;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cibotechnology.audio.AudioStreamListener;
import com.cibotechnology.audio.MediaBinder;
import com.cibotechnology.visualization.AudioVisualizer;

/**
 * Main activity: shows media player buttons. This activity shows the media
 * player buttons and lets the user click them. No media handling is done here
 * -- everything is done by passing Intents to our {@link MusicService}.
 * */
public class MainActivity extends Activity implements OnClickListener, ServiceConnection, AudioStreamListener {
    private static final String TAG = "com.cibotechnology.KLUC.MainActivity";

    Button mPlayButton;
    Button mPauseButton;
    Button mContactButton;
    AudioVisualizer mAudioVisualizer;
    Properties mConfig;
    ImageView mLoadingImage;
    ProgressBar mLoadingIndicator;

    /**
     * Called when the activity is first created. Here, we simply set the event
     * listeners and start the background service ({@link MusicService}) that
     * will handle the actual media playback.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.main);

            wireupUIListeners();
            readRadioSettings();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        stopVisualization();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        startVisualization();
    }

    @Override
    public void onWindowAttributesChanged(LayoutParams params) {
        super.onWindowAttributesChanged(params);
        Log.w(TAG, "onWindowAttributesChanged: " + params);
    }

    private void wireupUIListeners() {
        mPlayButton = (Button) findViewById(R.id.playbutton);
        mPauseButton = (Button) findViewById(R.id.pausebutton);
        mContactButton = (Button) findViewById(R.id.contactButton);
        mAudioVisualizer = (AudioVisualizer) findViewById(R.id.audiovisualizer);
        mLoadingImage = (ImageView) findViewById(R.id.loadingButton);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.loadingIndicator);

        mPlayButton.setOnClickListener(this);
        mPauseButton.setOnClickListener(this);
        mContactButton.setOnClickListener(this);
    }

    private void readRadioSettings() throws IOException {
        Resources resources = this.getResources();
        AssetManager assetManager = resources.getAssets();
        InputStream inputStream = assetManager.open("RadioConfig.properties");
        mConfig = new Properties();
        mConfig.load(inputStream);
        // System.out.println("configuration loaded: " + mConfig);
    }

    @Override
    public void onClick(View target) {
        if (target == mPlayButton) {
            handlePlayCommand();
        } else if (target == mPauseButton) {
            handlePauseCommand();
        } else if (target == mContactButton) {
            handleHomepageCommand();
        }
    }

    private void handleHomepageCommand() {
        String homepage = mConfig.getProperty("homepage");
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(homepage));
        startActivity(browserIntent);
    }

    private void handlePlayCommand() {
        Intent i = new Intent(MusicService.ACTION_URL);
        Uri uri = Uri.parse(mConfig.getProperty("source"));
        i.setData(uri);
        startService(i);

        boolean bound = this.bindService(i, this, BIND_AUTO_CREATE);
        if (!bound) {
            Toast.makeText(getApplicationContext(), "Could not bind to music service", Toast.LENGTH_SHORT).show();
        } else {
            this.setUIStateLoading();
        }
    }

    private void handlePauseCommand() {
        startService(new Intent(MusicService.ACTION_PAUSE));
    }

    private void setUIStateLoading() {
        mPlayButton.setVisibility(View.INVISIBLE);

        mLoadingImage.setVisibility(View.VISIBLE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    private void setUIStatePlaying() {
        mLoadingImage.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mPlayButton.setVisibility(View.INVISIBLE);

        mPauseButton.setVisibility(View.VISIBLE);
    }

    private void setUIStatePaused() {
        mPauseButton.setVisibility(View.INVISIBLE);
        mLoadingImage.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);

        mPlayButton.setVisibility(View.VISIBLE);
    }

    // ServiceConnection

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Log.e(TAG, "onServiceConnected");

        ((MediaBinder) service).setListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Log.e(TAG, "onServiceDisconnected");

        stopVisualization();
    }

    // Audio Visualization

    public static int snoop(short[] outData, int kind) {
        try {
            @SuppressWarnings("rawtypes")
            Class c = MediaPlayer.class;
            Method m = c.getMethod("snoop", outData.getClass(), Integer.TYPE);
            m.setAccessible(true);
            return Integer.parseInt((m.invoke(c, outData, kind)).toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 1;
        }
    }

    boolean mWatchingAudioLevels = false;
    MediaPlayer mPlayer = null;
    protected short[] mAudioData = new short[1024];
    public Handler snoopHandler = new Handler();
    public final Runnable snoopThread = new Runnable() {
        @Override
        public void run() {
            if (mWatchingAudioLevels && (null != mPlayer)) {
                snoop(mAudioData, 0);
                mAudioVisualizer.setAudioData(mAudioData);
                snoopHandler.postDelayed(this, 1);
            }
        }
    };

    public void startVisualization() {
        mWatchingAudioLevels = true;
        snoopHandler.postDelayed(snoopThread, 1);
    }

    private void stopVisualization() {
        mWatchingAudioLevels = false;
        mAudioVisualizer.setAudioData(null); // clear the back buffer
    }

    // AudioStreamListener

    @Override
    public void OnMediaPlayerChange(MediaPlayer player) {
        mPlayer = player;

        if ((null != player) && (player.isPlaying())) {
            startVisualization();
            setUIStatePlaying();
        } else {
            stopVisualization();
            setUIStatePaused();
        }
    }
}
