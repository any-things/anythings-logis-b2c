package operato.logis.dps.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;

import operato.logis.dps.DpsCodeConstants;
import operato.logis.dps.DpsConstants;
import operato.logis.dps.query.store.DpsBatchQueryStore;
import operato.logis.dps.query.store.DpsBoxQueryStore;
import operato.logis.dps.query.store.DpsPickQueryStore;
import operato.logis.dps.service.api.IDpsPickingService;
import operato.logis.dps.service.util.DpsBatchJobConfigUtil;
import operato.logis.dps.service.util.DpsServiceUtil;
import xyz.anythings.base.LogisCodeConstants;
import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.BoxType;
import xyz.anythings.base.entity.Cell;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.Order;
import xyz.anythings.base.entity.TrayBox;
import xyz.anythings.base.entity.ifc.IBucket;
import xyz.anythings.base.event.ICategorizeEvent;
import xyz.anythings.base.event.IClassifyErrorEvent;
import xyz.anythings.base.event.IClassifyOutEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.base.model.Category;
import xyz.anythings.base.service.api.IBoxingService;
import xyz.anythings.base.service.api.IIndicationService;
import xyz.anythings.base.service.impl.AbstractClassificationService;
import xyz.anythings.gw.entity.Gateway;
import xyz.anythings.gw.service.mq.model.device.DeviceCommand;
import xyz.anythings.sys.model.BaseResponse;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 피킹 추상 클래스
 * 
 * @author shortstop
 */
public abstract class AbstractPickingService extends AbstractClassificationService implements IDpsPickingService {
	
	/**
	 * 배치 쿼리 스토어
	 */
	@Autowired
	protected DpsBatchQueryStore batchQueryStore;
	/**
	 * 피킹 쿼리 스토어
	 */
	@Autowired
	protected DpsPickQueryStore pickQueryStore;	
	/**
	 * 박스 쿼리 스토어
	 */
	@Autowired
	protected DpsBoxQueryStore boxQueryStore;
	/**
	 * DPS 작업 상태 서비스 
	 */
	@Autowired
	protected DpsJobStatusService dpsJobStatusService;

	/************************************************************************************************/
	/*											분류 모듈 정보											*/
	/************************************************************************************************/

	/**
	 * 1-1. 분류 모듈 정보 : 분류 서비스 모듈의 작업 유형 (DAS, RTN, DPS) 리턴 
	 * 
	 * @return
	 */
	@Override
	public String getJobType() {
		return DpsConstants.JOB_TYPE_DPS;
	}

	/**
	 * 1-4. 모듈별 박싱 처리 서비스
	 * 
	 * @param params
	 * @return
	 */
	@Override
	public IBoxingService getBoxingService(Object... params) {
		return this.serviceDispatcher.getBoxingService(LogisConstants.JOB_TYPE_DPS);
	}
	
	/************************************************************************************************/
	/*											중분류												*/
	/************************************************************************************************/
	
	/**
	 * 중분류 이벤트
	 *  
	 * @param event
	 * @return
	 */
	@Override
	public Category categorize(ICategorizeEvent event) {
		// DPS 는 중분류 없음 
		return null;
	};
	
	/************************************************************************************************/
	/*											버킷 투입												*/
	/************************************************************************************************/
	
	@Override
	public Object boxCellMapping(JobBatch batch, String cellCd, String boxId) {
		// B2C는 구현하지 않아도 됨
		return null;
	}

	/**
	 * 2-1. 투입 ID로 유효성 체크 및 투입 유형을 찾아서 리턴 
	 * 
	 * @param batch
	 * @param inputId
	 * @param params
	 * @return LogisCodeConstants.INPUT_TYPE_...
	 */
	@Override
	public String checkInput(JobBatch batch, String inputId, Object ... params) {
		// 1. 투입 ID가 상품 코드 인지 체크 
		if(DpsBatchJobConfigUtil.isSkuCdValid(batch, inputId)) {
			return LogisCodeConstants.INPUT_TYPE_SKU_CD;
		}
		
		// 2. 투입 ID가 박스 코드 인지 체크
		if(DpsBatchJobConfigUtil.isBoxIdValid(batch, inputId)) {
			return LogisCodeConstants.INPUT_TYPE_BOX_ID;
		}
		
		// 3. 투입 ID가 셀 코드 인지 체크
		if(DpsBatchJobConfigUtil.isCellCdValid(batch, inputId)) {
			return LogisCodeConstants.INPUT_TYPE_CELL_CD;
		}
		
		// 4. 투입 ID가 표시기 코드 인지 체크
		if(DpsBatchJobConfigUtil.isIndCdValid(batch, inputId)) {
			return LogisCodeConstants.INPUT_TYPE_IND_CD;
		}
		
		// 5. 투입 ID가 표시기 코드 인지 체크
		if(DpsBatchJobConfigUtil.isRackCdValid(batch, inputId)) {
			return LogisCodeConstants.INPUT_TYPE_RACK_CD;
		}
		
		return null;
	};

