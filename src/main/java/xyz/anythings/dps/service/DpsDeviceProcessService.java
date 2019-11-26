package xyz.anythings.dps.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.event.rest.DeviceProcessRestEvent;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.model.EquipBatchSet;
import xyz.anythings.base.query.store.BatchQueryStore;
import xyz.anythings.dps.DpsCodeConstants;
import xyz.anythings.dps.DpsConstants;
import xyz.anythings.dps.model.DpsBatchSummary;
import xyz.anythings.dps.model.DpsSinglePackInform;
import xyz.anythings.dps.model.DpsSinglePackJobInform;
import xyz.anythings.dps.service.util.DpsBatchJobConfigUtil;
import xyz.anythings.dps.service.util.DpsServiceUtil;
import xyz.anythings.sys.model.BaseResponse;
import xyz.anythings.sys.service.AbstractExecutionService;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.DateUtil;
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
	DpsJobStatusService dpsJobStatusService;
	
	@Autowired
	BatchQueryStore batchQueryStore;
	
	/**
	 * DPS 단포 상품 변경 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/single_pack/box_input','dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void singlePackBoxInput(DeviceProcessRestEvent event) {
		// 1. 파라미터 
		String equipType = event.getRequestParams().get("equipType").toString();
		String equipCd = event.getRequestParams().get("equipCd").toString();
		String skuCd = event.getRequestParams().get("skuCd").toString();
		String bucketCd = event.getRequestParams().get("bucketCd").toString();
		Long domainId = Domain.currentDomainId();

		// 2. 설비 정보로 부터 JobBatch 추출 
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 3. 작업 설정의 배치에 사용중인 버킷 타입 가져오기
		String bucketType = DpsBatchJobConfigUtil.getInputBoxType(batch);
		
		// 4. 단포 버킷 투입 서비스 호출
		boolean isBox = ValueUtil.isEqualIgnoreCase(bucketType, DpsCodeConstants.BOX_TYPE_BOX);
		JobInstance job = (JobInstance)BeanUtil.get(DpsPickingService.class).inputSinglePackEmptyBucket(domainId,batch,isBox,skuCd,bucketCd);
		
		// 5. 상품에 대한 단포 작업 정보 조회 
		List<DpsSinglePackInform> singlePackInfo = BeanUtil.get(DpsJobStatusService.class).getSinglePackInform(domainId, batch, skuCd, job.getPickQty(),job.getBoxTypeCd());
		
		DpsSinglePackJobInform result = new DpsSinglePackJobInform(singlePackInfo, job);
		
		// 6. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true,"", result));
		event.setExecuted(true);
	}
	
	
	/**
	 * DPS 단포 상품 변경 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/single_pack/sku_change','dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void singlePackSkuChange(DeviceProcessRestEvent event) {
		// 1. 파라미터 
		String equipType = event.getRequestParams().get("equipType").toString();
		String equipCd = event.getRequestParams().get("equipCd").toString();
		String skuCd = event.getRequestParams().get("skuCd").toString();
		Long domainId = Domain.currentDomainId();

		// 2. 설비 정보로 부터 JobBatch 추출 
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 3. 상품에 대한 단포 작업 정보 조회 
		List<DpsSinglePackInform> singlePackInfo = BeanUtil.get(DpsJobStatusService.class).getSinglePackInform(domainId, batch, skuCd, null,null);
		
		// 4. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true,"", singlePackInfo));
		event.setExecuted(true);
	}
	
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
		
		boolean isBox = ValueUtil.isEqualIgnoreCase(inputType, DpsCodeConstants.CLASSIFICATION_INPUT_TYPE_BOX) ? true : false;
		
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
		JobInput input = AnyEntityUtil.findEntityBy(domainId, false, JobInput.class, null, "batchId,equipType,equipCd,stationCd,boxId,status", batch.getId(), equipType,equipCd,equipZone,bucketCd,DpsCodeConstants.JOB_INPUT_STATUS_WAIT);
		
		// 3.2 없으면 진행중 상태에서 조회 
		if(ValueUtil.isEmpty(input)) {
			// TODO 재고 부족으로 다시 작업 존에 박스 도착 했을 경우 ????
			// 없으면 error 
			input = AnyEntityUtil.findEntityBy(domainId, true, JobInput.class, null, "batchId,equipType,equipCd,stationCd,boxId,status", batch.getId(), equipType,equipCd,equipZone,bucketCd,DpsCodeConstants.JOB_INPUT_STATUS_RUN);
		}
		
		// 3.3 상태 update (WAIT = > RUNNING )
		if(ValueUtil.isEqualIgnoreCase(input.getStatus(), DpsCodeConstants.JOB_INPUT_STATUS_WAIT)) {
			input.setStatus(DpsCodeConstants.JOB_INPUT_STATUS_RUN);
			this.queryManager.update(input, DpsConstants.ENTITY_FIELD_STATUS);
		}
		
		// 3.4 jobInstance 조회 
		List<JobInstance> instanceList 
			= AnyEntityUtil.searchEntitiesBy(domainId, false, JobInstance.class, DpsConstants.ENTITY_FIELD_ID, 
											"batchId,orderNo,inputSeq,status"
											, batch.getId(), input.getOrderNo(), input.getInputSeq(), DpsConstants.JOB_STATUS_INPUT);
		
		if(ValueUtil.isNotEmpty(instanceList)) {
			String nowStr = DateUtil.currentTimeStr();
			
			// 3.5 상태 변경 피킹 시작 시간 셋팅 
			for(JobInstance instance : instanceList) {
				instance.setStatus(DpsConstants.JOB_STATUS_PICKING);
				instance.setPickStartedAt(nowStr);
			}
			AnyOrmUtil.updateBatch(instanceList, 100, DpsConstants.ENTITY_FIELD_STATUS,"pickStartedAt");
		}
		
		// 5. 도착 한 버킷을 기준으로 input List 조회  
		List<JobInput> tabList = this.dpsJobStatusService.searchInputList(batch, equipCd, equipZone, input.getId());
		
		// 6. 이벤트 처리 결과 셋팅 
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
		BatchProgressRate rate = this.dpsJobStatusService.getBatchProgressSummary(batch);
		
		// 2. Input List 조회 
		Page<JobInput> inputItems = this.dpsJobStatusService.paginateInputList(batch, equipCd, null, page, limit);
		
		// 3. parameter
		Map<String,Object> params = ValueUtil.newMap("domainId,batchId,equipType",Domain.currentDomainId(), batch.getId(),equipType);
		
		if(ValueUtil.isNotEmpty(batch.getEquipCd())) {
			params.put("equipCd", equipCd);
		}
		
		// 4. 투입 가능 박스 수량 조회 
		String inputableBoxQuery = this.batchQueryStore.getRackDpsBatchInputableBoxQuery();
		Integer inputableBox = AnyEntityUtil.findItem(batch.getDomainId(), false, Integer.class, inputableBoxQuery, params);
		
		return new DpsBatchSummary(rate,inputItems,inputableBox);
	}
}
