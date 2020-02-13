package xyz.anythings.dps.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.service.impl.AbstractJobStatusService;
import xyz.anythings.dps.model.DpsSinglePackInform;
import xyz.anythings.dps.query.store.DpsPickQueryStore;
import xyz.anythings.dps.service.api.IDpsJobStatusService;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.util.ValueUtil;

/**
 * 작업 현황 관련 조회 서비스
 * 
 * @author shortstop
 */
@Component("dpsJobStatusService")
public class DpsJobStatusService extends AbstractJobStatusService implements IDpsJobStatusService {

	/**
	 * DPS 피킹 쿼리 스토어 
	 */
	@Autowired
	protected DpsPickQueryStore pickQueryStore;
	
	@Override
	public BatchProgressRate getBatchProgressSummary(JobBatch batch) {
		String sql = this.batchQueryStore.getRackBatchProgressRateQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType", batch.getDomainId(), batch.getId(), batch.getEquipType());
		
		// 배치에 호기가 지정되어 있으면 지정 된 호기에 대한 진행율 
		if(ValueUtil.isNotEmpty(batch.getEquipCd())) {
			params.put("equipCd", batch.getEquipCd());
		}
		
		return AnyEntityUtil.findItem(batch.getDomainId(), false, BatchProgressRate.class, sql, params);
	}
	
	/**
	 * 키오스크 작업 투입 리스트 
	 */
	@Override
	public Page<JobInput> paginateInputList(JobBatch batch, String equipCd, String status, int page, int limit) {
		
		Map<String, Object> params = ValueUtil.newMap("domainId,equipType,batchId", batch.getDomainId(), batch.getEquipType(), batch.getId());
		String sql = this.batchQueryStore.getRackDpsBatchInputListQuery();
		
		if(ValueUtil.isNotEmpty(equipCd)) {
			params.put("equipCd", equipCd);
		}
		
		return this.queryManager.selectPageBySql(sql, params, JobInput.class, page, limit);
	}

	/**
	 * 태블릿 작업 화면 탭 리스트 
	 */
	@Override
	public List<JobInput> searchInputList(JobBatch batch, String equipCd, String stationCd, String selectedInputId) {
		
		Map<String, Object> params = ValueUtil.newMap("domainId,equipType,equipCd,equipZone,batchId"
									, batch.getDomainId(), batch.getEquipType(), equipCd, stationCd, batch.getId());
		
		String qry = this.batchQueryStore.getRackDpsBatchBoxInputTabsQuery();
		
		if(ValueUtil.isNotEmpty(selectedInputId)) {
			// 태블릿 작업 화면에 나올 하단 박스 리스트 (투입 정보 리스트) 중에 기준이 될 박스 투입 ID
			params.put("selectedInputId", selectedInputId);
		}
		
		return AnyEntityUtil.searchItems(batch.getDomainId(), false, JobInput.class, qry, params);
	}

	/**
	 * 태블릿 작업 화면 탭 상세 리스트 
	 */
	@Override
	public List<JobInstance> searchInputJobList(JobBatch batch, JobInput input, String stationCd) {
//		String detailListQry = this.batchQueryStore.getRackDpsBatchBoxInputTabDetailQuery();
//		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType,equipCd,orderNo,stationCd,stageCd"
//									, batch.getDomainId(),batch.getId(),batch.getEquipType(),input.getEquipCd()
//									, input.getOrderNo(),stationCd,batch.getStageCd());
//		
//		return AnyEntityUtil.searchItems(batch.getDomainId(), false, JobInstance.class, detailListQry, params);
		 
		String sql = this.pickQueryStore.getSearchPickingJobListQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,stageCd,equipType,stationCd,orderNo,boxId,statuses", batch.getDomainId(), batch.getId(), batch.getStageCd(), batch.getEquipType(), stationCd, input.getOrderNo(), input.getBoxId(), LogisConstants.JOB_STATUS_IPC);
		return this.queryManager.selectListBySql(sql, params, JobInstance.class, 0, 0);
	}

	@Override
	public List<JobInstance> searchPickingJobList(JobBatch batch, String stationCd) {
		
		// 1. 먼저 해당 작업 스테이션에서 진행 중인 투입 정보를 먼저 조회
		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId());
		condition.addFilter("batchId", batch.getId());
		condition.addFilter("stationCd", stationCd);
		condition.addFilter("status", JobInput.INPUT_STATUS_RUNNING);
		JobInput input = this.queryManager.selectByCondition(JobInput.class, condition);
		
		// 2. 투입 정보가 있다면 투입 정보로 피킹할 작업을 조회 
		if(input != null) {
			return this.searchInputJobList(batch, input, stationCd);
		// 3. 투입 정보가 없다면 에러
		} else {
			throw new ElidomRuntimeException("작업 스테이션 [" + stationCd + "]에서 현재 진행 중인 투입 정보가 없어서 피킹 작업을 찾을 수 없습니다.");
		}
	}
	
	@Override
	public List<DpsSinglePackInform> searchSinglePackInfo(JobBatch batch, String skuCd, String boxType, Integer jobPcs) {
		
		String singlePackInformQry = this.batchQueryStore.getRackDpsSinglePackInformQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,skuCd", batch.getDomainId(), batch.getId(), skuCd);
		
		if(ValueUtil.isNotEmpty(jobPcs)) {
			params.put("jobBoxType", boxType);
			params.put("jobPcs", jobPcs);
		}
		
		return AnyEntityUtil.searchItems(batch.getDomainId(), false, DpsSinglePackInform.class, singlePackInformQry, params);
	}

}
