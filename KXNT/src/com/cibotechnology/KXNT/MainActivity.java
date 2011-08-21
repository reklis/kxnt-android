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

package com.cibotechnology.KXNT;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;
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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cibotechnology.animation.CardFlipper;
import com.cibotechnology.animation.CardFlipperDelegate;
import com.cibotechnology.audio.AudioStreamListener;
import com.cibotechnology.audio.MediaBinder;
import com.cibotechnology.visualization.AudioVisualizer;

/**
 * Main activity: shows media player buttons. This activity shows the media
 * player buttons and lets the user click them. No media handling is done here
 * -- everything is done by passing Intents to our {@link MusicService}.
 * */
public class MainActivity extends Activity implements OnClickListener, CardFlipperDelegate, ServiceConnection, AudioStreamListener {
    private static final String TAG = "com.cibotechnology.KXNT.MainActivity";

    Button mPlayButton;
    Button mPauseButton;
    Button mFlipButton;
    Button mDoneButton;
    ViewGroup mFrontFace;
    ViewGroup mBackFace;
    ViewGroup mContainer;
    AudioVisualizer mAudioVisualizer;
    Properties mConfig;
    TextView mNowPlayingBanner;
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
            getLatestTweet();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void wireupUIListeners() {
        mContainer = (ViewGroup) findViewById(R.id.container);
        mFrontFace = (ViewGroup) findViewById(R.id.frontface);
        mBackFace = (ViewGroup) findViewById(R.id.backface);
        mPlayButton = (Button) findViewById(R.id.playbutton);
        mPauseButton = (Button) findViewById(R.id.pausebutton);
        mFlipButton = (Button) findViewById(R.id.flipbutton);
        mDoneButton = (Button) findViewById(R.id.donebutton);
        mNowPlayingBanner = (TextView) findViewById(R.id.nowPlayingBanner);
        mAudioVisualizer = (AudioVisualizer) findViewById(R.id.audiovisualizer);
        mLoadingImage = (ImageView) findViewById(R.id.loadingButton);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.loadingIndicator);

        mPlayButton.setOnClickListener(this);
        mPauseButton.setOnClickListener(this);
        mFlipButton.setOnClickListener(this);
        mDoneButton.setOnClickListener(this);
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
            play();
        } else if (target == mPauseButton) {
            pause();
        } else if (target == mFlipButton) {
            flip(true);
        } else if (target == mDoneButton) {
            flip(false);
        }
    }

    private void getLatestTweet() {
        AsyncTwitter twitter = AsyncTwitterFactory.getSingleton();
        twitter.addListener(new TwitterAdapter() {
            @Override
            public void onException(TwitterException ex, TwitterMethod method) {
                ex.printStackTrace();
            }

            @Override
            public void gotUserTimeline(ResponseList<Status> statuses) {
                if ((null != statuses) && (0 != statuses.size())) {
                    Status s = statuses.get(0);
                    showLatestTweet(s.getUser().getName(), s.getText());
                }
            }
        });
        twitter.getUserTimeline(mConfig.getProperty("twitteraccount"));
    }

    public void showLatestTweet(String name, String text) {
        mNowPlayingBanner.setText("@" + name + ": " + text);
    }

    private void play() {
        Intent i = new Intent(MusicService.ACTION_URL);
        Uri uri = Uri.parse(mConfig.getProperty("source"));
        i.setData(uri);
        startService(i);

        boolean bound = this.bindService(i, this, BIND_AUTO_CREATE);
        if (!bound) {
            Toast.makeText(getApplicationContext(), "Could not bind to music service", Toast.LENGTH_SHORT).show();
        } else {
            mPlayButton.setVisibility(View.INVISIBLE);

            mNowPlayingBanner.setText("Loading...");
            mNowPlayingBanner.setVisibility(View.VISIBLE);
            mLoadingImage.setVisibility(View.VISIBLE);
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void pause() {
        startService(new Intent(MusicService.ACTION_PAUSE));

    }

    private void flip(boolean toBackside) {
        CardFlipper cardFlipper = new CardFlipper();
        cardFlipper.setDelegate(this);
        if (toBackside) {
            cardFlipper.flipFromRightToLeft();
        } else {
            cardFlipper.flipFromLeftToRight();
        }
    }

    // CardFlipperDelegate

    @Override
    public ViewGroup getContainer() {
        return this.mContainer;
    }

    @Override
    public void hideFrontFace() {
        mFrontFace.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showBackFace() {
        mBackFace.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideBackFace() {
        mBackFace.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showFrontFace() {
        mFrontFace.setVisibility(View.VISIBLE);
    }

    // ServiceConnection

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.e(TAG, "onServiceConnected");

        ((MediaBinder) service).setListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "onServiceDisconnected");

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

    MediaPlayer mPlayer = null;
    protected short[] mAudioData = new short[1024];
    protected Handler snoopHandler = new Handler();
    protected final Runnable snoopThread = new Runnable() {
        @Override
        public void run() {
            if (null != mPlayer) {
                snoop(mAudioData, 0);
                mAudioVisualizer.setAudioData(mAudioData);
                snoopHandler.postDelayed(this, 1);
            }
        }
    };

    public void startVisualization() {
        snoopHandler.postDelayed(snoopThread, 1);

        mLoadingImage.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mPlayButton.setVisibility(View.INVISIBLE);

        mNowPlayingBanner.setText(mConfig.getProperty("description"));
        mNowPlayingBanner.setVisibility(View.VISIBLE);
        mPauseButton.setVisibility(View.VISIBLE);
    }

    private void stopVisualization() {
        mAudioVisualizer.setAudioData(null);
        mPlayer = null;

        mPauseButton.setVisibility(View.INVISIBLE);
        mLoadingImage.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mNowPlayingBanner.setVisibility(View.INVISIBLE);

        mPlayButton.setVisibility(View.VISIBLE);
    }

    // AudioStreamListener

    @Override
    public void OnMediaPlayerChange(MediaPlayer player) {
        if ((null != player) && (player.isPlaying())) {
            mPlayer = player;
            startVisualization();
        } else {
            stopVisualization();
        }
    }
}
