package com.cibotechnology.audio;

public interface AudioStreamListener {
    public void OnAudioLevelChange(float leftLevel, float rightLevel);
}
