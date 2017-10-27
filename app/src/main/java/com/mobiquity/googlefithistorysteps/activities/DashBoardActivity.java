package com.mobiquity.googlefithistorysteps.activities;

import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;
import com.mobiquity.googlefithistorysteps.R;
import com.mobiquity.googlefithistorysteps.di.manager.SharedPreferenceManager;
import com.mobiquity.googlefithistorysteps.interfaces.StepUpdateListener;
import com.mobiquity.googlefithistorysteps.task.GetStepsCountTask;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;

public class DashBoardActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, StepUpdateListener {

    private static final String TAG = DashBoardActivity.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_OAUTH = 1;
    @BindView(R.id.tvSteps)
    TextView txtSteps;
    @BindView(R.id.txtFrom)
    TextView txtFrom;
    @BindView(R.id.txtTo)
    TextView txtTo;
    @BindView(R.id.progressBarSteps)
    ProgressBar pbSteps;
    @BindView(R.id.progressBarFrom)
    ProgressBar pbFrom;
    @BindView(R.id.progressBarTo)
    ProgressBar pbTo;

    @Override
    protected int getViewId() {
        return R.layout.activity_dashboard;
    }

    @Override
    protected void activityOnCreate(@Nullable Bundle savedInstanceState) {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.RECORDING_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Google client: onConnected");
        recordStepsActivity();
        getStepsCount();
    }

    private void getStepsCount() {
        // If no last time stamp previously store in shared preference, means application opened first time so need to get last 30 days totalSteps count from Google FIT.
        // If last time stamp previously store in shared preference, means application opened next time so need to get totalSteps count from Google FIT from last time stamp to till now.
        if (sharedPreferenceManager.getLong(SharedPreferenceManager.KEY_LAST_TIME_STAMP) == 0) {
            Calendar cal = Calendar.getInstance();
            long endTime = cal.getTimeInMillis();

            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            cal.add(Calendar.MONTH, -1);
            long startTime = cal.getTimeInMillis();
            new GetStepsCountTask(mGoogleApiClient, startTime, endTime, this).execute();
        } else {
            long startTime = sharedPreferenceManager.getLong(SharedPreferenceManager.KEY_LAST_TIME_STAMP);
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            long endTime = cal.getTimeInMillis();
            new GetStepsCountTask(mGoogleApiClient, startTime, endTime, this).execute();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Google client: onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        try {
            connectionResult.startResolutionForResult(DashBoardActivity.this, REQUEST_OAUTH);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            if (resultCode == RESULT_OK) {
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.i(TAG, "Google client: RESULT_CANCELED");

            }
        }
    }

    private void recordStepsActivity() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.RecordingApi.subscribe(mGoogleApiClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }
                        } else {
                            Log.w(TAG, "There was a problem subscribing.");
                        }
                    }
                });
    }

    @Override
    public void onStepUpdate(int steps, long startTime, long endTime) {
        sharedPreferenceManager.storeLong(SharedPreferenceManager.KEY_LAST_TIME_STAMP, endTime);
        txtSteps.setVisibility(View.VISIBLE);
        txtSteps.setText(String.valueOf(steps));
        pbSteps.setVisibility(View.GONE);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        txtFrom.setVisibility(View.VISIBLE);
        txtFrom.setText(dateFormat.format(startTime));
        pbFrom.setVisibility(View.GONE);
        txtTo.setVisibility(View.VISIBLE);
        txtTo.setText(dateFormat.format(endTime));
        pbTo.setVisibility(View.GONE);
    }
}