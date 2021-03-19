package operato.logis.dps.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import operato.logis.dps.DpsCodeConstants;
import operato.logis.dps.query.store.DpsBatchQueryStore;
import operato.logis.dps.service.util.DpsBatchJobConfigUtil;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobConfigSet;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.Order;
import xyz.anythings.base.entity.Rack;
import xyz.anythings.base.event.EventConstants;
import xyz.anythings.base.service.api.IInstructionService;
import xyz.anythings.base.service.impl.AbstractInstructionService;
import xyz.anythings.base.service.impl.JobConfigProfileService;
import xyz.anythings.base.util.LogisBaseUtil;
import xyz.anythings.gw.entity.IndConfigSet;
import xyz.anythings.gw.service.IndConfigProfileService;
import xyz.anythings.sys.event.model.EventResultSet;
import xyz.anythings.sys.event.model.SysEvent;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 용 작업 지시 서비스
 * 
 * @author yang
 */
@Component("dpsInstructionService")
public class DpsInstructionService extends AbstractInstructionService implements IInstructionService {
	
	/**
	 * 커스텀 서비스 - 대상 분류
	 */
	private static final String DIY_CLASSIFY_ORDERS = "diy-dps-classify-orders";
	/**
	 * 커스텀 서비스 - 추천 로케이션
	 */
	private static final String DIY_RECOMMEND_CELLS = "diy-dps-recommend-cells";
	/**
	 * 커스텀 서비스 - 토털 피킹
	 */
	private static final String DIY_TOTAL_PICKING = "diy-dps-totalpicking";
	/**
	 * 커스텀 서비스 - 박스 요청
	 */
	private static final String DIY_REQUEST_BOX = "diy-dps-request-box";
	/**
	 * 커스텀 서비스 - 작업 지시 전 처리
	 */
	private static final String DIY_PRE_BATCH_START = "diy-dps-pre-batch-start";
	/**
	 * 커스텀 서비스 - 작업 지시 후 처리
	 */
	private static final String DIY_POST_BATCH_START = "diy-dps-post-batch-start";
	/**
	 * 커스텀 서비스 - 배치 병합 전 처리
	 */
	private static final String DIY_PRE_MERGE_BATCH = "diy-dps-pre-merge-batch";
	/**
	 * 커스텀 서비스 - 배치 병합 후 처리
	 */
	private static final String DIY_POST_MERGE_BATCH = "diy-dps-post-merge-batch";
	/**
	 * 커스텀 서비스 - 작업 지시 취소 전 처리
	 */
	private static final String DIY_PRE_CANCEL_BATCH = "diy-dps-pre-cancel-batch";
	/**
	 * 커스텀 서비스 - 작업 지시 취소 후 처리
	 */
	private static final String DIY_POST_CANCEL_BATCH = "diy-dps-post-cancel-batch";
	
	/**
	 * 작업 설정 프로파일 서비스
	 */
	@Autowired
	private JobConfigProfileService jobConfigProfileSvc;
	/**
	 * 표시기 설정 프로파일 서비스
	 */
	@Autowired
	private IndConfigProfileService indConfigSetService;
	/**
	 * 배치 쿼리 스토어
	 */
	@Autowired
	private DpsBatchQueryStore dpsBatchQueryStore;

	@Override
	public void targetClassing(JobBatch batch, Object... params) {
		// 1. 전처리 이벤트
		EventResultSet befResult = this.publishClassificationEvent(SysEvent.EVENT_STEP_BEFORE, batch, null, params);
		
		// 2. 다음 처리 취소 일 경우 결과 리턴
		if(!befResult.isAfterEventCancel()) {
			
			// 3. 대상 분류 전 처리
			this.doUpdateClassificationCodes(batch, params);
			
			// 4. 대상 분류 프로세싱
			this.doClassifyOrders(batch, null, params);
			
			// 5. 후처리 이벤트
			this.publishClassificationEvent(SysEvent.EVENT_STEP_AFTER, batch, null, params);
		}
	}
	
	@Override
	public Map<String, Object> searchInstructionData(JobBatch batch, Object... params) {
		// DPS에서는 구현이 필요없음
		return null;
	}
	