	/************************************************************************************************/
	/*											소분류												*/
	/************************************************************************************************/

	/**
	 * 3-1. 소분류 : 분류 처리 작업
	 * 
	 * @param exeEvent 분류 처리 이벤트
	 * @return
	 */
	@Override
	public Object classify(IClassifyRunEvent exeEvent) {
		switch (exeEvent.getClassifyAction()) {
			// 확정 처리 
			case DpsCodeConstants.CLASSIFICATION_ACTION_CONFIRM :
				this.confirmPick(exeEvent);
				break;
				
			// 수정 처리 
			case DpsCodeConstants.CLASSIFICATION_ACTION_MODIFY :
				this.splitPick(exeEvent);
				break;
				
			// 취소 처리 
			case DpsCodeConstants.CLASSIFICATION_ACTION_CANCEL :
				this.cancelPick(exeEvent);
				break;
				
			default : 
				return new BaseResponse(false, null);
		}
		
		return new BaseResponse(true, null);
	}
	
	/**
	 * 3-2. 소분류 : 분류 처리 결과 처리 (DAS, DPS, 반품 - 풀 박스 처리 후 호출, 소터 - 단위 상품별 분류 처리 시 I/F로 넘어온 후 호출)
	 * 
	 * @param outputEvent
	 * @return
	 */
	@Override
	public Object output(IClassifyOutEvent outputEvent) {
		return null;
	}

	/**
	 * 3-6. 소분류 : 피킹 확정 처리된 작업 취소
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	public int undoPick(IClassifyRunEvent exeEvent) {
		// DPS에서 피킹 확정 처리된 작업 취소
		return 0;
	}
	
	/**
	 * 3-7. 소분류 : 박스 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	public BoxPack fullBoxing(IClassifyRunEvent exeEvent) {
		JobBatch batch = exeEvent.getJobBatch();
		JobInstance job = exeEvent.getJobInstance();
		List<JobInstance> jobList = this.dpsJobStatusService.searchPickingJobList(batch, null, job.getOrderNo());
		return this.getBoxingService(batch).fullBoxing(batch, null, jobList);
	}

	/**
	 * 3-8. 소분류 : Boxing 취소
	 * 
	 * @param domainId
	 * @param boxPackId
	 * @return
	 */
	@Override
	public BoxPack cancelBoxing(Long domainId, String boxPackId) {
		// DPS에서 박싱 취소 없음
		return null;
	}

