package com.learn.support.quartz.repository;


import com.learn.support.quartz.entity.SchedulerJobInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SchedulerRepository extends JpaRepository<SchedulerJobInfo, UUID> {

	SchedulerJobInfo findByJobName(String jobName);

}