	/**
	 * 배치 데이터에 대해 설비 정보 여부 를 찾아 대상 설비 리스트를 리턴
	 * 
	 * @param jobBatch
	 * @param equipIdList
	 * @return
	 */
	@Override
	protected List<Rack> searchEquipListByBatch(JobBatch batch, List<String> equipIdList) {
		List<Rack> rackList = null;
		
		// 1. 상위 시스템에서 주문에 호기를 이미 설정해서 내린 경우
		if(this.isSplitableBatch(batch)) {
			rackList = this.searchRackByOrders(batch);
			
		// 2. 사용자가 작업 지시 시점에 배치 실행을 위한 호기를 선택한 경우 
		} else {
			rackList = this.searchEquipListByEquipIds(batch.getDomainId(), Rack.class, equipIdList);
		}
		
		if(ValueUtil.isEmpty(rackList)) {
			throw ThrowUtil.newValidationErrorWithNoLog("배치를 실행할 설비를 선택하세요.");
		}
		
		// 3. 배치 : 호기 = 1 : 1인 경우
		if(rackList.size() == 1) {
			Rack rack = rackList.get(0);
			String sql = "update orders set equip_group_cd = :equipGroupCd, equip_cd = :equipCd, equip_nm = :equipNm where domain_id = :domainId and batch_id = :batchId and equip_cd is null";
			Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipGroupCd,equipCd,equipNm", batch.getDomainId(), batch.getId(), rack.getEquipGroupCd(), rack.getRackCd(), rack.getRackNm());
			this.queryManager.executeBySql(sql, params);
			
			// 배치에 설비 설정
			batch.setEquipGroupCd(rack.getEquipGroupCd());
			batch.setEquipCd(rack.getRackCd());
			batch.setEquipNm(rack.getRackNm());
		}
		
		// 4. 랙 리스트 리턴
		return rackList;
	}
	
