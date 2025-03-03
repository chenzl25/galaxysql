package com.alibaba.polardbx.executor.ddl.newengine.meta;

import com.alibaba.polardbx.common.ddl.newengine.DdlPlanState;
import com.alibaba.polardbx.common.ddl.newengine.DdlType;
import com.alibaba.polardbx.executor.ddl.newengine.utils.DdlHelper;
import com.alibaba.polardbx.gms.scheduler.DdlPlanRecord;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DdlPlanManager {

    public boolean submitNewRebalanceJobIfAssertTrue(long ddlPlanId, Consumer<DdlPlanRecord> consumer) {
        return new DdlPlanAccessorDelegate<Boolean>() {
            @Override
            protected Boolean invoke() {
                DdlPlanRecord ddlPlanRecord = ddlPlanAccessor.queryForUpdate(ddlPlanId);

                //assert
                consumer.accept(ddlPlanRecord);

                long jobId = DdlHelper.getServerConfigManager()
                    .submitRebalanceDDL(ddlPlanRecord.getTableSchema(), ddlPlanRecord.getDdlStmt());
                LOGGER.info(String.format("create DDL JOB:[%s] for ddl_plan:[%s]", jobId, ddlPlanId));
                DdlPlanState ddlPlanState = jobId != -1 ? DdlPlanState.EXECUTING : DdlPlanState.SUCCESS;
                return ddlPlanAccessor.updateState(
                    ddlPlanId,
                    ddlPlanState,
                    ddlPlanRecord.getResult(),
                    jobId
                );
            }
        }.execute();
    }

    public List<DdlPlanRecord> getExecutableDdlPlan(DdlType ddlType){
        return new DdlPlanAccessorDelegate<List<DdlPlanRecord>>(){
            @Override
            protected List<DdlPlanRecord> invoke() {
                List<DdlPlanRecord> ddlPlanRecordList = ddlPlanAccessor.queryByType(ddlType.name());
                if(CollectionUtils.isEmpty(ddlPlanRecordList)){
                    return new ArrayList<>();
                }
                return ddlPlanRecordList;
            }
        }.execute();
    }

    public void incrementRetryCount(long ddlPlanId){
        new DdlPlanAccessorDelegate<Boolean>() {
            @Override
            protected Boolean invoke() {
                return ddlPlanAccessor.incrementRetryCount(ddlPlanId);
            }
        }.execute();
    }

}