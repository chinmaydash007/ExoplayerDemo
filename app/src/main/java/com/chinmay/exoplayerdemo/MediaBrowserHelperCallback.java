package com.chinmay.exoplayerdemo;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public interface MediaBrowserHelperCallback {

    void onMetadataChanged(final MediaMetadataCompat metadata);

    void onPlaybackStateChanged(PlaybackStateCompat state);

}









