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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.cibotechnology.animation.CardFlipper;
import com.cibotechnology.animation.CardFlipperDelegate;

/**
 * Main activity: shows media player buttons. This activity shows the media
 * player buttons and lets the user click them. No media handling is done here
 * -- everything is done by passing Intents to our {@link MusicService}.
 * */
public class MainActivity extends Activity implements OnClickListener, CardFlipperDelegate {
    /**
     * The URL we suggest as default when adding by URL. This is just so that
     * the user doesn't have to find an URL to test this sample.
     */
    final String STREAM_URL = "http://1331.live.streamtheworld.com:80/KXNTAM_SC";

    Button mPlayButton;
    Button mPauseButton;
    Button mFlipButton;
    Button mDoneButton;
    ViewGroup mFrontFace;
    ViewGroup mBackFace;
    ViewGroup mContainer;

    /**
     * Called when the activity is first created. Here, we simply set the event
     * listeners and start the background service ({@link MusicService}) that
     * will handle the actual media playback.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mContainer = (ViewGroup) findViewById(R.id.container);
        mFrontFace = (ViewGroup) findViewById(R.id.frontface);
        mBackFace = (ViewGroup) findViewById(R.id.backface);
        mPlayButton = (Button) findViewById(R.id.playbutton);
        mPauseButton = (Button) findViewById(R.id.pausebutton);
        mFlipButton = (Button) findViewById(R.id.flipbutton);
        mDoneButton = (Button) findViewById(R.id.donebutton);

        mPlayButton.setOnClickListener(this);
        mPauseButton.setOnClickListener(this);
        mFlipButton.setOnClickListener(this);
        mDoneButton.setOnClickListener(this);
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

    private void play() {
        Intent i = new Intent(MusicService.ACTION_URL);
        Uri uri = Uri.parse(STREAM_URL);
        i.setData(uri);
        startService(i);
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
}
