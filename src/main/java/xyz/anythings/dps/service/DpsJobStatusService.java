package xyz.anythings.dps.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.service.impl.AbstractJobStatusService;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.util.ValueUtil;

@Component("dpsJobStatusService")
public class DpsJobStatusService extends AbstractJobStatusService {

	/**
	 * 키오스크 작업 투입 리스트 
	 */
	@Override
	public Page<JobInput> paginateInputList(JobBatch batch, String equipCd, String status, int page, int limit) {
		// TODO : Status 는 어떻게??? 
		
		Map<String,Object> params = ValueUtil.newMap("domainId,equipType,batchId", batch.getDomainId(), batch.getEquipType(), batch.getId());
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
		
		Map<String,Object> params = ValueUtil.newMap("domainId,equipType,equipCd,equipZone,batchId"
									, batch.getDomainId(), batch.getEquipType(), equipCd, stationCd, batch.getId());
		
		String qry = this.batchQueryStore.getRackDpsBatchBoxInputTabsQuery();
		
		if(ValueUtil.isNotEmpty(selectedInputId)) {
			params.put("selectedInputId", selectedInputId); //기준이 될 Bucket Input  ( ex) 박스 도착 후 조회 되는 리스트 )
		}

		return this.queryManager.selectListBySql(qry, params, JobInput.class, 0, 0);
	}

	/**
	 * 태블릿 작업 화면 탭 상세 리스트 
	 */
	@Override
	public List<JobInstance> searchInputJobList(JobBatch batch, JobInput input, String stationCd) {
		// JobInstance 조회
		// - TODO : side, gwPath 정보 추가 
		String detailListQry = this.batchQueryStore.getRackDpsBatchBoxInputTabDetailQuery();
		Map<String,Object> params = ValueUtil.newMap("domainId,batchId,equipType,equipCd,orderNo,equipZone,stageCd"
									, batch.getDomainId(),batch.getId(),batch.getEquipType(),input.getEquipCd()
									, input.getOrderNo(),stationCd,batch.getStageCd());
		return this.queryManager.selectListBySql(detailListQry, params, JobInstance.class, 0, 0);
	}
}
