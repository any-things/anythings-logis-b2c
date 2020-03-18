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
import xyz.anythings.dps.model.DpsSinglePackSummary;
import xyz.anythings.dps.query.store.DpsBatchQueryStore;
import xyz.anythings.dps.query.store.DpsPickQueryStore;
import xyz.anythings.dps.service.api.IDpsJobStatusService;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.util.ValueUtil;

/**
 * 작업 현황 관련 조회 서비스
 * 
 * @author shortstop
 */
@Component("dpsJobStatusService")
public class DpsJobStatusService extends AbstractJobStatusService implements IDpsJobStatusService {

	/**
	 * DPS 배치 관련 쿼리 스토어 
	 */
	@Autowired
	protected DpsBatchQueryStore dpsBatchQueryStore;
	/**
	 * DPS 피킹 쿼리 스토어 
	 */
	@Autowired
	protected DpsPickQueryStore dpsPickQueryStore;
	
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
		String sql = this.dpsBatchQueryStore.getBatchInputListQuery();
		
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
		
		String query = this.dpsBatchQueryStore.getBatchBoxInputTabListQuery();
		
		if(ValueUtil.isNotEmpty(selectedInputId)) {
			// 태블릿 작업 화면에 나올 하단 박스 리스트 (투입 정보 리스트) 중에 기준이 될 박스 투입 ID
			params.put("selectedInputId", selectedInputId);
		}
		
		return AnyEntityUtil.searchItems(batch.getDomainId(), false, JobInput.class, query, params);
	}

	/**
	 * 태블릿 작업 화면 탭 상세 리스트 
	 */
	@Override
	public List<JobInstance> searchInputJobList(JobBatch batch, JobInput input, String stationCd) {
		
		String inputJobsSql = this.dpsBatchQueryStore.getBatchBoxInputTabDetailQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType,equipCd,orderNo,stationCd,stageCd"
									, batch.getDomainId(),batch.getId(), batch.getEquipType(), input.getEquipCd()
									, input.getOrderNo(), stationCd, batch.getStageCd());
		return AnyEntityUtil.searchItems(batch.getDomainId(), false, JobInstance.class, inputJobsSql, params);
	}
	
	@Override
	public List<JobInstance> searchInputJobList(JobBatch batch, Map<String, Object> condition) {
		
		// 1. 배치 조건을 검색 조건에 추가
		this.addBatchConditions(batch, condition);
		// 2. 작업 리스트 조회 
		return this.queryManager.selectList(JobInstance.class, condition);		

	}

	@Override
	public List<JobInstance> searchPickingJobList(JobBatch batch, String stationCd, String classCd) {
		
		// 표시기 점등을 위해서 다른 테이블의 데이터도 필요해서 쿼리로 조회 
		String sql = this.dpsPickQueryStore.getSearchPickingJobListQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,stageCd,equipType,stationCd,classCd,statuses", batch.getDomainId(), batch.getId(), batch.getStageCd(), batch.getEquipType(), stationCd, classCd, LogisConstants.JOB_STATUS_WIPC);
		return this.queryManager.selectListBySql(sql, params, JobInstance.class, 0, 0);
	}

	@Override
	public List<JobInstance> searchPickingJobList(JobBatch batch, Map<String, Object> condition) {

		// 표시기 점등을 위해서 다른 테이블의 데이터도 필요해서 쿼리로 조회
		String sql = this.dpsPickQueryStore.getSearchPickingJobListQuery();
		this.addBatchConditions(batch, condition);
		return this.queryManager.selectListBySql(sql, condition, JobInstance.class, 0, 0);
	}
	
	@Override
	public List<DpsSinglePackSummary> searchSinglePackSummary(JobBatch batch, String skuCd, String boxType, Integer jobPcs) {
		
		String singlePackSummaryQuery = this.dpsPickQueryStore.getSinglePackSummaryQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,skuCd", batch.getDomainId(), batch.getId(), skuCd);
		
		if(ValueUtil.isNotEmpty(jobPcs)) {
			params.put("jobBoxType", boxType);
			params.put("jobPcs", jobPcs);
		}
		
		return AnyEntityUtil.searchItems(batch.getDomainId(), false, DpsSinglePackSummary.class, singlePackSummaryQuery, params);
	}

}
