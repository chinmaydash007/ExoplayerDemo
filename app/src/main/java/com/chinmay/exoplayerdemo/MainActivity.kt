package com.chinmay.exoplayerdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.chinmay.exoplayerdemo.Constants.SEEK_BAR_MAX
import com.chinmay.exoplayerdemo.Constants.SEEK_BAR_PROGRESS
import com.chinmay.exoplayerdemo.databinding.MainActivityBinding
import com.chinmay.exoplayerdemo.service.MediaService

class MainActivity : AppCompatActivity(), MediaBrowserHelperCallback {
    var TAG = this.javaClass.simpleName
    lateinit var binding: MainActivityBinding
    lateinit var mediaBrowserHelper: MediaBrowserHelper
    var isPlaying = false
    private lateinit var mSeekbarBroadcastReceiver: SeekBarBroadcastReceiver
    private var selectedMediaMetadataCompat: MediaMetadataCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        if(savedInstanceState!=null){
            selectedMediaMetadataCompat=savedInstanceState.getParcelable<MediaMetadataCompat>("selectedMedia")
            isPlaying=savedInstanceState.getBoolean("isPlaying")
            changePlayPauseImage(isPlaying)

        }


        val myApplication: MyApplication = MyApplication.getInstance()
        mediaBrowserHelper = MediaBrowserHelper(this, MediaService::class.java)
        mediaBrowserHelper.setMediaBrowserHelperCallback(this)

        val mMediaLibrary = mutableListOf(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "11111")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Mitch Tabian & Jim Wilson")
                .putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                    "https://codingwithmitch.s3.amazonaws.com/static/profile_images/default_avatar.jpg"
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    "Yoga Session-Episode 1"
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                    "https://tik.getvisitapp.com/output/session-44/hls/session-44-Mindful-use-of-Technology.m3u8"
                )
                .build()
        )

        myApplication.setMediaItems(mMediaLibrary)

        binding.playPause.setOnClickListener {
            if (isPlaying) {
                mediaBrowserHelper.transportControls.pause()
                isPlaying = false
            } else {
                mediaBrowserHelper.transportControls.play()
                isPlaying = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserHelper.onStart()
    }

    override fun onResume() {
        super.onResume()
        initSeekBarBroadcastReceiver()
    }

    override fun onPause() {
        super.onPause()
        if (mSeekbarBroadcastReceiver != null) {
            unregisterReceiver(mSeekbarBroadcastReceiver)
        }
    }


    override fun onStop() {
        super.onStop()
        mediaBrowserHelper.onStop()
    }


    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        if (metadata != null) {
            Log.d(TAG, "onMetadataChanged: called" + metadata.description.title)
            setSongTitle(metadata.description.title)
        }
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        var mIsPlaying = state != null &&
                state.state == PlaybackStateCompat.STATE_PLAYING
        Log.d(TAG, "onPlaybackStateChanged: called. $mIsPlaying")
        changePlayPauseImage(mIsPlaying)
    }

    private inner class SeekBarBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val seekProgress = intent.getLongExtra(SEEK_BAR_PROGRESS, 0)
            val seekMax = intent.getLongExtra(SEEK_BAR_MAX, 0)
            var timeElapsed = seekProgress / 1000
            var totalTime = seekMax / 1000
            binding.textView.text = "$timeElapsed / $totalTime"
            Log.d(TAG, "seekProgress:$seekProgress     seekmax:$seekMax")
        }
    }

    private fun initSeekBarBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(getString(R.string.broadcast_seekbar_update))
        mSeekbarBroadcastReceiver = SeekBarBroadcastReceiver()
        registerReceiver(mSeekbarBroadcastReceiver, intentFilter)
    }

    private fun changePlayPauseImage(mIsPlaying: Boolean) {
        if (mIsPlaying) {
            binding.playPause.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_pause_circle_outline_white_24dp
                )
            )
        } else {
            binding.playPause.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_play_circle_outline_white_24dp
                )
            )

        }
    }

    private fun setSongTitle(title: CharSequence?) {
        binding.mediaSongTitle.text = title
    }

    fun addToMediaList(randomMedia: RandomMedia): MediaMetadataCompat {
        val media = MediaMetadataCompat.Builder()
            .putString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                randomMedia.mediaId
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                randomMedia.artist
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                randomMedia.title
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                randomMedia.media_uri
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                randomMedia.desc
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                randomMedia.icon_Uri
            )
            .build()
        return media
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("selectedMedia", selectedMediaMetadataCompat)
        outState.putBoolean("isPlaying", isPlaying)
    }



}

data class RandomMedia(
    var mediaId: String,
    var artist: String,
    var title: String,
    var media_uri: String,
    var desc: String,
    var icon_Uri: String
)