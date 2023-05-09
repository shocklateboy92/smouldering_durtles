/*
 * Copyright The Smouldering Durtles Contributors
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
import static java.util.Objects.requireNonNull;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.smouldering_durtles.wk.jobs.Job;


public class JobRunnerWorker extends Worker {
    public static final String KEY_JOB_CLASS = "com.smouldering_durtles.wk.JOB_CLASS";
    public static final String KEY_JOB_DATA = "com.smouldering_durtles.wk.JOB_DATA";

    public JobRunnerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String jobClassName = requireNonNull(getInputData().getString(KEY_JOB_CLASS), "Job class name is null");
            Class<? extends Job> jobClass = Class.forName(jobClassName).asSubclass(Job.class);
            String jobData = requireNonNull(getInputData().getString(KEY_JOB_DATA), "Job data is null");
            Job job = jobClass.getConstructor(String.class).newInstance(jobData);
            job.run();
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}
