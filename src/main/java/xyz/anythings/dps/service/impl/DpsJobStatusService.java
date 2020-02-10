package xyz.anythings.dps.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.query.store.BatchQueryStore;
import xyz.anythings.base.service.impl.AbstractJobStatusService;
import xyz.anythings.dps.model.DpsSinglePackInform;
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

	@Autowired
	protected BatchQueryStore batchQueryStore;
	
	@Override
	public BatchProgressRate getBatchProgressSummary(JobBatch batch) {
		String qry = this.batchQueryStore.getRackBatchProgressRateQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType", batch.getDomainId(), batch.getId(), batch.getEquipType());
		
		// 배치에 호기가 지정되어 있으면 지정 된 호기에 대한 진행율 
		if(ValueUtil.isNotEmpty(batch.getEquipCd())) {
			params.put("equipCd", batch.getEquipCd());
		}
		
		return AnyEntityUtil.findItem(batch.getDomainId(), false, BatchProgressRate.class, qry, params);
	}

	@Override
	public JobInput findLatestInput(JobBatch batch) {
		String qry = this.batchQueryStore.getLatestJobInputQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType", batch.getDomainId(), batch.getId(), batch.getEquipType());
		
		if(ValueUtil.isNotEmpty(batch.getEquipCd())) {
			params.put("equipCd", batch.getEquipCd());
		}
		
		return AnyEntityUtil.findItem(batch.getDomainId(), false, JobInput.class, qry, params);  
	}

	@Override
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public Integer findNextInputSeq(JobBatch batch) {
		// 작업 배치의 마지막 투입 시퀀스를 조회 후 하나 올려서 리턴
		JobBatch findBatch = AnyEntityUtil.findEntityByIdWithLock(true, JobBatch.class, batch.getId());
		int lastInputSeq = (findBatch.getLastInputSeq() == null) ? 1 : findBatch.getLastInputSeq() + 1;
		batch.setLastInputSeq(lastInputSeq);
		this.queryManager.update(batch, "lastInputSeq");
		return lastInputSeq;
	}
	
	/**
	 * 키오스크 작업 투입 리스트 
	 */
	@Override
	public Page<JobInput> paginateInputList(JobBatch batch, String equipCd, String status, int page, int limit) {
		// TODO : Status 는 어떻게??? 
		
		Map<String, Object> params = ValueUtil.newMap("domainId,equipType,batchId", batch.getDomainId(), batch.getEquipType(), batch.getId());
		String qry = this.batchQueryStore.getRackDpsBatchInputListQuery();
		
		if(ValueUtil.isNotEmpty(equipCd)) {
			params.put("equipCd", equipCd);
		}
		
		return this.queryManager.selectPageBySql(qry, params, JobInput.class, page, limit);
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
			params.put("selectedInputId", selectedInputId); //기준이 될 Bucket Input  ( ex) 박스 도착 후 조회 되는 리스트 )
		}
		
		return AnyEntityUtil.searchItems(batch.getDomainId(), false, JobInput.class, qry, params);
	}

	/**
	 * 태블릿 작업 화면 탭 상세 리스트 
	 */
	@Override
	public List<JobInstance> searchInputJobList(JobBatch batch, JobInput input, String stationCd) {
		// JobInstance 조회
		// - TODO : side, gwPath 정보 추가 
		String detailListQry = this.batchQueryStore.getRackDpsBatchBoxInputTabDetailQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType,equipCd,orderNo,equipZone,stageCd"
									, batch.getDomainId(),batch.getId(),batch.getEquipType(),input.getEquipCd()
									, input.getOrderNo(),stationCd,batch.getStageCd());
		
		return AnyEntityUtil.searchItems(batch.getDomainId(), false, JobInstance.class, detailListQry, params);
	}

	@Override
	public List<JobInstance> searchPickingJobList(JobBatch batch, String stationCd) {
		// TODO Auto-generated method stub
		return null;
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
