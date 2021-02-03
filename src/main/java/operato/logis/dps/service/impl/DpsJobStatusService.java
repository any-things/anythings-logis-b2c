package operato.logis.dps.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import operato.logis.dps.query.store.DpsBatchQueryStore;
import operato.logis.dps.query.store.DpsPickQueryStore;
import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.service.api.IJobStatusService;
import xyz.anythings.base.service.impl.AbstractJobStatusService;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.util.ValueUtil;

/**
 * 작업 현황 관련 조회 서비스
 * 
 * @author shortstop
 */
@Component("dpsJobStatusService")
public class DpsJobStatusService extends AbstractJobStatusService implements IJobStatusService {

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
		
		String sql = this.dpsBatchQueryStore.getBatchProgressRateQuery();
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
	 * 작업 스테이션 범위의 투입 리스트 
	 */
	@Override
	public Page<JobInput> paginateInputList(JobBatch batch, String equipCd, String stationCd, String status, int page, int limit) {
		
		Map<String, Object> params = ValueUtil.newMap("domainId,equipType,batchId,stationCd", batch.getDomainId(), batch.getEquipType(), batch.getId(), stationCd);
		String sql = this.dpsBatchQueryStore.getBatchInputListQuery();
		
		if(ValueUtil.isNotEmpty(equipCd)) {
			params.put("equipCd", equipCd);
		}
		
		return this.queryManager.selectPageBySql(sql, params, JobInput.class, page, limit);
	}

	@Override
	public Page<JobInput> paginateNotInputList(JobBatch batch, String equipCd, String stationCd, int page, int limit) {
		// TODO Auto-generated method stub
		return null;
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
	public JobInstance findPickingJob(Long domainId, String jobInstanceId) {
		String sql = this.dpsPickQueryStore.getSearchPickingJobListQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,id", domainId, jobInstanceId);
		List<JobInstance> jobList = this.queryManager.selectListBySql(sql, params, JobInstance.class, 1, 1);
		return ValueUtil.isEmpty(jobList) ? null : jobList.get(0);
	}
	
	@Override
	public List<JobInstance> searchPickingJobList(JobBatch batch, Map<String, Object> condition) {

		// 표시기 점등을 위해서 다른 테이블의 데이터도 필요해서 쿼리로 조회
		String sql = this.dpsPickQueryStore.getSearchPickingJobListQuery();
		this.addBatchConditions(batch, condition);
		return this.queryManager.selectListBySql(sql, condition, JobInstance.class, 0, 0);
	}

}
