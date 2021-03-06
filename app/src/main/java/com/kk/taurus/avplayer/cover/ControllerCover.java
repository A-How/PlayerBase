package com.kk.taurus.avplayer.cover;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kk.taurus.avplayer.play.EventConstant;
import com.kk.taurus.avplayer.R;
import com.kk.taurus.playerbase.event.BundlePool;
import com.kk.taurus.playerbase.event.EventKey;
import com.kk.taurus.playerbase.event.OnPlayerEventListener;
import com.kk.taurus.playerbase.player.OnTimerUpdateListener;
import com.kk.taurus.playerbase.receiver.ICover;
import com.kk.taurus.playerbase.receiver.IReceiverGroup;
import com.kk.taurus.playerbase.touch.OnTouchGestureListener;
import com.kk.taurus.playerbase.receiver.BaseCover;
import com.kk.taurus.playerbase.utils.TimeUtil;

/**
 * Created by Taurus on 2018/4/15.
 */

public class ControllerCover extends BaseCover implements OnTimerUpdateListener, OnTouchGestureListener{

    private final int MSG_CODE_DELAY_HIDDEN_CONTROLLER = 101;

    private View mTopContainer;
    private View mBottomContainer;

    private ImageView mBackIcon;
    private ImageView mStateIcon;

    private TextView mCurrTime;
    private TextView mTotalTime;

    private View mFastForwardBox;
    private TextView mFastForwardStepTime;
    private TextView mFastForwardProgressTime;

    private SeekBar mSeekBar;
    private int mBufferPercentage;

    private int mSeekProgress = -1;

    private int mCurrentPosition;
    private int mDuration;

