/* 
 * Copyright 2016 OpenMarket Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import im.vector.R;
import im.vector.activity.VectorCallViewActivity;
import im.vector.util.CallUtilities;
import im.vector.util.VectorUtils;

import org.matrix.androidsdk.call.IMXCall;
import org.matrix.androidsdk.data.Room;

/**
 * This class displays the pending call information.
 */
public class VectorPendingCallView extends RelativeLayout {

    /**
     * The current managed call
     */
    private IMXCall mCall;

    /**
     * The UI handler.
     */
    private Handler mUIHandler;

    // the UI items
    private TextView mCallDescriptionTextView;
    private TextView mCallStatusTextView;

    /** set to true to hide the line displaying the call status **/
    private boolean mIsCallStatusHidden;

    // the call listener
    private final IMXCall.MXCallListener mCallListener = new IMXCall.MXCallListener() {
        @Override
        public void onStateDidChange(String state) {
            refresh();
        }

        @Override
        public void onCallError(String error) {
            refresh();
        }

        @Override
        public void onViewLoading(View callView) {
            refresh();
        }

        @Override
        public void onViewReady() {
        }

        @Override
        public void onCallAnsweredElsewhere() {
            onCallTerminated();
        }

        @Override
        public void onCallEnd(final int aReasonId) {
            onCallTerminated();
        }
    };


    /**
     * constructors
     **/
    public VectorPendingCallView(Context context) {
        super(context);
        initView();
    }

    public VectorPendingCallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public VectorPendingCallView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    /**
     * Common initialisation method.
     */
    private void initView() {
        View.inflate(getContext(), R.layout.vector_pending_call_view, this);

        // retrieve the UI items
        mCallDescriptionTextView = (TextView) findViewById(R.id.pending_call_room_name_textview);
        mCallDescriptionTextView.setVisibility(View.GONE);

        mCallStatusTextView = (TextView) findViewById(R.id.pending_call_status_textview);
        mCallStatusTextView.setVisibility(View.GONE);

        // UI handler
        mUIHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Check if there is a pending call.
     * If there is a pending call, this view is visible.
     * If there is none, this view is gone.
     */
    public void checkPendingCall() {
        IMXCall call = VectorCallViewActivity.getActiveCall();

        // no more call
        if (null == call) {

            // unregister the listener
            if (null != mCall) {
                mCall.removeListener(mCallListener);
            }
            mCall = null;

            // hide the view
            setVisibility(View.GONE);
        } else {
            // check if is a new call
            if (mCall != call) {
                // remove any pending listener
                if (null != mCall) {
                    mCall.removeListener(mCallListener);
                }

                // replace the previous one
                mCall = call;

                // listener
                call.addListener(mCallListener);

                // display it
                setVisibility(View.VISIBLE);
            }

            refresh();
        }
    }

    /**
     * Refresh the call information.
     */
    private void refresh() {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null != mCall) {
                    refreshCallDescription();
                    refreshCallStatus();

                    mUIHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refresh();
                        }
                    }, 1000);
                }
            }
        });
    }

    /**
     * The call is ended.
     * Terminates the refresh processes.
     */
    private void onCallTerminated() {
        mCall = null;
        setVisibility(View.GONE);
    }

    /**
     * refresh the call description
     */
    private void refreshCallDescription() {
        if (null != mCall) {
            mCallDescriptionTextView.setVisibility(View.VISIBLE);

            Room room = mCall.getRoom();

            String description;

            if (null != room) {
                description = VectorUtils.getRoomDisplayname(getContext(), mCall.getSession(), room);
            } else {
                description = mCall.getCallId();
            }

            if (TextUtils.equals(mCall.getCallState(), IMXCall.CALL_STATE_CONNECTED) && !mIsCallStatusHidden) {
                description += " - " + getResources().getString(R.string.active_call);
            }

            mCallDescriptionTextView.setText(description);
        } else {
            mCallDescriptionTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Refresh the call status
     */
    private void refreshCallStatus() {
        String callStatus = CallUtilities.getCallStatus(getContext(), mCall);

        mCallStatusTextView.setText(callStatus);
        mCallStatusTextView.setVisibility(TextUtils.isEmpty(callStatus) ? View.GONE : View.VISIBLE);
    }


    /**
     * Enable/disable the display of the status active call.
     * @param aIsEnabled true to display the call status, false otherwise
     */
    public void enableCallStatusDisplay(boolean aIsEnabled){
        mIsCallStatusHidden = !aIsEnabled;
    }
}
