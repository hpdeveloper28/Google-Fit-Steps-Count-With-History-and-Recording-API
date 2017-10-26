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
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.mobiquity.googlefithistorysteps.R;
import com.mobiquity.googlefithistorysteps.di.manager.SharedPreferenceManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;

public class DashBoardActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = DashBoardActivity.class.getSimpleName();
    private int totalSteps;
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
            new GetStepsCount(startTime, endTime).execute();
        } else {
            long startTime = sharedPreferenceManager.getLong(SharedPreferenceManager.KEY_LAST_TIME_STAMP);
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            long endTime = cal.getTimeInMillis();
            new GetStepsCount(startTime, endTime).execute();
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


    /**
     * This Asynctask will get number of steps for user and update UI
     * Integer as Result: Total count for steps
     */
    private class GetStepsCount extends AsyncTask<Void, Void, Integer> {

        private long startTime, endTime;

        private GetStepsCount(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return getStepsCountFromFIT(startTime, endTime);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            // Store #endTime in shared preferene for next time use.
            sharedPreferenceManager.storeLong(SharedPreferenceManager.KEY_LAST_TIME_STAMP, endTime);
            txtSteps.setVisibility(View.VISIBLE);
            txtSteps.setText(String.valueOf(integer));
            pbSteps.setVisibility(View.GONE);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            txtFrom.setVisibility(View.VISIBLE);
            txtFrom.setText(dateFormat.format(startTime));
            pbFrom.setVisibility(View.GONE);
            txtTo.setVisibility(View.VISIBLE);
            txtTo.setText(dateFormat.format(endTime));
            pbTo.setVisibility(View.GONE);
        }

        private int getStepsCountFromFIT(long startDateMillis, long endDateMillis) {

            DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                    .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                    .setType(DataSource.TYPE_DERIVED)
                    .setStreamName("estimated_steps")
                    .setAppPackageName("com.google.android.gms")
                    .build();

            DataReadRequest readRequest = new DataReadRequest.Builder()
                    .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startDateMillis, endDateMillis, TimeUnit.MILLISECONDS)
                    .build();

            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mGoogleApiClient, readRequest).await(1, TimeUnit.MINUTES);


            //Used for aggregated data
            if (dataReadResult.getBuckets().size() > 0) {
                Log.i(TAG, "Get Buckets");
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        showDataSet(dataSet);
//                        addSteps(steps);
                    }
                }
            }
            //Used for non-aggregated data
            else if (dataReadResult.getDataSets().size() > 0) {
                Log.i(TAG, "Get DataSet");
                for (DataSet dataSet : dataReadResult.getDataSets()) {
                    showDataSet(dataSet);
//                    addSteps(steps);
                }
            } else {
                Log.i(TAG, "No history found for this user");
            }
            return totalSteps;
        }


        private void addSteps(int step) {
            Log.e(TAG, step+"");
            totalSteps += step;
        }

        private void showDataSet(DataSet dataSet) {
            int steps = 0;
            for (DataPoint dp : dataSet.getDataPoints()) {
                for (Field field : dp.getDataType().getFields()) {
                    steps = dp.getValue(field).asInt();
                    addSteps(steps);
                }
            }
//            return steps;
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
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
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
}