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


class MainActivity : AppCompatActivity(), MediaBrowserHelperCallback, StartPlaying {
    var TAG = this.javaClass.simpleName
    lateinit var binding: MainActivityBinding
    lateinit var mediaBrowserHelper: MediaBrowserHelper
    var isPlaying = false
    private lateinit var mSeekbarBroadcastReceiver: SeekBarBroadcastReceiver
    private var selectedMediaMetadataCompat: MediaMetadataCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        if (savedInstanceState != null) {
            selectedMediaMetadataCompat =
                savedInstanceState.getParcelable<MediaMetadataCompat>("selectedMedia")
            isPlaying = savedInstanceState.getBoolean("isPlaying")
            changePlayPauseImage(isPlaying)
        }

        val myApplication: MyApplication = MyApplication.getInstance()
        mediaBrowserHelper = MediaBrowserHelper(this, MediaService::class.java)
        mediaBrowserHelper.setMediaBrowserHelperCallback(this)


        var randomMedia = RandomMedia(
            "11111",
            "Yoga Teacher",
            "Yoga Session-Episode 1",
            "https://tik.getvisitapp.com/output/session-44/hls/session-44-Mindful-use-of-Technology.m3u8",
            "",
            "https://codingwithmitch.s3.amazonaws.com/static/profile_images/default_avatar.jpg"
        )


        val mMediaLibrary = getMediaLibrary(randomMedia)

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

    private fun getMediaLibrary(randomMedia: RandomMedia): MutableList<MediaMetadataCompat> {
        val mMediaLibrary = mutableListOf(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, randomMedia.mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, randomMedia.artist)
                .putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                    randomMedia.icon_Uri
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    randomMedia.title
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                    randomMedia.media_uri
                )
                .build()
        )
        return mMediaLibrary
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

    //these methods are responsible for changing the ui state when the media start playing
    //1
    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        if (metadata != null) {
            Log.d(TAG, "onMetadataChanged: called" + metadata.description.title)
            setSongTitle(metadata.description.title)
        }
    }

    //2
    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        var mIsPlaying = state != null &&
                state.state == PlaybackStateCompat.STATE_PLAYING
        Log.d(TAG, "onPlaybackStateChanged: called. $mIsPlaying")
        changePlayPauseImage(mIsPlaying)
    }

    //Broadcast receiver for updating timer.
    private inner class SeekBarBroadcastReceiver : BroadcastReceiver() {
        //3
        override fun onReceive(context: Context, intent: Intent) {
            val seekProgress = intent.getLongExtra(SEEK_BAR_PROGRESS, 0)
            val seekMax = intent.getLongExtra(SEEK_BAR_MAX, 0)
            var timeElapsed = seekProgress / 1000
            var totalTime = seekMax / 1000

            if (seekMax > 0) {
                binding.textView.text = "${getDuration(timeElapsed)} / ${getDuration(totalTime)}"
            }

            Log.d(TAG, "seekProgress:$seekProgress     seekmax:$seekMax")
        }
    }

    private fun getDuration(seconds: Long): String {
        val p1 = (seconds % 60).toInt()
        var p2 = (seconds / 60).toInt()
        val p3 = p2 % 60
        p2 = p2 / 60
        return "$p2:$p3:$p1"

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


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("selectedMedia", selectedMediaMetadataCompat)
        outState.putBoolean("isPlaying", isPlaying)
    }

    //this method is a which control play() from MediaBrwserHelper.
    override fun isConnectedAndStartplayingAudio() {
        Log.d(TAG, "start Audio playback")
        mediaBrowserHelper.transportControls.play()
        isPlaying=true
    }
}

//model class for API response
data class RandomMedia(
    var mediaId: String,
    var artist: String,
    var title: String,
    var media_uri: String,
    var desc: String,
    var icon_Uri: String
)