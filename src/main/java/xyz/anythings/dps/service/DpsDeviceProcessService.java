package xyz.anythings.dps.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.LogisCodeConstants;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.event.rest.DeviceProcessRestEvent;
import xyz.anythings.base.model.BaseResponse;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.model.EquipBatchSet;
import xyz.anythings.base.query.store.BatchQueryStore;
import xyz.anythings.base.rest.DeviceProcessController;
import xyz.anythings.base.util.LogisEntityUtil;
import xyz.anythings.dps.DpsCodeConstants;
import xyz.anythings.dps.model.DpsBatchSummary;
import xyz.anythings.dps.service.util.DpsServiceUtil;
import xyz.anythings.sys.service.AbstractExecutionService;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * dps 디바이스 이벤트 처리 서비스 
 * @author yang
 *
 */
@Component
public class DpsDeviceProcessService extends AbstractExecutionService{
	
	@Autowired 
	DpsPickingService dpsPickingService;
	
	
	@Autowired
	BatchQueryStore batchQueryStore;
	
	/**
	 * DPS 배치 작업 진행 율 조회 
	 * 진행율 + 투입 순서리스트 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/batch_summary','dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void batchSummaryEventProcess(DeviceProcessRestEvent event) {
		// 1. 파라미터 
		String equipType = event.getRequestParams().get("equipType").toString();
		String equipCd = event.getRequestParams().get("equipCd").toString();
		int limit = ValueUtil.toInteger(event.getRequestParams().get("limit"));
		int page = ValueUtil.toInteger(event.getRequestParams().get("page"));
		
		
		Long domainId = Domain.currentDomainId();
		
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 2. 배치 서머리 조회 
		DpsBatchSummary summary = this.getBatchSummary(batch,equipType,equipCd,limit,page);

		// 3. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true,"", summary));
		event.setExecuted(true);
	}
	
	
	/**
	 * DPS 버킷 투입
	 * BOX or Tray
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/input_bucket','dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void inputBucket(DeviceProcessRestEvent event) {
		// 1. 파라미터 
		String equipType = event.getRequestParams().get("equipType").toString();
		String equipCd = event.getRequestParams().get("equipCd").toString();
		String bucketCd = event.getRequestParams().get("bucketCd").toString();
		String inputType = event.getRequestParams().get("inputType").toString();
		
		int limit = ValueUtil.toInteger(event.getRequestParams().get("limit"));
		int page = ValueUtil.toInteger(event.getRequestParams().get("page"));

		Long domainId = Domain.currentDomainId();
		
		// 1.equipType / Cd 로 설비 및 배치 조회 
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		boolean isBox = ValueUtil.isEqualIgnoreCase(inputType, LogisCodeConstants.CLASSIFICATION_INPUT_TYPE_BOX) ? true : false;
		
		// 2. 버킷 투입 ( 박스 or 트레이 )
		this.dpsPickingService.inputEmptyBucket(domainId, batch, isBox, bucketCd);
		
		// 3. 배치 서머리 조회 
		DpsBatchSummary summary = this.getBatchSummary(batch,equipType,equipCd,limit,page);

		// 4. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true,"", summary));
		event.setExecuted(true);
	}
	

	/**
	 * DPS 작업 존 버킷 도착 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/bucket_arrive','dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void bucketArrive(DeviceProcessRestEvent event) {
		// 1. 파라미터 
		String equipType = event.getRequestParams().get("equipType").toString();
		String equipCd = event.getRequestParams().get("equipCd").toString();
		String equipZone = event.getRequestParams().get("equipZone").toString();
		String bucketCd = event.getRequestParams().get("bucketCd").toString();
		
		Long domainId = Domain.currentDomainId();
		
		// 2. Batch, Equip 정보 조회 
		EquipBatchSet equipBatch = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatch.getBatch();
		
		// 3.JobInput 조회 및 상태 변경 
		// 3.1 대기 상태인 인풋 정보 조회 
		JobInput input = LogisEntityUtil.findEntityBy(domainId, false, JobInput.class, null, "batchId,equipType,equipCd,stationCd,boxId,status", batch.getId(), equipType,equipCd,equipZone,bucketCd,DpsCodeConstants.JOB_INPUT_STATUS_WAIT);
		
		// 3.2 없으면 진행중 상태에서 조회 
		if(ValueUtil.isEmpty(input)) {
			// 재고 부족으로 다시 작업 존에 박스 도착 했을 경우 ????
			// 없으면 error 
			input = LogisEntityUtil.findEntityBy(domainId, true, JobInput.class, null, "batchId,equipType,equipCd,stationCd,boxId,status", batch.getId(), equipType,equipCd,equipZone,bucketCd,DpsCodeConstants.JOB_INPUT_STATUS_RUN);
		}
		
		// 3.3 상태 update (WAIT = > RUNNING )
		if(ValueUtil.isEqualIgnoreCase(input.getStatus(), DpsCodeConstants.JOB_INPUT_STATUS_WAIT)) {
			input.setStatus(DpsCodeConstants.JOB_INPUT_STATUS_RUN);
			this.queryManager.update(input, "status");
		}
		
		// 4. 도착 한 버킷을 기준으로 input List 조회  
		List<JobInput> tabList = BeanUtil.get(DeviceProcessController.class).searchInputList(equipType,equipCd,equipZone,input.getId());
		
		
		// 5. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true,"", tabList));
		event.setExecuted(true);
	}
	
	/**
	 * B2C 배치에 대한 진행율 을 조회 한다. 
	 * @param equipType
	 * @param equipCd
	 * @param limit
	 * @param page
	 * @return
	 */
	private DpsBatchSummary getBatchSummary(JobBatch batch, String equipType, String equipCd, int limit, int page) {
		// 1. 작업 진행율 조회  
		BatchProgressRate rate = BeanUtil.get(DeviceProcessController.class).batchProgressRate(equipType, equipCd);
		
		// 2. Input List 조회 
		Page<JobInput> inputItems = BeanUtil.get(DeviceProcessController.class).searchInputList(equipType, equipCd, page, limit, null);
		
		// 3. parameter
		Map<String,Object> params = ValueUtil.newMap("domainId,batchId,equipType",Domain.currentDomainId(), batch.getId(),equipType);
		
		if(ValueUtil.isNotEmpty(batch.getEquipCd())) {
			params.put("equipCd", equipCd);
		}
		
		// 4. 투입 가능 박스 수량 조회 
		String inputableBoxQuery = this.batchQueryStore.getRackDpsBatchInputableBoxQuery();
		Integer inputableBox = this.queryManager.selectBySql(inputableBoxQuery, params, Integer.class);
		
		return new DpsBatchSummary(rate,inputItems,inputableBox);
	}
}
