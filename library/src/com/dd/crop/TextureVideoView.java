package com.dd.crop;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

/*
 *    The MIT License (MIT)
 *
 *   Copyright (c) 2014 Danylyk Dmytro
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 */

public class TextureVideoView extends TextureView implements TextureView.SurfaceTextureListener {

    // Indicate if logging is on
    public static final boolean LOG_ON = true;

    // Log tag
    private static final String TAG = TextureVideoView.class.getName();

    private MediaPlayer mMediaPlayer;
    private int currentPosition = 0;
    private boolean haveToSeek = false;

    private float mVideoHeight;
    private float mVideoWidth;

    private boolean mIsDataSourceSet;
    private boolean mIsViewAvailable;
    private boolean mIsVideoPrepared;
    private boolean mIsPlayCalled;
    private boolean mPreventPlayback;

    private ScaleType mScaleType;
    private State mState;
    private LogCallback logCallback;

    public enum ScaleType {
        CENTER_CROP, TOP, BOTTOM
    }

    public enum State {
        UNINITIALIZED, PLAY, STOP, PAUSE, END
    }

    public interface LogCallback {
        void onLogMessageReceived(String message);
    }

    public TextureVideoView(Context context) {
        super(context);
        initView();
    }

    public TextureVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public TextureVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        initPlayer();
        setScaleType(ScaleType.CENTER_CROP);
        setSurfaceTextureListener(this);
    }

    public LogCallback getLogCallback() {
        return logCallback;
    }

    public void setLogCallback(LogCallback logCallback) {
        this.logCallback = logCallback;
    }

    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
    }

    private void updateTextureViewSize() {

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (mVideoWidth > viewWidth && mVideoHeight > viewHeight) {
            //TODO: need to verify logic for larger video sizes
            scaleX = mVideoWidth / viewWidth;
            scaleY = mVideoHeight / viewHeight;
        } else if (mVideoWidth / mVideoHeight > viewWidth / viewHeight) {
            scaleY = 1f;
            scaleX = (viewHeight / mVideoHeight) / (viewWidth / mVideoWidth);
        } else {
            scaleX = 1f;
            scaleY = (viewWidth / mVideoWidth) / (viewHeight / mVideoHeight);
        }

        log("Video Details " + viewHeight + " " + viewWidth);

        // Calculate pivot points, in our case crop from center
        int pivotPointX;
        int pivotPointY;

        switch (mScaleType) {
            case TOP:
                pivotPointX = 0;
                pivotPointY = 0;
                break;
            case BOTTOM:
                pivotPointX = (int) (viewWidth);
                pivotPointY = (int) (viewHeight);
                break;
            case CENTER_CROP:
                pivotPointX = (int) (viewWidth / 2);
                pivotPointY = (int) (viewHeight / 2);
                break;
            default:
                pivotPointX = (int) (viewWidth / 2);
                pivotPointY = (int) (viewHeight / 2);
                break;
        }

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);

        setTransform(matrix);
    }

    private void initPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        } else {
            mMediaPlayer.reset();
        }
        mIsVideoPrepared = false;
        mIsPlayCalled = false;
        mPreventPlayback = false;
        mState = State.UNINITIALIZED;
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(String)
     */
    public void setDataSource(String path) {
        initPlayer();

        try {
            mMediaPlayer.setDataSource(path);
            mIsDataSourceSet = true;
            prepare();
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(android.content.Context, android.net.Uri)
     */
    public void setDataSource(Context context, Uri uri) {
        initPlayer();

        try {
            mMediaPlayer.setDataSource(context, uri);
            mIsDataSourceSet = true;
            prepare();
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(java.io.FileDescriptor)
     */
    public void setDataSource(AssetFileDescriptor afd) {
        initPlayer();

        try {
            long startOffset = afd.getStartOffset();
            long length = afd.getLength();
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), startOffset, length);
            mIsDataSourceSet = true;
            prepare();
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    private void prepare() {
        try {
            mMediaPlayer.setOnVideoSizeChangedListener(
                    new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                            Log.e("OnVideo", width + " " + height);
                            if (!mIsVideoPrepared) {
                                mVideoWidth = width;
                                mVideoHeight = height;
                            }
                            updateTextureViewSize();
                        }
                    }
            );
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (!mp.isLooping()) {
                        mState = State.END;
                        log("Video has ended.");

                        if (mListener != null) {
                            mListener.onVideoEnd();
                        }
                    }
                }
            });

            // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
            // creating MediaPlayer
            mMediaPlayer.prepareAsync();

            // Play video when the media source is ready for playback.
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mIsVideoPrepared = true;
                    if (mIsPlayCalled && mIsViewAvailable && !mPreventPlayback) {
                        log("Player is prepared and play() was called.");
                        play();
                    }

                    if (mListener != null) {
                        mListener.onVideoPrepared();
                    }
                }
            });

        } catch (IllegalArgumentException e) {
            log(e.getMessage());
        } catch (SecurityException e) {
            log(e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            log(e.toString());
        }
    }

    /**
     * Play or resume video. Video will be played as soon as view is available and media player is
     * prepared.
     * <p>
     * If video is stopped or ended and play() method was called, video will start over.
     */
    public void play() {
        if (!mIsDataSourceSet) {
            log("play() was called but data source was not set.");
            return;
        }

        mIsPlayCalled = true;

        if (!mIsVideoPrepared) {
            log("play() was called but video is not prepared yet, waiting.");
            return;
        }

        if (!mIsViewAvailable) {
            log("play() was called but view is not available yet, waiting.");
            return;
        }

        if (mState == State.PLAY) {
            log("play() was called but video is already playing.");
            return;
        }

        if (mState == State.PAUSE) {
            log("play() was called but video is paused, resuming.");
            mState = State.PLAY;
            mMediaPlayer.start();
            return;
        }

        if (mState == State.END) {
            log("play() called but video ended");
            return;
        }

        if (mState == State.STOP) {
            log("play() was called but video already ended, starting over.");
            mState = State.PLAY;
            mMediaPlayer.seekTo(0);
            mMediaPlayer.start();
            return;
        }

        if (mMediaPlayer == null) {
            log("play() mediaPlayer is null");
            return;
        }

        if (haveToSeek) {
            mMediaPlayer.seekTo(currentPosition);
            haveToSeek = false;
        }

        mState = State.PLAY;
        mMediaPlayer.start();
    }

    /**
     * Pause video. If video is already paused, stopped or ended nothing will happen.
     */
    public void pause() {
        if (mState == State.PAUSE) {
            log("pause() was called but video already paused.");
            return;
        }

        if (mState == State.STOP) {
            log("pause() was called but video already stopped.");
            return;
        }

        if (mState == State.END) {
            log("pause() was called but video already ended.");
            return;
        }

        if (mMediaPlayer == null) {
            log("pause() mediaPlayer is null");
            return;
        }

        mState = State.PAUSE;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    /**
     * Stop video (pause and seek to beginning). If video is already stopped or ended nothing will
     * happen.
     */
    public void stop() {
        if (mState == State.STOP) {
            log("stop() was called but video already stopped.");
            return;
        }

        if (mState == State.END) {
            log("stop() was called but video already ended.");
            return;
        }

        if (mMediaPlayer == null) {
            log("stop() mediaPlayer is null");
            return;
        }

        mState = State.STOP;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mMediaPlayer.seekTo(0);
        }
    }

    /**
     * Prevent playback from callbacks - onSurfaceTextureAvailable and media player prepared
     */
    public void preventPlayback() {
        mPreventPlayback = true;
    }

    /**
     * @see android.media.MediaPlayer#setLooping(boolean)
     */
    public void setLooping(boolean looping) {
        mMediaPlayer.setLooping(looping);
    }

    /**
     * @see android.media.MediaPlayer#seekTo(int)
     */
    public void seekTo(int milliseconds) {
        mMediaPlayer.seekTo(milliseconds);
    }

    /**
     * @see android.media.MediaPlayer#getDuration()
     */
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    void log(String message) {
        if (LOG_ON) {
            Log.d(TAG, message);

            if (logCallback != null) {
                logCallback.onLogMessageReceived(message);
            }
        }
    }

    private MediaPlayerListener mListener;

    /**
     * Listener trigger 'onVideoPrepared' and `onVideoEnd` events
     */
    public void setListener(MediaPlayerListener listener) {
        mListener = listener;
    }

    public interface MediaPlayerListener {

        public void onVideoPrepared();

        public void onVideoEnd();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        log("Surface Available");
        if (mState != State.END && !mPreventPlayback && mMediaPlayer != null) {

            Surface surface = new Surface(surfaceTexture);
            mMediaPlayer.setSurface(surface);
            mIsViewAvailable = true;

            updateTextureViewSize();

            if (mIsDataSourceSet && mIsPlayCalled && mIsVideoPrepared) {
                log("View is available and play() was called.");
                play();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        log("Surface Destroy");
        if (mMediaPlayer == null) {
            return false;
        }

        currentPosition = mMediaPlayer.getCurrentPosition();
        mMediaPlayer.release();
        mMediaPlayer = null;
        mIsViewAvailable = false;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void releasePlayer() {
        // cannot use media player after this
        mState = State.END;
        mMediaPlayer.release();
    }

    public void seekToCurrent() {
        haveToSeek = true;
    }

    public boolean isSurfaceAvailable() {
        return mIsViewAvailable;
    }

    public void muteAudio() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(0, 0);
        }
    }

    public void unMuteAudio() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(1, 1);
        }
    }

}