    private boolean mTimerUpdateProgressEnable = true;

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CODE_DELAY_HIDDEN_CONTROLLER:
                    setControllerState(false);
                    break;
            }
        }
    };
    private boolean firstTouch;
    private boolean horizontalSlide;
    private boolean rightVerticalSlide;

    private int mWidth,mHeight;
    private long newPosition;

    private boolean mGestureEnable = true;
    private boolean mHorizontalSlide;

    public ControllerCover(Context context) {
        super(context);
    }

    @Override
    public void onReceiverCreate() {
        super.onReceiverCreate();
        mTopContainer = findViewById(R.id.cover_player_controller_top_container);
        mBottomContainer = findViewById(R.id.cover_player_controller_bottom_container);

        mBackIcon = findViewById(R.id.cover_player_controller_image_view_back_icon);
        mStateIcon = findViewById(R.id.cover_player_controller_image_view_play_state);
        mCurrTime = findViewById(R.id.cover_player_controller_text_view_curr_time);
        mTotalTime = findViewById(R.id.cover_player_controller_text_view_total_time);
        mSeekBar = findViewById(R.id.cover_player_controller_seek_bar);

        mFastForwardBox = findViewById(R.id.cover_player_gesture_operation_fast_forward_box);
        mFastForwardStepTime = findViewById(R.id.cover_player_gesture_operation_fast_forward_text_view_step_time);
        mFastForwardProgressTime = findViewById(R.id.cover_player_gesture_operation_fast_forward_text_view_progress_time);

        mBackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyReceiverEvent(EventConstant.EVENT_CODE_CONTROLLER_REQUEST_FINISH, null);
            }
        });

        mStateIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean selected = mStateIcon.isSelected();
                notifyReceiverEvent(
                        selected?EventConstant.EVENT_CODE_CONTROLLER_REQUEST_RESUME
                                :EventConstant.EVENT_CODE_CONTROLLER_REQUEST_PAUSE, null);
                mStateIcon.setSelected(!selected);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
    }

    private IReceiverGroup.OnGroupValueUpdateListener mOnGroupValueUpdateListener =
            new IReceiverGroup.OnGroupValueUpdateListener() {
        @Override
        public String[] filterKeys() {
            return new String[]{EventConstant.KEY_COMPLETE_SHOW};
        }

        @Override
        public void onValueUpdate(String key, Object value) {
            if(key.equals(EventConstant.KEY_COMPLETE_SHOW)){
                boolean show = (boolean) value;
                if(show){
                    setControllerState(false);
                }
                setGestureEnable(!show);
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser)
                updateUI(progress, seekBar.getMax());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            sendSeekEvent(seekBar.getProgress());
        }
    };

    private void sendSeekEvent(int progress){
        mTimerUpdateProgressEnable = false;
        mSeekProgress = progress;
        mHandler.removeCallbacks(mSeekEventRunnable);
        mHandler.postDelayed(mSeekEventRunnable, 300);
    }

    private Runnable mSeekEventRunnable = new Runnable() {
        @Override
        public void run() {
            if(mSeekProgress < 0)
                return;
            Bundle bundle = BundlePool.obtain();
            bundle.putInt(EventKey.INT_DATA, mSeekProgress);
            notifyReceiverEvent(EventConstant.EVENT_CODE_CONTROLLER_REQUEST_SEEK,bundle);
        }
    };

    private void setFastForwardState(boolean state) {
        mFastForwardBox.setVisibility(state?View.VISIBLE:View.GONE);
    }

    private void setFastForwardStepTime(String text) {
        mFastForwardStepTime.setText(text);
    }

    private void setFastForwardProgressTime(String text) {
        mFastForwardProgressTime.setText(text);
    }

    public void setGestureEnable(boolean gestureEnable) {
        this.mGestureEnable = gestureEnable;
    }

    private void setTopContainerState(boolean state){
        mTopContainer.setVisibility(state?View.VISIBLE:View.GONE);
    }

    private void setBottomContainerState(boolean state){
        mBottomContainer.setVisibility(state?View.VISIBLE:View.GONE);
    }

    private void setControllerState(boolean state){
        setBottomContainerState(state);
        if(state){
            sendDelayHiddenMessage();
        }else{
            removeDelayHiddenMessage();
        }
    }

    private boolean isControllerShow(){
        return mBottomContainer.getVisibility()==View.VISIBLE;
    }

    private void toggleController(){
        if(isControllerShow()){
            setControllerState(false);
        }else{
            setControllerState(true);
        }
    }

    private void sendDelayHiddenMessage(){
        removeDelayHiddenMessage();
        mHandler.sendEmptyMessageDelayed(MSG_CODE_DELAY_HIDDEN_CONTROLLER, 5000);
    }

    private void removeDelayHiddenMessage(){
        mHandler.removeMessages(MSG_CODE_DELAY_HIDDEN_CONTROLLER);
    }

    @Override
    protected void onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow();
        getGroupValue().registerOnGroupValueUpdateListener(mOnGroupValueUpdateListener);
        setControllerState(false);
        getView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mWidth = getView().getWidth();
                mHeight = getView().getHeight();
                getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    protected void onCoverDetachedToWindow() {
        super.onCoverDetachedToWindow();
        getGroupValue().unregisterOnGroupValueUpdateListener(mOnGroupValueUpdateListener);
        removeDelayHiddenMessage();
        mHandler.removeCallbacks(mSeekEventRunnable);
    }

    private void setCurrTime(int curr){
        mCurrTime.setText(TimeUtil.getTimeSmartFormat(curr));
    }

    private void setTotalTime(int duration){
        mTotalTime.setText(TimeUtil.getTimeSmartFormat(duration));
    }

    private void setSeekProgress(int curr, int duration){
        mSeekBar.setMax(duration);
        mSeekBar.setProgress(curr);
        float secondProgress = mBufferPercentage * 1.0f/100 * duration;
        setSecondProgress((int) secondProgress);
    }

    private void setSecondProgress(int secondProgress){
        mSeekBar.setSecondaryProgress(secondProgress);
    }

    @Override
    public void onTimerUpdate(int curr, int duration) {
        if(!mTimerUpdateProgressEnable)
            return;
        mCurrentPosition = curr;
        mDuration = duration;
        updateUI(curr, duration);
    }

    private void updateUI(int curr, int duration) {
        setSeekProgress(curr, duration);
        setCurrTime(curr);
        setTotalTime(duration);
    }

    @Override
    public void onPlayerEvent(int eventCode, Bundle bundle) {
        switch (eventCode){
            case OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_UPDATE:
                if(bundle!=null){
                    mBufferPercentage = bundle.getInt(EventKey.INT_DATA);
                }
                break;
            case OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE:
                mTimerUpdateProgressEnable = true;
                break;
        }
    }

    @Override
    public void onErrorEvent(int eventCode, Bundle bundle) {

    }

    @Override
    public void onReceiverEvent(int eventCode, Bundle bundle) {

    }

    @Override
    public void onPrivateEvent(int eventCode, Bundle bundle) {

    }

    @Override
    public View onCreateCoverView(Context context) {
        return View.inflate(context, R.layout.layout_controller_cover, null);
    }

    @Override
    public int getCoverLevel() {
        return ICover.COVER_LEVEL_LOW;
    }

    @Override
    public void onSingleTapUp(MotionEvent event) {
        if(!mGestureEnable)
            return;
        toggleController();
    }

    @Override
    public void onDoubleTap(MotionEvent event) {

    }

    @Override
    public void onDown(MotionEvent event) {
        mHorizontalSlide = false;
        firstTouch = true;
    }

    @Override
    public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if(!mGestureEnable)
            return;
        float mOldX = e1.getX(), mOldY = e1.getY();
        float deltaY = mOldY - e2.getY();
        float deltaX = mOldX - e2.getX();
        if (firstTouch) {
            horizontalSlide = Math.abs(distanceX) >= Math.abs(distanceY);
            rightVerticalSlide = mOldX > mWidth * 0.5f;
            firstTouch = false;
        }

        if(horizontalSlide){
            onHorizontalSlide(-deltaX / mWidth);
        }else{
            if(Math.abs(deltaY) > mHeight)
                return;
            if(rightVerticalSlide){
                onRightVerticalSlide(deltaY / mHeight);
            }else{
                onLeftVerticalSlide(deltaY / mHeight);
            }
        }
    }

    private void onHorizontalSlide(float percent){
        if(mDuration <= 0)
            return;
        mHorizontalSlide = true;
        mTimerUpdateProgressEnable = false;
        long position = mCurrentPosition;
        long duration = mDuration;
        long deltaMax = Math.min(mDuration/2, duration - position);
        long delta = (long) (deltaMax * percent);
        newPosition = delta + position;
        if (newPosition > duration) {
            newPosition = duration;
        } else if (newPosition <= 0) {
            newPosition = 0;
            delta=-position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0) {
            updateUI((int) newPosition, mDuration);
            setFastForwardState(true);
            String text = showDelta > 0 ? ("+" + showDelta) : "" + showDelta;
            setFastForwardStepTime(text + "s");
            String progressText = TimeUtil.getTimeSmartFormat(newPosition)+"/" + TimeUtil.getTimeSmartFormat(duration);
            setFastForwardProgressTime(progressText);
        }
    }

    private void onRightVerticalSlide(float percent){
        mHorizontalSlide = false;
    }

    private void onLeftVerticalSlide(float percent){
        mHorizontalSlide = false;
    }

    @Override
    public void onEndGesture() {
        setFastForwardState(false);
        if(newPosition >= 0 && mHorizontalSlide){
            sendSeekEvent((int) newPosition);
            newPosition = 0;
        }else{
            mTimerUpdateProgressEnable = true;
        }
        mHorizontalSlide = false;
    }
}
