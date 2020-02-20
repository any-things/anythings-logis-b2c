package xyz.anythings.dps.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.event.rest.DeviceProcessRestEvent;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.model.EquipBatchSet;
import xyz.anythings.base.query.store.BatchQueryStore;
import xyz.anythings.base.service.impl.AbstractLogisService;
import xyz.anythings.dps.DpsCodeConstants;
import xyz.anythings.dps.DpsConstants;
import xyz.anythings.dps.model.DpsBatchSummary;
import xyz.anythings.dps.model.DpsSinglePackSummary;
import xyz.anythings.dps.model.DpsSinglePackJobInform;
import xyz.anythings.dps.service.api.IDpsJobStatusService;
import xyz.anythings.dps.service.api.IDpsPickingService;
import xyz.anythings.dps.service.util.DpsBatchJobConfigUtil;
import xyz.anythings.dps.service.util.DpsServiceUtil;
import xyz.anythings.sys.model.BaseResponse;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 모바일 장비에서 요청하는 트랜잭션 이벤트 처리 서비스 
 * 
 * @author yang
 */
@Component
public class DpsDeviceProcessService extends AbstractLogisService {
	/**
	 * DPS 피킹 서비스
	 */
	@Autowired
	private IDpsPickingService dpsPickingService;
	/**
	 * DPS 작업 현황 조회 서비스
	 */
	@Autowired
	private IDpsJobStatusService dpsJobStatusService;
	/**
	 * 배치 쿼리 스토어
	 */
	@Autowired
	private BatchQueryStore batchQueryStore;
	
	/**
	 * DPS 배치 작업 진행율 조회 : 진행율 + 투입 순서 리스트
	 *  
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/batch_summary', 'dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void batchSummaryEventProcess(DeviceProcessRestEvent event) {
		
		// 1. 파라미터
		Map<String, Object> params = event.getRequestParams();
		String equipType = params.get("equipType").toString();
		String equipCd = params.get("equipCd").toString();
		int limit = ValueUtil.toInteger(params.get("limit"));
		int page = ValueUtil.toInteger(params.get("page"));
		
		// 2. 배치 조회
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(event.getDomainId(), equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 3. 배치 서머리 조회 
		DpsBatchSummary summary = this.getBatchSummary(batch, equipType, equipCd, limit, page);

		// 4. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, summary));
		event.setExecuted(true);
	}
	
	/**
	 * B2C 배치에 대한 진행율 조회 
	 * 
	 * @param batch
	 * @param equipType
	 * @param equipCd
	 * @param limit
	 * @param page
	 * @return
	 */
	private DpsBatchSummary getBatchSummary(JobBatch batch, String equipType, String equipCd, int limit, int page) {
		
		// 1. 작업 진행율 조회  
		BatchProgressRate rate = this.dpsJobStatusService.getBatchProgressSummary(batch);
		
		// 2. 투입 정보 리스트 조회 
		Page<JobInput> inputItems = this.dpsJobStatusService.paginateInputList(batch, equipCd, null, page, limit);
		
		// 3. 파라미터
		Long domainId = batch.getDomainId();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType", domainId, batch.getId(), equipType);
		
		if(ValueUtil.isNotEmpty(batch.getEquipCd())) {
			params.put("equipCd", equipCd);
		}
		
		// 4. 투입 가능 박스 수량 조회 
		Integer inputableBox = AnyEntityUtil.findItem(domainId, false, Integer.class, this.batchQueryStore.getRackDpsBatchInputableBoxQuery(), params);
		
		// 5. 결과 리턴
		return new DpsBatchSummary(rate, inputItems, inputableBox);
	}
	
