package com.chinmay.exoplayerdemo.service;

import android.app.Notification;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

import com.chinmay.exoplayerdemo.MediaNotificationManager;
import com.chinmay.exoplayerdemo.MyApplication;
import com.chinmay.exoplayerdemo.R;

import java.util.ArrayList;
import java.util.List;

import static com.chinmay.exoplayerdemo.Constants.SEEK_BAR_MAX;
import static com.chinmay.exoplayerdemo.Constants.SEEK_BAR_PROGRESS;

public class MediaService extends MediaBrowserServiceCompat {

    private static final String TAG = "MediaService";

    private MediaSessionCompat mSession;
    private MediaPlayerAdapter mPlayback;
    private MyApplication mMyApplication;
    private MediaNotificationManager mMediaNotificationManager;
    private boolean mIsServiceStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        mMyApplication = MyApplication.getInstance();

        //Build the MediaSession
        mSession = new MediaSessionCompat(this, TAG);

        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                // https://developer.android.com/guide/topics/media-apps/mediabuttons#mediabuttons-and-active-mediasessions
                // Media buttons on the device
                // (handles the PendingIntents for MediaButtonReceiver.buildMediaButtonPendingIntent)

                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS); // Control the items in the queue (aka playlist)
        // See https://developer.android.com/guide/topics/media-apps/mediabuttons for more info on flags

        mSession.setCallback(new MediaSessionCallback());

        // A token that can be used to create a MediaController for this session
        setSessionToken(mSession.getSessionToken());

        mPlayback = new MediaPlayerAdapter(this, new MediaPlayerListener());
        mMediaNotificationManager = new MediaNotificationManager(this);

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved: stopped");
        super.onTaskRemoved(rootIntent);
        mPlayback.stop();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mSession.release();
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released");
    }


    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String s, int i, @Nullable Bundle bundle) {
        Log.d(TAG, "onGetRoot: called. ");
        if (s.equals(getApplicationContext().getPackageName())) {
            // Allowed to browse media
            return new BrowserRoot("some_real_playlist", null); // return no media
        }
        return new BrowserRoot("empty_media", null); // return no media
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren: called: " + s + ", " + result);

        //  Browsing not allowed
        if (TextUtils.equals("empty_media", s)) {
            result.sendResult(null);
            return;
        }

        result.sendResult(mMyApplication.getMediaItems()); // return all available media
    }


    public class MediaSessionCallback extends MediaSessionCompat.Callback {

        private final List<MediaSessionCompat.QueueItem> mPlaylist = new ArrayList<>();
        private int mQueueIndex = -1;
        private MediaMetadataCompat mPreparedMedia;

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId: CALLED.");
            mPreparedMedia = mMyApplication.getTreeMap().get(mediaId);
            mSession.setMetadata(mPreparedMedia);
            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
            mPlayback.playFromMedia(mPreparedMedia);
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onAddQueueItem: CALLED: position in list: " + mPlaylist.size());
            mPlaylist.add(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            mPlaylist.remove(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mPlaylist.isEmpty()) ? -1 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }

        @Override
        public void onPrepare() {
            if (mQueueIndex < 0 && mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            String mediaId = mPlaylist.get(mQueueIndex).getDescription().getMediaId();
            mPreparedMedia = mMyApplication.getTreeMap().get(mediaId);
            mSession.setMetadata(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }

        @Override
        public void onPlay() {

            if (!isReadyToPlay()) {
                // Nothing to play.
                return;
            }

            if (mPreparedMedia == null) {
                onPrepare();
            }

            mPlayback.playFromMedia(mPreparedMedia);

        }

        @Override
        public void onPause() {
            mPlayback.pause();
        }

        @Override
        public void onStop() {
            mPlayback.stop();
            mSession.setActive(false);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext: SKIP TO NEXT");
            // increment and then check using modulus
            mQueueIndex = (++mQueueIndex % mPlaylist.size());
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious: SKIP TO PREVIOUS");
            mQueueIndex = mQueueIndex > 0 ? mQueueIndex - 1 : mPlaylist.size() - 1;
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSeekTo(long pos) {
            mPlayback.seekTo(pos);
        }

        private boolean isReadyToPlay() {
            return (!mPlaylist.isEmpty());
        }
    }

    public class MediaPlayerListener implements PlaybackInfoListener {
        private ServiceManager mServiceManager;

        public MediaPlayerListener() {
            mServiceManager = new ServiceManager();
        }

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            mSession.setPlaybackState(state);

            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mServiceManager.displayNotification(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    Log.d(TAG, "onPlaybackStateChange: STOPPED.");
                    mServiceManager.moveServiceOutOfStartedState();
                    break;
            }
        }

        @Override
        public void onSeekTo(long progress, long max) {
//            Log.d(TAG, "onSeekTo: CALLED: updating seekbar: " + progress + ", max: " + max);
            Intent intent = new Intent();
            intent.setAction(getString(R.string.broadcast_seekbar_update));
            intent.putExtra(SEEK_BAR_PROGRESS, progress);
            intent.putExtra(SEEK_BAR_MAX, max);
            sendBroadcast(intent);
        }

        @Override
        public void onPlaybackComplete() {
            Log.d(TAG, "onPlaybackComplete: SKIPPING TO NEXT.");
            mSession.getController().getTransportControls().stop();
        }

        class ServiceManager {

            private PlaybackStateCompat mState;

            public ServiceManager() {
            }


            public void displayNotification(PlaybackStateCompat state) {

                // Manage the started state of this service.
                Notification notification = null;
                switch (state.getState()) {

                    case PlaybackStateCompat.STATE_PLAYING:
                        notification =
                                mMediaNotificationManager.buildNotification(
                                        state, getSessionToken(), mPlayback.getCurrentMedia().getDescription(), BitmapFactory.decodeResource(getBaseContext().getResources(), R.drawable.hello));

                        if (!mIsServiceStarted) {
                            ContextCompat.startForegroundService(
                                    MediaService.this,
                                    new Intent(MediaService.this, MediaService.class));
                            mIsServiceStarted = true;
                        }

                        startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
                        break;

                    case PlaybackStateCompat.STATE_PAUSED:
                        stopForeground(false);
                        notification =
                                mMediaNotificationManager.buildNotification(
                                        state, getSessionToken(), mPlayback.getCurrentMedia().getDescription(), BitmapFactory.decodeResource(getBaseContext().getResources(), R.drawable.hello));
                        mMediaNotificationManager.getNotificationManager()
                                .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
                        break;
                }
            }

            private void moveServiceOutOfStartedState() {
                stopForeground(true);
                stopSelf();
                mIsServiceStarted = false;
            }
        }

    }
}



