	/**
	 * 배치를 호기별로 분할할 지 여부 판단
	 * 
	 * @param batch
	 * @return
	 */
	private boolean isSplitableBatch(JobBatch batch) {
		String sql = "select distinct equip_cd from orders where domain_id = :domainId and batch_id = :batchId and equip_cd is not null";
		Map<String, Object> queryParams = ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId());
		int count = this.queryManager.selectSizeBySql(sql, queryParams);
		return count > 1;
	}
	
	/**
	 * 배치 주문으로 부터 랙 조회
	 * 
	 * @param batch
	 * @return
	 */
	private List<Rack> searchRackByOrders(JobBatch batch) {
		String sql = "select * from racks where domain_id = :domainId and rack_cd in (select distinct equip_cd from orders where domain_id = :domainId and batch_id = :batchId) order by equip_cd";
		Map<String, Object> queryParams = ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId());
		return this.queryManager.selectListBySql(sql, queryParams, Rack.class, 0, 0);
	}
	
	@Override
	public int instructBatch(JobBatch batch, List<String> equipIdList, Object... params) {
		// 1. 작업 배치 정보로 설비 리스트 조회
		List<Rack> rackList = this.searchEquipListByBatch(batch, equipIdList);
		
		// 2. 배치 정보에 설정값이 없는지 체크하여 없으면 설정
		this.checkJobAndIndConfigSet(batch);
		
		// 3. 랙에 배치 할당
		for(Rack rack : rackList) {
			rack.setBatchId(batch.getId());
			rack.setStatus(JobBatch.STATUS_RUNNING);
		}
		
		AnyOrmUtil.updateBatch(rackList, 100, "batchId", "status", "updatedAt");
		
		// 4. 소분류 코드, 방면 분류 코드 값을 설정에 따라서 주문 정보에 추가한다.
		this.doUpdateClassificationCodes(batch, params);

		// 5. 대상 분류
		this.doClassifyOrders(batch, rackList, params);
		
		// 6. 추천 로케이션 정보 생성
		this.doRecommendCells(batch, rackList, params);
		
		// 7. 작업 지시 처리
		int retCnt = this.doInstructBatch(batch, rackList, params);
		
		// 8. 작업 지시 후 박스 요청 
		this.doRequestBox(batch, rackList, params);
		
		// 9. 건수 리턴
		return retCnt;
	}

	@Override
	public int instructTotalpicking(JobBatch batch, List<String> equipIdList, Object... params) {
		// 1. 토털 피킹 전 처리 이벤트
		EventResultSet befResult = this.publishTotalPickingEvent(SysEvent.EVENT_STEP_BEFORE, batch, equipIdList, params);
		
		// 2. 다음 처리 취소일 경우 결과 리턴
		if(befResult.isAfterEventCancel()) {
			return ValueUtil.toInteger(befResult.getResult());
		}
		
		// 3. 커스텀 서비스 호출
		Object retVal = this.customService.doCustomService(batch.getDomainId(), DIY_TOTAL_PICKING, ValueUtil.newMap("batch,equipList", batch, equipIdList));
		
		// 4. 토털 피킹 후 처리 이벤트
		EventResultSet aftResult = this.publishTotalPickingEvent(SysEvent.EVENT_STEP_AFTER, batch, equipIdList, params);
		
		// 5. 후처리 이벤트 실행 후 리턴 결과가 있으면 해당 결과 리턴
		if(aftResult.isExecuted()) {
			if(retVal == null && aftResult.getResult() != null) {
				retVal = aftResult.getResult();
			}
		}
		
		// 6. 결과 건수 리턴
		return this.returnValueToInt(retVal);
	}

	@Override
	public int mergeBatch(JobBatch mainBatch, JobBatch newBatch, Object... params) {
		// 1. 작업 배치 정보로 설비 리스트 조회
		String sql = "select * from racks where domain_id = :domainId and batch_id = :batchId";
		List<Rack> rackList = this.queryManager.selectListBySql(sql, ValueUtil.newMap("domainId,batchId", mainBatch.getDomainId(), mainBatch.getId()), Rack.class, 0, 0);
		
		// 2. 병합의 경우에는 메인 배치의 설정 셋을 가져온다.
		newBatch.setJobConfigSetId(mainBatch.getJobConfigSetId());
		newBatch.setIndConfigSetId(mainBatch.getIndConfigSetId());
		this.queryManager.update(newBatch , "jobConfigSetId","indConfigSetId");
		
		// 3. 소분류 코드, 방면 분류 코드 값을 설정에 따라서 주문 정보에 추가한다.
		this.doUpdateClassificationCodes(newBatch, params);

		// 4. 대상 분류 
		this.doClassifyOrders(newBatch, rackList, params);
		
		// 5. 추천 로케이션 정보 생성
		this.doRecommendCells(newBatch, rackList, params);
		
		// 6. 작업 병합 처리
		int retCnt = this.doMergeBatch(mainBatch, newBatch, rackList, params);
		
		// 7. 작업 병합 후 박스 요청
		this.doRequestBox(mainBatch, rackList, params);
		
		// 8. 병합 건수 리턴
		return retCnt;
	}

	@Override
	public int cancelInstructionBatch(JobBatch batch) {
		// 1. 작업 지시 취소 전 처리 이벤트 
		EventResultSet befResult = this.publishInstructionCancelEvent(SysEvent.EVENT_STEP_BEFORE, batch, null);
		
		// 2. 다음 처리 취소일 경우 결과 리턴 
		if(befResult.isAfterEventCancel()) {
			return ValueUtil.toInteger(befResult.getResult());
		}
		
		// 3. 커스텀 서비스 작업 지시 취소 전 처리 호출
		Map<String, Object> svcParams = ValueUtil.newMap("batch", batch);
		this.customService.doCustomService(batch.getDomainId(), DIY_PRE_CANCEL_BATCH, svcParams);
		
		// 4. 작업 지시 취소 조건 체크
		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId());
		condition.addFilter("batchId", batch.getId());
		condition.addFilter(new Filter("pickedQty", OrmConstants.GREATER_THAN, 0));
		if(this.queryManager.selectSize(JobInstance.class, condition) > 0) {
			throw ThrowUtil.newValidationErrorWithNoLog(true, "NOT_ALLOWED_CANCEL_AFTER_START_JOB"); // 분류 작업시작 이후여서 취소가 불가능합니다
		}
		
		// 5. 작업 삭제
		String sql = "delete from job_instances where domain_id = :domainId and batch_id = :batchId";
		Map<String, Object> queryParams = ValueUtil.newMap("domainId,batchId,fromStatus,toStatus,orderType", batch.getDomainId(), batch.getId(), Order.STATUS_WAIT, Order.STATUS_TYPE, DpsCodeConstants.DPS_ORDER_TYPE_MT);
		this.queryManager.executeBySql(sql, queryParams);
		
		// 6. 합포 주문 업데이트
		sql = this.dpsBatchQueryStore.getDpsOrderStatusByInstruct();
		this.queryManager.executeBySql(sql, queryParams);
		
		// 7. 랙 배치 ID 업데이트
		queryParams.put("equipCd", batch.getEquipCd());
		sql = "update racks set batch_id = null, status = null where domain_id = :domainId and rack_cd = :equipCd";
		this.queryManager.executeBySql(sql, queryParams);
		
		// 8. 작업 배치 업데이트
		sql = "select id from job_batches where domain_id = :domainId and batch_group_id = :batchGroupId and status != 'MERGED'";
		int sameGroupBatchCount = this.queryManager.selectSizeBySql(sql, ValueUtil.newMap("domainId,batchGroupId", batch.getDomainId(), batch.getBatchGroupId()));
		
		// 9. 병합된 배치를 제외한 동일 그룹의 배치가 자신 밖에 없다면 호기 선택 모드이다. 이 때는 작업 지시 취소 이후 다른 호기를 선택할 수 있으므로 호기 정보를 없애야 한다.
		if(sameGroupBatchCount <= 1) {
			// 작업 배치의 호기 정보 리셋
			batch.setEquipGroupCd(null);
			batch.setEquipCd(null);
			batch.setEquipNm(null);
			
			// 주문의 호기 정보 리셋
			sql = "update orders set equip_cd = null, equip_nm = null where domain_id = :domainId and batch_id = :batchId";
			this.queryManager.executeBySql(sql, queryParams);
		}
		
		// 10. 작업 배치 정보 업데이트
		batch.setStatus(JobBatch.STATUS_READY);
		batch.setInstructedAt(null);
		this.queryManager.update(batch, "equipGroupCd", "equipCd", "equipNm", "status", "instructedAt", "updatedAt");
		
		// 11. 커스텀 서비스 작업 지시 취소 후 처리 호출
		Object retVal = this.customService.doCustomService(batch.getDomainId(), DIY_POST_CANCEL_BATCH, svcParams);
		
		// 12. 작업 지시 취소 후 처리 이벤트
		EventResultSet aftResult = this.publishInstructionCancelEvent(SysEvent.EVENT_STEP_AFTER, batch, null);
		
		// 13. 후 처리 이벤트 실행 후 리턴 결과가 있으면 해당 결과 리턴
		if(aftResult.isExecuted()) {
			if(retVal == null && aftResult.getResult() != null) {
				retVal = aftResult.getResult();
			}
		}
		
		// 14. 결과 리턴
		return this.returnValueToInt(retVal);
	}
	
	/**
	 * 배치에 설정값이 설정되어 있는지 체크하고 기본 설정값으로 설정한다.
	 * 
	 * @param batch
	 * @param rackList
	 */
	private void checkJobAndIndConfigSet(JobBatch batch) {
		// 1. 작업 관련 설정이 없는 경우 기본 작업 설정을 찾아서 세팅
		if(ValueUtil.isEmpty(batch.getJobConfigSetId())) {
			JobConfigSet jobConfigSet = this.jobConfigProfileSvc.getStageConfigSet(batch.getDomainId(), batch.getStageCd());
			
			if(jobConfigSet == null) {
				throw ThrowUtil.newJobConfigNotSet();
			} else {
				batch.setJobConfigSetId(jobConfigSet.getId());
				batch.setJobConfigSet(jobConfigSet);
			}
		}
		
		// 2. 표시기 관련 설정이 없는 경우 표시기 설정을 찾아서 세팅
		if(ValueUtil.isEmpty(batch.getIndConfigSetId())) {
			IndConfigSet indConfigSet = this.indConfigSetService.getStageConfigSet(batch.getDomainId(), batch.getStageCd());
			
			if(indConfigSet == null) {
				throw ThrowUtil.newIndConfigNotSet();
			} else {
				batch.setIndConfigSetId(indConfigSet.getId());
				batch.setIndConfigSet(indConfigSet);
			}
		}
	}
	
	/**
	 * 작업 배치 소속 주문 데이터의 소분류, 방면 분류 코드를 업데이트 ...
	 * 
	 * @param batch
	 * @param params
	 */
	private void doUpdateClassificationCodes(JobBatch batch, Object ... params) {
		// 1. 소분류 매핑 필드 - class_cd 매핑 
		String classTargetField = DpsBatchJobConfigUtil.getBoxMappingTargetField(batch);
		
		if(ValueUtil.isNotEmpty(classTargetField)) {
			String sql = "UPDATE ORDERS SET CLASS_CD = %s WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId";
			sql = String.format(sql, classTargetField);
			Map<String, Object> updateParams = ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId());
			this.queryManager.executeBySql(sql, updateParams);
		}

		// 2. 방면분류 매핑 필드 - box_class_cd 매핑
		String boxClassTargetField = DpsBatchJobConfigUtil.getBoxOutClassTargetField(batch , false);
		
		if(ValueUtil.isNotEmpty(boxClassTargetField)) {
			String sql = "UPDATE ORDERS SET BOX_CLASS_CD = %s WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId";
			sql = String.format(sql, boxClassTargetField);
			Map<String, Object> updateParams = ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId());
			this.queryManager.executeBySql(sql, updateParams);
		}
	}
	
	/**
	 * 작업 대상 분류
	 * 
	 * @param batch
	 * @param rackList
	 * @param params
	 */
	private void doClassifyOrders(JobBatch batch, List<Rack> rackList, Object... params) {
		// 1. 전처리 이벤트
		EventResultSet befResult = this.publishClassificationEvent(SysEvent.EVENT_STEP_BEFORE, batch, rackList, params);
		
		// 2. 다음 처리 취소 일 경우 결과 리턴
		if(!befResult.isAfterEventCancel()) {
			
			// 3. 대상 분류 프로세싱
			this.processClassifyOrders(batch, rackList, params);
			
			// 4. 후처리 이벤트
			this.publishClassificationEvent(SysEvent.EVENT_STEP_AFTER, batch, rackList, params);
		}
	}
	
	/**
	 * 대상 분류 프로세싱 - 커스텀 서비스 (diy-dps-classify-orders) 연동
	 * 
	 * @param batch
	 * @param rackList
	 * @param params
	 * @return
	 */
	private int processClassifyOrders(JobBatch batch, List<Rack> rackList, Object... params) {
		// 1. 상위 시스템 대상 분류 여부 확인
		String sql = "SELECT ID FROM ORDERS WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId AND ORDER_TYPE IS NOT NULL";
		Map<String, Object> queryParams = ValueUtil.newMap("domainId,batchId,orderType", batch.getDomainId(), batch.getId(), DpsCodeConstants.DPS_ORDER_TYPE_MT);
		int classifyCount = this.queryManager.selectSizeBySql(sql, queryParams);
		
		// 2. 상위 시스템에서 대상 분류 한 작업 이면 건수만 리턴
		if(classifyCount > 0) {
			queryParams.put("currentTime", new Date());
			sql = "UPDATE ORDERS SET STATUS = 'T', UPDATED_AT = :currentTime WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId";
			return this.queryManager.executeBySql(sql, queryParams);
		}
		
		// 3. 합포 대상 분류
		sql = this.dpsBatchQueryStore.getDpsClassifyMultiOrders();
		classifyCount += this.queryManager.executeBySql(sql, queryParams);
		
		// 4. 커스텀 서비스 호출
		this.customService.doCustomService(batch.getDomainId(), DIY_CLASSIFY_ORDERS, ValueUtil.newMap("batch,equipList", batch, rackList));
		return classifyCount;
	}
	
	/**
	 * 추천 로케이션 처리
	 * 
	 * @param batch
	 * @param rackList
	 * @param params
	 */
	private void doRecommendCells(JobBatch batch, List<Rack> rackList, Object ... params) {
		// 1. 전 처리 이벤트
		EventResultSet befResult = this.publishRecommendCellsEvent(SysEvent.EVENT_STEP_BEFORE, batch, rackList, params);
		
		// 2. 다음 처리 취소 일 경우 결과 리턴 
		if(!befResult.isAfterEventCancel()) {
			
			// 3. 작업 지시 실행
			this.processRecommendCells(batch, rackList, params);
			
			// 4. 후 처리 이벤트
			this.publishRecommendCellsEvent(SysEvent.EVENT_STEP_AFTER, batch, rackList, params);
		}
	}
	
	/**
	 * 추천 로케이션 실행 - 커스텀 서비스 (diy-dps-recommend-cells) 연동
	 * 
	 * @param batch
	 * @param rackList
	 * @param params
	 */
	private void processRecommendCells(JobBatch batch, List<Rack> rackList, Object ... params) {
		// 재고 적치 추천 셀 사용 유무
		boolean useRecommendCell = DpsBatchJobConfigUtil.isRecommendCellEnabled(batch);
		
		if(useRecommendCell) {
			// 커스텀 서비스 호출
			this.customService.doCustomService(batch.getDomainId(), DIY_RECOMMEND_CELLS, ValueUtil.newMap("batch,equipList", batch, rackList));
		}
	}
	
	/**
	 * 작업 지시 처리
	 * 
	 * @param batch
	 * @param rackList
	 * @param params
	 * @return
	 */
	private int doInstructBatch(JobBatch batch, List<Rack> rackList, Object ... params) {
		// 1. 전 처리 이벤트
		EventResultSet befResult = this.publishInstructionEvent(SysEvent.EVENT_STEP_BEFORE, batch, rackList, params);
		
		// 2. 다음 처리 취소 일 경우 결과 리턴 
		if(befResult.isAfterEventCancel()) {
			return ValueUtil.toInteger(befResult.getResult());
		}
		
		// 3. 작업 지시 실행
		int resultCnt = this.processInstruction(batch, params);
		
		// 4. 후 처리 이벤트 
		EventResultSet aftResult = this.publishInstructionEvent(SysEvent.EVENT_STEP_AFTER, batch, rackList, params);
		
		// 5. 후 처리 이벤트가 실행 되고 리턴 결과가 있으면 해당 결과 리턴
		if(aftResult.isExecuted()) {
			if(resultCnt == 0 && aftResult.getResult() != null) {
				resultCnt = ValueUtil.toInteger(aftResult.getResult());
			}
		}

		// 6. 결과 리턴
		return resultCnt;
	}
	
	/**
	 * 작업 지시 실행 - 커스텀 서비스 (diy-dps-pre-instruct-batch, diy-dps-post-instruct-batch) 연동
	 * 
	 * @param batch
	 * @param params
	 * @return
	 */
	private int processInstruction(JobBatch batch, Object ... params) {
		// 1. 작업 지시 전 처리를 위한 커스텀 서비스 호출
		Long domainId = batch.getDomainId();
		Map<String, Object> svcParams = ValueUtil.newMap("batch,diyParams", batch, params);
		this.customService.doCustomService(domainId, DIY_PRE_BATCH_START, svcParams);

		// 2. 호기별로 배치 분리 설정인지 체크
		if(this.isSplitableBatch(batch)) {
			// 호기별 배치 분리 및 주문 분리
			batch = this.splitBatch(batch);
		}
		
		// 3. 합포 주문 업데이트
		Map<String, Object> queryParams = ValueUtil.newMap("domainId,batchGroupId,equipCd,equipNm,fromStatus,toStatus",
						domainId, batch.getBatchGroupId(), batch.getEquipCd(), batch.getEquipNm(), Order.STATUS_TYPE, Order.STATUS_WAIT);
		String sql = this.dpsBatchQueryStore.getDpsOrderStatusByInstruct();
		this.queryManager.executeBySql(sql, queryParams);
		
		// 4. 작업 배치 업데이트
		batch.setStatus(JobBatch.STATUS_RUNNING);
		batch.setInstructedAt(new Date());
		this.queryManager.update(batch, "equipGroupCd", "equipCd", "equipNm", "status", "jobConfigSetId", "indConfigSetId", "instructedAt", "updatedAt");
		
		// 5. 작업 지시 후 처리를 위한 커스텀 서비스 호출
		this.customService.doCustomService(domainId, DIY_POST_BATCH_START, svcParams);
		return 1;
	}
	
	/**
	 * 배치를 주문의 호기별로 분할
	 * 
	 * @param batch
	 * @return
	 */
	private JobBatch splitBatch(JobBatch batch) {
		Long domainId = batch.getDomainId();
		Map<String, Object> queryParams = ValueUtil.newMap("domainId,batchId", domainId, batch.getId());
		List<Rack> rackList = this.searchRackByOrders(batch);
		
		if(rackList != null && !rackList.isEmpty()) {
			boolean isFirst = true;
			JobBatch currentBatch = null;
			
			// 조회된 모든 랙을 대상으로 
			for(Rack rack : rackList) {
				// 2. 현재 배치 정보 설정
				currentBatch = isFirst ? batch : null;
				
				// 3. 첫번째 호기가 아니면 분할 배치 생성 
				if(currentBatch == null) {
					currentBatch = new JobBatch();
					currentBatch = ValueUtil.populate(batch, currentBatch);
					currentBatch.setId(LogisBaseUtil.newJobBatchId(domainId, batch.getStageCd()));
				}
				
				// 4. 분할 배치별 주문 수 계산 후 배치 정보에 업데이트
				String sql = "select count(distinct(class_cd)) as batch_order_qty, sum(order_qty) as batch_pcs from orders where domain_id = :domainId and batch_id = :batchId and equip_cd = :equipCd";
				JobBatch orderCnt = this.queryManager.selectBySql(sql, queryParams, JobBatch.class);
				currentBatch.setEquipCd(rack.getRackCd());
				currentBatch.setEquipNm(rack.getRackNm());
				currentBatch.setBatchOrderQty(orderCnt.getBatchOrderQty());
				currentBatch.setBatchPcs(orderCnt.getBatchPcs());
				
				// 5. 배치 정보 생성 혹은 업데이트
				if(isFirst) {
					this.queryManager.update(currentBatch, "jobConfigSetId", "indConfigSetId", "equipCd", "equipNm", "batchOrderQty", "batchPcs", "updatedAt");
					isFirst = false;
					
				} else {
					this.queryManager.insert(currentBatch);
					
					// 6. 배치별 주문 정보 업데이트
					queryParams.put("newBatchId", currentBatch.getId());
					queryParams.put("currentDate", new Date());
					sql = "update orders set equip_nm = :equipNm, batch_id = :newBatchId, updated_at = :currentDate where domain_id = :domainId and batch_id = :batchId and equip_cd = :equipCd";
					this.queryManager.executeBySql(sql, queryParams);
				}

				// 7. 랙에 배치 ID 설정
				rack.setBatchId(currentBatch.getId());
				this.queryManager.update(rack, "batchId", "updatedAt");
			}
		}
		
		return batch;
	}

	/**
	 * 박스 요청 처리
	 *  
	 * @param batch
	 * @param equipList
	 * @param params
	 * @return
	 */
	private int doRequestBox(JobBatch batch, List<?> equipList, Object... params) {
		// 1. 단독 처리 이벤트   
		EventResultSet eventResult = this.publishRequestBoxEvent(batch, equipList, params);
		
		// 2. 다음 처리 취소 일 경우 결과 리턴
		if(eventResult.isAfterEventCancel()) {
			return ValueUtil.toInteger(eventResult.getResult());
		}
		
		// 3. 커스텀 서비스 호출
		Object retVal = this.customService.doCustomService(batch.getDomainId(), DIY_REQUEST_BOX, ValueUtil.newMap("batch", batch));
		return this.returnValueToInt(retVal);
	}
	
	/**
	 * 작업 병합 처리
	 * 
	 * @param mainBatch
	 * @param newBatch
	 * @param equipList
	 * @param params
	 * @return
	 */
	private int doMergeBatch(JobBatch mainBatch, JobBatch newBatch, List<?> equipList, Object... params) {
		// 1. 전처리 이벤트
		EventResultSet befResult = this.publishMergingEvent(SysEvent.EVENT_STEP_BEFORE, mainBatch, newBatch, equipList, params);
		
		// 2. 다음 처리 취소 일 경우 결과 리턴
		if(befResult.isAfterEventCancel()) {
			return ValueUtil.toInteger(befResult.getResult());
		}
		
		// 3. 배치 병합 처리
		int resultCnt = this.processMerging(mainBatch, newBatch, params);
		
		// 4. 후처리 이벤트
		EventResultSet aftResult = this.publishMergingEvent(SysEvent.EVENT_STEP_AFTER, mainBatch, newBatch, equipList, params);
		
		// 5. 후처리 이벤트가 실행 되고 리턴 결과가 있으면 해당 결과 리턴
		if(aftResult.isExecuted()) {
			if(resultCnt == 0 && aftResult.getResult() != null) {
				resultCnt = ValueUtil.toInteger(aftResult.getResult());
			}
		}

		// 6. 결과 리턴
		return resultCnt;
	}
	
	/**
	 * 작업 병합 처리
	 * 
	 * @param mainBatch
	 * @param newBatch
	 * @param params
	 * @return
	 */
	private int processMerging(JobBatch mainBatch, JobBatch newBatch, Object ... params) {
		// 1. 병합 전 커스텀 서비스 호출
		Long domainId = mainBatch.getDomainId();
		Map<String, Object> svcParams = ValueUtil.newMap("mainBatch,newBatch", mainBatch, newBatch);
		this.customService.doCustomService(domainId, DIY_PRE_MERGE_BATCH, svcParams);
		
		// 2. 호기별로 배치 분리 설정인지 체크
		if(this.isSplitableBatch(newBatch)) {
			// 호기별 배치 분리 및 주문 분리
			throw ThrowUtil.newValidationErrorWithNoLog("병합하려는 배치의 주문에 여러 개의 랙이 포함되어 있어서 병합이 불가능합니다.");
		}
		
		// 3. 합포 주문 업데이트
		Map<String, Object> queryParams = ValueUtil.newMap("domainId,batchId,equipCd,equipNm,fromStatus,toStatus",
				domainId, newBatch.getId(), mainBatch.getEquipCd(), mainBatch.getEquipNm(), Order.STATUS_TYPE, Order.STATUS_WAIT);
		String sql = this.dpsBatchQueryStore.getDpsOrderStatusByInstruct();
		this.queryManager.executeBySql(sql, queryParams);
		
		// 4. 병합 주문의 배치 ID를 모두 메인 배치로 변경
		sql = "update orders set batch_id = :mainBatchId where domain_id = :domainId and batch_id = :mergedBatchId";
		this.queryManager.executeBySql(sql, ValueUtil.newMap("domainId,mainBatchId,mergedBatchId", domainId, mainBatch.getId(), newBatch.getId()));
		
		// 5. 작업 배치 업데이트
		newBatch.setBatchGroupId(mainBatch.getBatchGroupId());
		newBatch.setStatus(JobBatch.STATUS_MERGED);
		newBatch.setInstructedAt(new Date());
		this.queryManager.update(newBatch, "batchGroupId", "status", "instructedAt", "updatedAt");
		
		// 6. 메인 배치 업데이트
		this.updateBatchOrderCount(mainBatch);
		
		// 7. 병합 후 커스텀 서비스 호출
		Object retVal = this.customService.doCustomService(domainId, DIY_POST_MERGE_BATCH, svcParams);
		return this.returnValueToInt(retVal);
	}
	
	/**
	 * 배치 주문 수 업데이트
	 * 
	 * @param batch
	 */
	private void updateBatchOrderCount(JobBatch batch) {
		String sql = this.dpsBatchQueryStore.getDpsUpdateOrderCount();
		this.queryManager.executeBySql(sql, ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId()));
	}
	
	/**
	 * Object to integer
	 * 
	 * @param retVal
	 * @return
	 */
	private int returnValueToInt(Object retVal) {
		return (retVal == null) ? 0 : (retVal instanceof Integer) ? ValueUtil.toInteger(retVal) : 0;
	}
	
	/******************************************************************
	 * 							이벤트 전송
	/******************************************************************/
	
	/**
	 * 대상 분류 이벤트 전송
	 * 
	 * @param eventStep
	 * @param batch
	 * @param equipList
	 * @param params
	 * @return
	 */
	private EventResultSet publishClassificationEvent(short eventStep, JobBatch batch, List<?> equipList, Object... params) {
		return this.publishInstructEvent(EventConstants.EVENT_INSTRUCT_TYPE_CLASSIFICATION, eventStep, batch, equipList, params);
	}
	
	/**
	 * 박스 요청 이벤트 전송
	 * 
	 * @param batch
	 * @param equipList
	 * @param params
	 * @return
	 */
	private EventResultSet publishRequestBoxEvent(JobBatch batch, List<?> equipList, Object... params) {
		return this.publishInstructEvent(EventConstants.EVENT_INSTRUCT_TYPE_BOX_REQ, SysEvent.EVENT_STEP_ALONE, batch, equipList, params);
	}
	
	/**
	 * 추천 로케이션 이벤트 전송
	 * 
	 * @param eventStep
	 * @param batch
	 * @param equipList
	 * @param params
	 * @return
	 */
	private EventResultSet publishRecommendCellsEvent(short eventStep, JobBatch batch, List<?> equipList, Object... params) {
		return this.publishInstructEvent(EventConstants.EVENT_INSTRUCT_TYPE_RECOMMEND_CELLS, eventStep, batch, equipList, params);
	}
	
	/**
	 * 토털 피킹 이벤트 전송
	 * 
	 * @param eventStep
	 * @param mainBatch
	 * @param equipList
	 * @param params
	 * @return
	 */
	private EventResultSet publishTotalPickingEvent(short eventStep, JobBatch batch, List<?> equipList, Object... params) {
		return this.publishInstructEvent(EventConstants.EVENT_INSTRUCT_TYPE_TOTAL_PICKING, eventStep, batch, equipList, params);
	}

}
