/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.fragments.services;

import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.jobs.Job;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


/**
 * A service for running jobs. Jobs are tasks that have to be
 * pushed to a background task and should only run one at a time,
 * but should be run as soon as reasonably possible. Jobs are not persisted,
 * they will not survice app restarts or failures.
 *
 * <p>
 *     Jobs are mostly about doing database writes in the background,
 *     and to do regular background housekeeping.
 * </p>
 */
public final class JobRunnerService extends Service {
    /**
     * Schedule a job for this service. It goes into a queue of pending jobs,
     * and will be executed as soon as the service has time for it.
     *
     * @param jobClass the class that implements the job being scheduled
     * @param jobData parameters for this job, encoded in a class-specific format
     */
    public static void schedule(final Class<? extends Job> jobClass, final String jobData) {
        safe(() -> {
            Data inputData = new Data.Builder()
                    .putString(JobRunnerWorker.KEY_JOB_CLASS, jobClass.getCanonicalName())
                    .putString(JobRunnerWorker.KEY_JOB_DATA, jobData)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(JobRunnerWorker.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(WkApplication.getInstance()).enqueue(workRequest);
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
