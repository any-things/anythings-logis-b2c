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
import xyz.anythings.dps.DpsCodeConstants;
import xyz.anythings.dps.DpsConstants;
import xyz.anythings.dps.model.DpsBatchSummary;
import xyz.anythings.dps.model.DpsSinglePackInform;
import xyz.anythings.dps.model.DpsSinglePackJobInform;
import xyz.anythings.dps.service.api.IDpsJobStatusService;
import xyz.anythings.dps.service.api.IDpsPickingService;
import xyz.anythings.dps.service.util.DpsBatchJobConfigUtil;
import xyz.anythings.dps.service.util.DpsServiceUtil;
import xyz.anythings.sys.model.BaseResponse;
import xyz.anythings.sys.service.AbstractExecutionService;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.DateUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 디바이스 이벤트 처리 서비스 
 * 
 * @author yang
 */
@Component
public class DpsDeviceProcessService extends AbstractExecutionService {
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
	 * DPS 단포 피킹
	 * 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/single_pack/pick', 'dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void singlePackPick(DeviceProcessRestEvent event) {
		// 1. 파라미터 
		String jobId = event.getRequestParams().get("jobId").toString();

		// 2. jobInstance 조회 
		JobInstance job = AnyEntityUtil.findEntityById(true, JobInstance.class, jobId);
		
		// 3. JobBatch 조회 
		JobBatch batch = AnyEntityUtil.findEntityById(true, JobBatch.class, job.getBatchId());
		
		// 4. 피킹 검수 설정 확인
		int resQty = job.getPickQty();
		if(DpsBatchJobConfigUtil.isPickingWithInspectionEnabled(batch)) {
			resQty = 1;
		}
		
		// 5. 확정 처리 
		this.dpsPickingService.confirmPick(batch, job, resQty);
		
		// 6. 결과 재조회 
		job = AnyEntityUtil.findEntityById(true, JobInstance.class, jobId);
		
		// 7. 상품에 대한 단포 작업 정보 조회 
		List<DpsSinglePackInform> singlePackInfo = this.dpsJobStatusService.searchSinglePackInfo(batch, job.getSkuCd(), job.getBoxTypeCd(), job.getPickQty());
		
		// 8. 피킹 상태가 아니면 완료  
		if(!ValueUtil.isEqualIgnoreCase(job.getStatus(), DpsConstants.JOB_STATUS_PICKING)) {
			job = null;
		}
		
		// 9. 이벤트 처리 결과 셋팅
		DpsSinglePackJobInform result = new DpsSinglePackJobInform(singlePackInfo, job);
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, result));
		event.setExecuted(true);
	}
	
	/**
	 * DPS 단포 상품 변경
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
		String bucketCd = params.get("bucketCd").toString();

		// 2. 설비 정보로 부터 JobBatch 추출 
		Long domainId = event.getDomainId();
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 3. 작업 설정의 배치에 사용중인 버킷 타입 가져오기
		String bucketType = DpsBatchJobConfigUtil.getInputBoxType(batch);
		
		// 4. 단포 버킷 투입 서비스 호출
		boolean isBox = ValueUtil.isEqualIgnoreCase(bucketType, DpsCodeConstants.BOX_TYPE_BOX);
		JobInstance job = (JobInstance)BeanUtil.get(DpsPickingService.class).inputSinglePackEmptyBucket(batch, isBox, skuCd, bucketCd);
		
		// 5. 상품에 대한 단포 작업 정보 조회 
		List<DpsSinglePackInform> singlePackInfo = this.dpsJobStatusService.searchSinglePackInfo(batch, skuCd, job.getBoxTypeCd(), job.getPickQty());
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

		// 2. 설비 정보로 부터 JobBatch 추출 
		Long domainId = event.getDomainId();
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 3. 상품에 대한 단포 작업 정보 조회 
		List<DpsSinglePackInform> singlePackInfo = this.dpsJobStatusService.searchSinglePackInfo(batch, skuCd, null, null);
		
		// 4. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, singlePackInfo));
		event.setExecuted(true);
	}
	
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
		Long domainId = event.getDomainId();
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		
		// 3. 배치 서머리 조회 
		DpsBatchSummary summary = this.getBatchSummary(batch, equipType, equipCd, limit, page);

		// 4. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, summary));
		event.setExecuted(true);
	}
	
	/**
	 * DPS 버킷 투입 (BOX or Tray)
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
		String bucketCd = params.get("bucketCd").toString();
		String inputType = params.get("inputType").toString();
		int limit = ValueUtil.toInteger(params.get("limit"));
		int page = ValueUtil.toInteger(params.get("page"));
		
		// 2.equipType / equipCd로 설비 및 배치 조회
		Long domainId = event.getDomainId();
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatchSet.getBatch();
		boolean isBox = ValueUtil.isEqualIgnoreCase(inputType, DpsCodeConstants.CLASSIFICATION_INPUT_TYPE_BOX) ? true : false;
		
		// 3. 버킷 투입 (박스 or 트레이)
		this.dpsPickingService.inputEmptyBucket(batch, isBox, bucketCd);
		
		// 4. 배치 서머리 조회 
		DpsBatchSummary summary = this.getBatchSummary(batch, equipType, equipCd, limit, page);

		// 5. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, summary));
		event.setExecuted(true);
	}
	
	/**
	 * DPS 작업 존 버킷 도착
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
		String equipZone = params.get("equipZone").toString();
		String bucketCd = params.get("bucketCd").toString();
				
		// 2. Batch, Equip 정보 조회
		Long domainId = event.getDomainId();
		EquipBatchSet equipBatch = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		JobBatch batch = equipBatch.getBatch();
		
		// 3.JobInput 조회 및 상태 변경 
		// 3.1 대기 상태인 인풋 정보 조회 
		JobInput input = AnyEntityUtil.findEntityBy(domainId, false, JobInput.class, null, "batchId,equipType,equipCd,stationCd,boxId,status", batch.getId(), equipType, equipCd, equipZone, bucketCd, DpsCodeConstants.JOB_INPUT_STATUS_WAIT);
		
		// 3.2 없으면 진행중 상태에서 조회 
		if(ValueUtil.isEmpty(input)) {
			// TODO 재고 부족으로 다시 작업 존에 박스 도착 했을 경우 ????
			// 없으면 error 
			input = AnyEntityUtil.findEntityBy(domainId, true, JobInput.class, null, "batchId,equipType,equipCd,stationCd,boxId,status", batch.getId(), equipType, equipCd, equipZone, bucketCd, DpsCodeConstants.JOB_INPUT_STATUS_RUN);
		}
		
		// 3.3 상태 update (WAIT = > RUNNING)
		if(ValueUtil.isEqualIgnoreCase(input.getStatus(), DpsCodeConstants.JOB_INPUT_STATUS_WAIT)) {
			input.setStatus(DpsCodeConstants.JOB_INPUT_STATUS_RUN);
			this.queryManager.update(input, DpsConstants.ENTITY_FIELD_STATUS);
		}
		
		// 3.4 jobInstance 조회 
		List<JobInstance> instanceList = AnyEntityUtil.searchEntitiesBy(domainId, false, JobInstance.class, DpsConstants.ENTITY_FIELD_ID, 
											"batchId,orderNo,inputSeq,status"
											, batch.getId(), input.getOrderNo(), input.getInputSeq(), DpsConstants.JOB_STATUS_INPUT);
		
		if(ValueUtil.isNotEmpty(instanceList)) {
			String nowStr = DateUtil.currentTimeStr();
			
			// 3.5 상태 변경 피킹 시작 시간 셋팅 
			for(JobInstance instance : instanceList) {
				instance.setStatus(DpsConstants.JOB_STATUS_PICKING);
				instance.setPickStartedAt(nowStr);
			}
			
			AnyOrmUtil.updateBatch(instanceList, 100, DpsConstants.ENTITY_FIELD_STATUS, "pickStartedAt");
		}
		
		// 5. 도착 한 버킷을 기준으로 input List 조회  
		List<JobInput> tabList = this.dpsJobStatusService.searchInputList(batch, equipCd, equipZone, input.getId());
		
		// 6. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true, LogisConstants.OK_STRING, tabList));
		event.setExecuted(true);
	}
	
	/**
	 * B2C 배치에 대한 진행율 을 조회 한다. 
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
		
		// 2. Input List 조회 
		Page<JobInput> inputItems = this.dpsJobStatusService.paginateInputList(batch, equipCd, null, page, limit);
		
		// 3. parameter
		Long domainId = batch.getDomainId();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType", domainId, batch.getId(), equipType);
		
		if(ValueUtil.isNotEmpty(batch.getEquipCd())) {
			params.put("equipCd", equipCd);
		}
		
		// 4. 투입 가능 박스 수량 조회 
		String inputableBoxQuery = this.batchQueryStore.getRackDpsBatchInputableBoxQuery();
		Integer inputableBox = AnyEntityUtil.findItem(domainId, false, Integer.class, inputableBoxQuery, params);
		
		// 5. 결과 리턴
		return new DpsBatchSummary(rate, inputItems, inputableBox);
	}

}