	/**
	 * 주문 상세 정보 조회 
	 *  
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/order_items', 'dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void searchOrderItems(DeviceProcessRestEvent event) {
		
		// 1. 파라미터
		Map<String, Object> params = event.getRequestParams();
		String equipType = params.get("equipType").toString();
		String equipCd = params.get("equipCd").toString();
		String orderNo = params.get("orderNo").toString();
		
		// 2. 배치 조회
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(event.getDomainId(), equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		List<JobInstance> jobList = this.dpsJobStatusService.searchInputJobList(batch, ValueUtil.newMap("orderNo", orderNo));

		// 4. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, jobList));
		event.setExecuted(true);
	}
	
	/**
	 * DPS 박스 투입 (BOX or Tray)
	 * 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/input_bucket', 'dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void inputBucket(DeviceProcessRestEvent event) {
		
		// 1. 파라미터 
		Map<String, Object> params = event.getRequestParams();
		String equipType = params.get("equipType").toString();
		String equipCd = params.get("equipCd").toString();
		String boxId = params.get("bucketCd").toString();
		String inputType = params.get("inputType").toString();
		int limit = ValueUtil.toInteger(params.get("limit"));
		int page = ValueUtil.toInteger(params.get("page"));
		
		// 2. 설비 코드로 현재 진행 중인 작업 배치 및 설비 정보 조회
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(event.getDomainId(), equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		boolean isBox = ValueUtil.isEqualIgnoreCase(inputType, DpsCodeConstants.CLASSIFICATION_INPUT_TYPE_BOX) ? true : false;
		
		// 3. 박스 투입 (박스 or 트레이)
		this.dpsPickingService.inputEmptyBucket(batch, isBox, boxId);
		
		// 4. 배치 서머리 조회 
		DpsBatchSummary summary = this.getBatchSummary(batch, equipType, equipCd, limit, page);

		// 5. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, summary));
		event.setExecuted(true);
	}
	
	/**
	 * DPS 작업 존 박스 도착
	 * 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/bucket_arrive', 'dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void bucketArrive(DeviceProcessRestEvent event) {
		
		// 1. 파라미터 
		Map<String, Object> params = event.getRequestParams();
		String equipType = params.get("equipType").toString();
		String equipCd = params.get("equipCd").toString();
		// TODO equipZone --> stationCd로 변경
		String stationCd = params.get("equipZone").toString();
		String boxId = params.get("bucketCd").toString();
				
		// 2. 설비 코드로 현재 진행 중인 작업 배치 및 설비 정보 조회
		Long domainId = event.getDomainId();
		EquipBatchSet equipBatch = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatch.getBatch();
		
		// 3.1 대기 상태인 투입 정보 조회 
		JobInput input = AnyEntityUtil.findEntityBy(domainId, false, JobInput.class, null, "batchId,equipType,equipCd,stationCd,boxId,status", batch.getId(), equipType, equipCd, stationCd, boxId, DpsCodeConstants.JOB_INPUT_STATUS_WAIT);
		
		// 3.2 없으면 진행 중인 투입 정보 조회 
		if(input == null) {
			input = AnyEntityUtil.findEntityBy(domainId, true, JobInput.class, null, "batchId,equipType,equipCd,stationCd,boxId,status", batch.getId(), equipType, equipCd, stationCd, boxId, DpsCodeConstants.JOB_INPUT_STATUS_RUN);
		}
		
		// 3.3 투입 정보 상태 업데이트 (WAIT => RUNNING)
		if(ValueUtil.isEqualIgnoreCase(input.getStatus(), DpsCodeConstants.JOB_INPUT_STATUS_WAIT)) {
			input.setStatus(DpsCodeConstants.JOB_INPUT_STATUS_RUN);
			this.queryManager.update(input, DpsConstants.ENTITY_FIELD_STATUS, DpsConstants.ENTITY_FIELD_UPDATER_ID, DpsConstants.ENTITY_FIELD_UPDATED_AT);
		}
		
		// 4. 표시기 점등을 위한 작업 데이터 조회
		List<JobInstance> jobList = this.dpsJobStatusService.searchPickingJobList(batch, stationCd, input.getOrderNo());
				
		// 5. 작업 데이터로 표시기 점등 & 작업 데이터 상태 및 피킹 시작 시간 등 업데이트 
		if(ValueUtil.isNotEmpty(jobList)) {
			this.serviceDispatcher.getIndicationService(batch).indicatorsOn(batch, false, jobList);
		}
		
		// 6. 도착한 박스 기준으로 태블릿에 표시할 투입 박스 리스트 조회  
		List<JobInput> inputList = this.dpsJobStatusService.searchInputList(batch, equipCd, stationCd, input.getId());
		
		// 7. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, inputList));
		event.setExecuted(true);
	}
	
	/**
	 * DPS 단포 피킹 처리
	 * 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/single_pack/pick', 'dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void singlePackPick(DeviceProcessRestEvent event) {
		
		// 1. 파라미터 
		String jobId = event.getRequestParams().get("jobId").toString();

		// 2. 작업 데이터 조회 
		JobInstance job = AnyEntityUtil.findEntityById(true, JobInstance.class, jobId);
		
		// 3. 작업 배치 조회 
		JobBatch batch = AnyEntityUtil.findEntityById(true, JobBatch.class, job.getBatchId());
		
		// 4. 피킹 검수 설정 확인
		int resQty = job.getPickQty();
		if(DpsBatchJobConfigUtil.isPickingWithInspectionEnabled(batch)) {
			resQty = 1;
		}
		
		// 5. 확정 처리 
		this.dpsPickingService.confirmPick(batch, job, resQty);
				
		// 6. 상품에 대한 단포 작업 정보 조회 
		List<DpsSinglePackSummary> singlePackInfo = this.dpsJobStatusService.searchSinglePackInfo(batch, job.getSkuCd(), job.getBoxTypeCd(), job.getPickQty());
		
		// 7. 피킹 상태가 아니면 완료  
		if(!ValueUtil.isEqualIgnoreCase(job.getStatus(), DpsConstants.JOB_STATUS_PICKING)) {
			job = null;
		}
		
		// 8. 이벤트 처리 결과 셋팅
		DpsSinglePackJobInform result = new DpsSinglePackJobInform(singlePackInfo, job);
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, result));
		event.setExecuted(true);
	}
	
	/**
	 * DPS 단포 박스 투입
	 * 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/single_pack/box_input', 'dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void singlePackBoxInput(DeviceProcessRestEvent event) {
		
		// 1. 파라미터 
		Map<String, Object> params = event.getRequestParams();
		String equipType = params.get("equipType").toString();
		String equipCd = params.get("equipCd").toString();
		String skuCd = params.get("skuCd").toString();
		String boxId = params.get("bucketCd").toString();

		// 2. 설비 코드로 현재 진행 중인 작업 배치 및 설비 정보 조회
		Long domainId = event.getDomainId();
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 3. 사용 박스 타입 작업 설정에서 조회
		String boxType = DpsBatchJobConfigUtil.getInputBoxType(batch);
		
		// 4. 단포 박스 투입 서비스 호출
		boolean isBox = ValueUtil.isEqualIgnoreCase(boxType, DpsCodeConstants.BOX_TYPE_BOX);
		JobInstance job = (JobInstance)this.dpsPickingService.inputSinglePackEmptyBucket(batch, isBox, skuCd, boxId);
		
		// 5. 상품에 대한 단포 작업 정보 조회 
		List<DpsSinglePackSummary> singlePackInfo = this.dpsJobStatusService.searchSinglePackInfo(batch, skuCd, job.getBoxTypeCd(), job.getPickQty());
		DpsSinglePackJobInform result = new DpsSinglePackJobInform(singlePackInfo, job);
		
		// 6. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, result));
		event.setExecuted(true);
	}
	
	/**
	 * DPS 단포 상품 변경
	 * 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/single_pack/sku_change', 'dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void singlePackSkuChange(DeviceProcessRestEvent event) {
		
		// 1. 파라미터 
		Map<String, Object> params = event.getRequestParams();
		String equipType = params.get("equipType").toString();
		String equipCd = params.get("equipCd").toString();
		String skuCd = params.get("skuCd").toString();

		// 2. 설비 코드로 현재 진행 중인 작업 배치 및 설비 정보 조회 
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(event.getDomainId(), equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 3. 상품에 대한 단포 작업 정보 조회 
		List<DpsSinglePackSummary> singlePackInfo = this.dpsJobStatusService.searchSinglePackInfo(batch, skuCd, null, null);
		
		// 4. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, singlePackInfo));
		event.setExecuted(true);
	}

}
