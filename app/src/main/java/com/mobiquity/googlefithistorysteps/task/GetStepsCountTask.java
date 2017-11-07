package com.mobiquity.googlefithistorysteps.task;

import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.mobiquity.googlefithistorysteps.interfaces.StepUpdateListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by hirenpatel on 26/10/17.
 * This Asynctask will get number of steps between two times (Start and End) for user and update UI
 * Integer as Result: Total count for steps
 */

public class GetStepsCountTask extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = GetStepsCountTask.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private int totalSteps;
    private long startTime, endTime;
    private StepUpdateListener stepUpdateListener;

    public GetStepsCountTask(GoogleApiClient client, long startTime, long endTime, StepUpdateListener listener) {
        this.mGoogleApiClient = client;
        this.startTime = startTime;
        this.endTime = endTime;
        this.stepUpdateListener = listener;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        return getStepsCountFromFIT(startTime, endTime);
    }

    @Override
    protected void onPostExecute(Integer steps) {
        super.onPostExecute(steps);
        // Store #endTime in shared preferene for next time use.
        stepUpdateListener.onStepUpdate(steps, startTime, endTime);

    }

    /**
     * Get steps walked by user between two times
     * @param startDateMillis - Start time
     * @param endDateMillis - End time
     * @return - Total steps count walked by user
     */
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
                }
            }
        }
        //Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Get DataSet");
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                showDataSet(dataSet);
            }
        } else {
            Log.i(TAG, "No history found for this user");
        }
        return totalSteps;
    }

    /**
     * Make total steps
     * @param step - Single step found from field of DataPoint
     */
    private void addSteps(int step) {
        Log.e(TAG, step + "");
        totalSteps += step;
    }

    /**
     * Get Field from DataPoint and DataPoint from DataSource
     * @param dataSet
     */
    private void showDataSet(DataSet dataSet) {
        int steps = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {
            for (Field field : dp.getDataType().getFields()) {
                steps = dp.getValue(field).asInt();
                addSteps(steps);
            }
        }
    }
}