	/**
	 * 3-9. 소분류 : 주문별 박스별 피킹 완료 여부 체크
	 * 
	 * @param batch
	 * @param orderNo
	 * @param boxId
	 * @return
	 */
	@Override
	public boolean checkBoxingEnd(JobBatch batch, String orderNo, String boxId) {
		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId());
		condition.addFilter("batchId", batch.getId());
		condition.addFilter("orderNo", orderNo);
		condition.addFilter("boxId", boxId);
		condition.addFilter("status", LogisConstants.IN, LogisConstants.JOB_STATUS_WIP);
		return this.queryManager.selectSize(JobInstance.class, condition) == 0;
	}

	/**
	 * 3-10. 소분류 : 스테이션에 투입된 주문별 피킹 작업 완료 여부 체크
	 * 
	 * @param batch
	 * @param stationCd
	 * @param job
	 * @return
	 */
	@Override
	public boolean checkStationJobsEnd(JobBatch batch, String stationCd, JobInstance job) {
		String sql = "select id from job_instances where domain_id = :domainId and sub_equip_cd in (select cell_cd from cells where domain_id = :domainId and station_cd = :stationCd) and order_no = :orderNo and box_id = :boxId and status in (:statuses)";
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,orderNo,boxId,stationCd,statuses", batch.getDomainId(), batch.getId(), job.getOrderNo(), job.getBoxId(), stationCd, LogisConstants.JOB_STATUS_WIP);
		return this.queryManager.selectSizeBySql(sql, params) == 0;
	}
	
	/************************************************************************************************/
	/*											기타													*/
	/************************************************************************************************/

	/**
	 * 4-1. 기타 : 배치 내 모든 분류 작업이 완료되었는지 여부 
	 * 
	 * @param batch
	 * @return
	 */
	@Override
	public boolean checkEndClassifyAll(JobBatch batch) {
		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId());
		condition.addFilter("batchId", batch.getId());
		condition.addFilter("status", LogisConstants.IN, LogisConstants.JOB_STATUS_WIPF);
		return this.queryManager.selectSize(JobInstance.class, condition) == 0;
	}
	
	/**
	 * 4-2. 기타 : 분류 서비스 모듈별 작업 시작 중 추가 처리
	 * 
	 * @param batch
	 */
	@Override
	public void batchStartAction(JobBatch batch) {
		// 설정에서 작업배치 시에 게이트웨이 리부팅 할 지 여부 조회
		if(DpsBatchJobConfigUtil.isGwRebootWhenInstruction(batch)) {
			IIndicationService indSvc = this.serviceDispatcher.getIndicationService(batch);
			List<Gateway> gwList = indSvc.searchGateways(batch);
			
			// 게이트웨이 리부팅 처리
			for(Gateway gw : gwList) {
				indSvc.rebootGateway(batch, gw);
			}
		}
	}
	
	/**
	 * 4-3. 기타 : 분류 서비스 모듈별 작업 마감 중 추가 처리
	 * 
	 * @param batch
	 */
	@Override
	public void batchCloseAction(JobBatch batch) {
	}
	
	/**
	 * 4-4. 기타 : 분류 작업 처리시 에러 핸들링
	 * 
	 * @param errorEvent
	 */
	@Override
	public void handleClassifyException(IClassifyErrorEvent errorEvent) {
	}
	
	/************************************************************************************************/
	/**											Protected											*/
	/************************************************************************************************/
	
	/************************************************************************************************/
	/*											2. 버킷 투입											*/
	/************************************************************************************************/
	
	/**
	 * 박스 투입 후 액션 
	 * 
	 * @param batch
	 * @param bucket
	 * @param orderNo
	 */
	protected void afterInputEmptyBucket(JobBatch batch, IBucket bucket, String orderNo) {
		// 트레이 박스인 경우 트레이 상태 업데이트 
		if(ValueUtil.isEqualIgnoreCase(DpsCodeConstants.BOX_TYPE_TRAY, bucket.getBucketType())) {
			TrayBox tray = (TrayBox)bucket;
			tray.setStatus(DpsConstants.COMMON_STATUS_INPUT);
			this.queryManager.update(tray, DpsConstants.ENTITY_FIELD_STATUS, DpsConstants.ENTITY_FIELD_UPDATER_ID, DpsConstants.ENTITY_FIELD_UPDATED_AT);
		}
	}
	
	/**
	 * 박스 혹은 트레이를 작업에 투입
	 * 
	 * @param batch
	 * @param classCd
	 * @param bucket
	 * @param indColor
	 * @return
	 */
	protected void doInputEmptyBucket(JobBatch batch, String classCd, IBucket bucket, String indColor) {
		
		// 1. 주문 번호로 투입 정보 조회  
		String newInputsQuery = this.batchQueryStore.getBatchNewInputDataQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType,classCd,orderType", batch.getDomainId(), batch.getId(), batch.getEquipType(), classCd, DpsCodeConstants.DPS_ORDER_TYPE_MT);
		List<JobInput> inputList = AnyEntityUtil.searchItems(batch.getDomainId(), false, JobInput.class, newInputsQuery, params);
		
		// 2. 투입 정보 생성 및 작업 데이터 업데이트 
		this.createInputData(batch, inputList, classCd, bucket, indColor);
	}
	
	/**
	 * 투입 정보 생성 및 작업 데이터 업데이트 
	 * 
	 * @param batch
	 * @param inputList
	 * @param classCd
	 * @param bucket
	 * @param indColor
	 */
	protected void createInputData(JobBatch batch, List<JobInput> inputList, String classCd, IBucket bucket, String indColor) {

		// 1. 주문 - 박스 ID 매핑 쿼리 추출
		String updateJobSql = this.batchQueryStore.getBatchMapBoxIdAndSeqQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType,classCd,userId,boxId,colorCd,inputAt",
				                    batch.getDomainId(), batch.getId(), batch.getEquipType(), classCd, 
				                    User.currentUser().getId(), bucket.getBucketCd(), indColor, DateUtil.currentTimeStr());
		
		for(JobInput input : inputList) {
			// 2. 다음 투입 시퀀스 추출 
			int newSeq = this.dpsJobStatusService.findNextInputSeq(batch);
			
			// 3. 새로운 투입 정보 생성
			input.setComCd(batch.getComCd());
			input.setInputSeq(newSeq);
			input.setBoxId(bucket.getBucketCd());
			input.setBoxType(bucket.getBucketType());
			input.setColorCd(indColor);
			input.setInputType(DpsCodeConstants.JOB_INPUT_TYPE_PCS); 
			input.setStatus(DpsCodeConstants.JOB_INPUT_STATUS_WAIT);
			
			// 4. 주문 - 박스 ID 매핑 파라미터 설정
			params.put("inputSeq", newSeq);
			params.put("stationCd", input.getStationCd());
			
			// 5. 작업 데이터 주문 - 박스 ID 매핑 쿼리 실행
			this.queryManager.executeBySql(updateJobSql, params);
		}
		
		// 6. 주문 정보 주문 - 박스 ID 매핑 쿼리 실행
		String sql = "update orders set box_id = :boxId, status = :inputStatus where domain_id = :domainId and batch_id = :batchId and class_cd = :classCd";
		this.queryManager.executeBySql(sql, params);
		
		// 7. 투입 정보 일괄 생성 
		this.queryManager.insertBatch(inputList);
	}
	
	/**
	 * 버킷 투입 전 액션
	 * 
	 * @param batch
	 * @param isBox
	 * @param bucket
	 * @return
	 */
	protected String beforeInputEmptyBucket(JobBatch batch, boolean isBox, IBucket bucket) {
		
		// 1. 버킷의 투입 가능 여부 확인
		this.checkUsableBox(batch, bucket, isBox);
		
		// 2. 박스와 매핑하고자 하는 작업 정보를 조회한다.
		String nextOrderNo = this.findNextMappingJob(batch, bucket);
		
		// 3. 이미 처리된 주문인지 한 번 더 체크
		if(AnyEntityUtil.selectSizeByEntity(batch.getDomainId(), JobInput.class, "batchId,orderNo", batch.getId(), nextOrderNo) > 0) {
			// 주문 은(는) 이미 투입 상태입니다
			throw new ElidomRuntimeException(ThrowUtil.translateMessage("A_ALREADY_B_STATUS", "terms.label.order", "terms.label.input"));
		}
		
		return nextOrderNo;
	}
	
	/**
	 * 합포 주문의 투입될 박스와 매핑될 작업 정보를 조회
	 * 
	 * @param batch
	 * @param bucket
	 * @return
	 */
	protected String findNextMappingJob(JobBatch batch, IBucket bucket) {
		
		// 박스와 매핑될 컬럼명 조회
		String boxMappingColumn = DpsBatchJobConfigUtil.getBoxMappingTargetField(batch);
		String nextJobId = this.findNextMappingJob(batch, bucket.getBucketTypeCd(), bucket.getBucketCd(), boxMappingColumn, DpsCodeConstants.DPS_ORDER_TYPE_MT, null);
		
		if(ValueUtil.isEmpty(nextJobId)) {
			// 박스에 할당할 주문 정보가 존재하지 않습니다
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_ASSIGN_BOX"));
		}

		return nextJobId;
	}
	
	/**
	 * 박스 혹은 버킷의 사용 가능 여부를 확인 
	 * 
	 * @param batch
	 * @param bucket
	 * @param isBox
	 */
	protected void checkUsableBox(JobBatch batch, IBucket bucket, boolean isBox) {
		
		boolean usedBox = false;
		
		// 1. 사용 가능한 박스 인지 체크
		if(isBox) {
			// 1.1 박스는 쿼리를 해서 확인
			usedBox = this.checkUniqueBoxId(batch, bucket.getBucketCd());
		} else {
			// 1.2 트레이는 상태가 WAIT 인 트레이만 사용 가능
			if(ValueUtil.isNotEqual(bucket.getStatus(), DpsConstants.COMMON_STATUS_WAIT)) {
				usedBox = true;
			}
		}
		
		// 2. 중복되는 버킷이 있으면 사용중 이면 불가
		if(usedBox) {
			// 박스 / 트레이 은(는) 이미 투입 상태입니다
			String bucketStr = isBox ? "terms.label.box" : "terms.label.tray";
			throw ThrowUtil.newValidationErrorWithNoLog(ThrowUtil.translateMessage("A_ALREADY_B_STATUS", bucketStr, "terms.label.input"));
		}
	}
	
	/**
	 * 박스 혹은 버킷이 투입 가능한 지 확인 & Locking
	 * 
	 * @param batch
	 * @param boxId
	 * @param inputType
	 */
	protected IBucket vaildInputBucketByBucketCd(JobBatch batch, String boxId, boolean isBox, boolean withLock) {
		
		// 1. 박스 타입이면 박스에서 조회
		if(isBox) {
			String boxTypeCd = this.getBoxTypeByBoxId(batch, boxId);
			BoxType boxType = DpsServiceUtil.findBoxType(batch.getDomainId(), boxTypeCd, withLock, true);
			boxType.setBoxId(boxId);
			return boxType;
			
		// 2. 트레이 타입이면 트레이에서 조회
		} else {
			return DpsServiceUtil.findTrayBox(batch.getDomainId(), boxId, withLock, true);
		}
	}

	/**
	 * 배치 설정에 박스 아이디 유니크 범위로 중복 여부 확인 
	 * 
	 * @param batch
	 * @param boxId
	 * @return
	 */
	private boolean checkUniqueBoxId(JobBatch batch, String boxId) {
		
		// 1. 박스 아이디 유니크 범위 설정
		String uniqueScope = DpsBatchJobConfigUtil.getBoxIdUniqueScope(batch, DpsConstants.BOX_ID_UNIQUE_SCOPE_GLOBAL);
		
		// 2. 파라미터 셋팅 
		Map<String, Object> params = ValueUtil.newMap("domainId,boxId,batchId,uniqueScope", batch.getDomainId(), boxId, batch.getId(), uniqueScope);
		
		// 3. 중복 박스 ID가 존재하는지 쿼리
		String qry = this.boxQueryStore.getBoxIdUniqueCheckQuery();
		
		// 4. 존재하지 않으면 사용 가능
		return this.queryManager.selectBySql(qry, params, Integer.class) > 0;
	}
	
	/**
	 * boxId 에서 박스 타입 구하기
	 * 
	 * @param batch
	 * @param boxId
	 * @return
	 */
	private String getBoxTypeByBoxId(JobBatch batch, String boxId) {
		
		// 1. 박스 ID 에 박스 타입 split 기준, TODO 커스텀 서비스 호출
		String boxTypeSplit = DpsBatchJobConfigUtil.getBoxTypeSplitByBoxId(batch); 
		
		// 2. 설정 값이 없으면 기본 0,1
		if(ValueUtil.isEmpty(boxTypeSplit) || boxTypeSplit.length() < 3) {
			boxTypeSplit = "0,1";
		}
		
		// 3. 기준에 따라 박스 타입 분할 
		String[] splitIndex = boxTypeSplit.split(DpsConstants.COMMA);
		return boxId.substring(ValueUtil.toInteger(splitIndex[0]), ValueUtil.toInteger(splitIndex[1]));
	}
	
	/**
	 * 다음 맵핑 할 작업의 ID를 찾는다.
	 * 
	 * @param batch
	 * @param boxTypeCd
	 * @param bucketCd
	 * @param boxMappingColumn
	 * @param orderType
	 * @param skuCd
	 * @return
	 */
	private String findNextMappingJob(JobBatch batch, String boxTypeCd, String bucketCd, String boxMappingColumn, String orderType, String skuCd) {
		
		// 1. 쿼리 
		String qry = this.pickQueryStore.getFindNextMappingJobQuery();
		
		// 2. 파라미터
		Long domainId = batch.getDomainId();
		Map<String, Object> params = ValueUtil.newMap("domainId,mapColumn,batchId,orderType,boxTypeCd,bucketCd", domainId, boxMappingColumn, batch.getId(), orderType, boxTypeCd, bucketCd);
		if(ValueUtil.isNotEmpty(skuCd)) {
			params.put("skuCd", skuCd);
		}
		
		// 3. 조회 (맵핑 기준에 따라 결과가 달라짐)
		return AnyEntityUtil.findItem(domainId, false, String.class, qry, params);
	}
	
	/************************************************************************************************/
	/*											3. 소분류												*/
	/************************************************************************************************/

	/**
	 * 소분류 작업 처리 전 처리 액션
	 * 
	 * @param batch
	 * @param job
	 * @param cell
	 * @param pickQty
	 * @return
	 */
	protected int beforeConfirmPick(JobBatch batch, JobInstance job, Cell cell, int pickQty) {
		// 1. 작업이 이미 완료되었다면 리턴
		if(job.isDoneJob()) {
			return 0;
		// 2. 이미 모두 처리되었다면 스킵
		} else if(job.getPickedQty() >= job.getPickQty()) {
			return 0;
		}
		
		// 3. 피킹 수량 보정 - 주문 수량 보다 처리 수량이 큰 경우 차이 값 만큼만 처리 
		if(job.getPickedQty() + pickQty > job.getPickQty()) {
			pickQty = job.getPickQty() - job.getPickedQty();
		}
		
		// 4. 피킹 수량 리턴
		return pickQty;
	}
	
	/**
	 * 소분류 작업 처리
	 * 
	 * @param batch
	 * @param job
	 * @param cell
	 * @param pickQty
	 */
	protected void doConfirmPick(JobBatch batch, JobInstance job, Cell cell, int pickQty) {
		// 1. 피킹 작업 처리
		job.setPickedQty(job.getPickedQty() == null ? pickQty : job.getPickedQty() + pickQty);
		job.setPickingQty(0);
		String currentTime = DateUtil.currentTimeStr();
		
		if(ValueUtil.isEmpty(job.getPickStartedAt())) {
			job.setPickStartedAt(currentTime);
		}
		
		if(job.getPickedQty() >= job.getPickQty()) {
			job.setStatus(DpsConstants.JOB_STATUS_FINISH);
			job.setPickEndedAt(currentTime);
		}
		
		this.queryManager.update(job, "pickStartedAt", "pickEndedAt", "pickedQty", "pickingQty", DpsConstants.ENTITY_FIELD_STATUS, DpsConstants.ENTITY_FIELD_UPDATER_ID, DpsConstants.ENTITY_FIELD_UPDATED_AT);

		// 2. 재고 계산
		this.serviceDispatcher.getStockService().removeStockForPicking(job.getDomainId(), job.getEquipType(), job.getEquipCd(), job.getSubEquipCd(), job.getComCd(), job.getSkuCd(), -1 * pickQty);
	}

	/**
	 * 소분류 작업 처리 후 처리 액션
	 * 
	 * @param batch
	 * @param job
	 * @param cell
	 * @param pickQty
	 */
	protected void afterComfirmPick(JobBatch batch, JobInstance job, Cell cell, Integer pickQty) {
		// 1. 작업 정보와 연관된 주문을 조회해서 피킹 확정 수량을 업데이트한다.
		this.updateOrdersByPick(job, pickQty);
		
		// 2. 작업이 해당 스테이션에서 끝났는지 체크
		boolean isStationJobEnded = this.checkStationJobsEnd(batch, (cell != null ? cell.getStationCd() : null), job);

		// 3. 작업이 해당 스테이션에서 완료되었다면
		if(isStationJobEnded) {
			// 3-1. 작업 스테이션의 투입 정보 상태 '완료'로 업데이트
			this.updateJobInputStatus(batch, job, cell, DpsCodeConstants.JOB_INPUT_STATUS_FINISHED);

			// 3-2. 주문(박스)에 대한 피킹이 모두 완료되었는지 체크 
			if(this.checkBoxingEnd(batch, job.getOrderNo(), job.getBoxId())) {
				// 3-3. 작업의 '상태' 및 '박싱 시각'을 업데이트
				String sql = "update job_instances set status = :status, boxed_at = :currentTime where domain_id = :domainId and batch_id = :batchId and class_cd = :classCd";
				Map<String, Object> condition = ValueUtil.newMap("domainId,batchId,classCd,status,currentTime", job.getDomainId(), job.getBatchId(), job.getClassCd(), LogisConstants.JOB_STATUS_BOXED, job.getPickEndedAt());
				this.queryManager.executeBySql(sql, condition);
			}
		}
		
		// 4. 모바일 새로고침 명령 전달
		this.serviceDispatcher.getDeviceService().sendMessageToDevice(batch.getDomainId(), DeviceCommand.EQUIP_TABLET, batch.getStageCd(), cell.getEquipType(), cell.getEquipCd(), cell.getStationCd(), null, batch.getJobType(), DeviceCommand.COMMAND_REFRESH, "confirm-pick", null);
	}
	
	/**
	 * 피킹 처리에 따른 주문 정보 업데이트
	 * 
	 * @param job
	 * @param pickQty
	 * @return
	 */
	private List<String> updateOrdersByPick(JobInstance job, int pickQty) {
		List<Order> orderList = this.searchOrdersByJob(job);
		if(ValueUtil.isEmpty(orderList)) {
			return null;
		}
		
		List<String> orderIds = new ArrayList<String>(orderList.size());
		int remainPickQty = pickQty;
		
		for(Order order : orderList) {
			if(remainPickQty <= 0) {
				break;
			}
			
			int orderMaxPickQty = order.getOrderQty() - order.getPickedQty();
			int orderPickQty = (orderMaxPickQty >= pickQty) ? pickQty : pickQty - orderMaxPickQty;
			remainPickQty = remainPickQty - orderPickQty;
			order.setPickedQty(ValueUtil.toInteger(order.getPickedQty()) + orderPickQty);
			order.setStatus(order.getPickedQty() >= order.getOrderQty() ? Order.STATUS_FINISHED : Order.STATUS_RUNNING);
			this.queryManager.update(order, "status", "pickedQty", "updatedAt");
			orderIds.add(order.getId());
		}

		return orderIds;
	}
	
	/**
	 * 박스 내품 내역을 생성하기 위해 주문 내역 정보를 조회
	 * 
	 * @param job
	 * @return
	 */
	private List<Order> searchOrdersByJob(JobInstance job) {
		StringJoiner sql = new StringJoiner(SysConstants.LINE_SEPARATOR);
		sql.add("SELECT * FROM (")
		   .add("	SELECT")
		   .add("		ID, DOMAIN_ID, SKU_CD, ORDER_QTY, PICKED_QTY")
		   .add("	FROM")
		   .add("		ORDERS")
		   .add("	WHERE")
		   .add("		DOMAIN_ID = :domainId AND BATCH_ID = :batchId AND ORDER_NO = :orderNo AND COM_CD = :comCd AND SKU_CD = :skuCd AND (ORDER_QTY > PICKED_QTY)")
		   .add(") A ORDER BY A.PICKED_QTY ASC");
		
		Map<String, Object> params = 
				ValueUtil.newMap("domainId,batchId,orderNo,comCd,skuCd", job.getDomainId(), job.getBatchId(), job.getOrderNo(), job.getComCd(), job.getSkuCd());
		return this.queryManager.selectListBySql(sql.toString(), params, Order.class, 0, 0);
	}
	
	/**
	 * 합포인 경우 투입 정보의 상태를 업데이트 
	 * 
	 * @param batch
	 * @param job
	 * @param cell
	 * @param status
	 */
	private void updateJobInputStatus(JobBatch batch, JobInstance job, Cell cell, String status) {
		JobInput input = AnyEntityUtil.findEntityBy(batch.getDomainId(), true, true, JobInput.class, null
									, "batchId,equipType,equipCd,orderNo,inputSeq"
									, batch.getId(), job.getEquipType(), job.getEquipCd(), job.getOrderNo(), job.getInputSeq());
		
		if(input != null) {
			input.setStatus(status);
			this.queryManager.update(input, DpsConstants.ENTITY_FIELD_STATUS, DpsConstants.ENTITY_FIELD_UPDATED_AT);
		}
	}

}
