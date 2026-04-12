package com.loopers.batch.job.dailymetrics;

import com.loopers.batch.job.dailymetrics.step.DailyMetricsSnapshotTasklet;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = DailyMetricsSnapshotJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class DailyMetricsSnapshotJobConfig {

    public static final String JOB_NAME = "dailyMetricsSnapshotJob";
    private static final String STEP_NAME = "dailyMetricsSnapshotStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final DailyMetricsSnapshotTasklet dailyMetricsSnapshotTasklet;

    @Bean(JOB_NAME)
    public Job dailyMetricsSnapshotJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(dailyMetricsSnapshotStep())
                .listener(jobListener)
                .build();
    }

    @JobScope
    @Bean(STEP_NAME)
    public Step dailyMetricsSnapshotStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(dailyMetricsSnapshotTasklet, transactionManager)
                .listener(stepMonitorListener)
                .build();
    }
}
