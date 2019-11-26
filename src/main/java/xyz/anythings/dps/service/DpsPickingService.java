package xyz.anythings.dps.service;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.Cell;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.Rack;
import xyz.anythings.base.entity.ifc.IBucket;
import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.dps.DpsCodeConstants;
import xyz.anythings.dps.DpsConstants;
import xyz.anythings.dps.service.impl.AbstractDpsPickingService;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.ValueUtil;


/**
 * DPS 박스 처리 포함한 피킹 서비스 트랜잭션 구현 
 * @author yang
 *
 */
@Component("dpsPickingService")
public class DpsPickingService extends AbstractDpsPickingService{

	/***********************************************************************************************/
	/*   버킷 투입    */
	/***********************************************************************************************/


	/**
	 * 2-3. 투입 : 배치 작업에 공 박스 투입
	 * 
	 * @param inputEvent
	 * @return
	 */
	@Override
	@EventListener(condition = "#inputEvent.getInputType() == 'box' and #inputEvent.isForInspection() == false and #inputEvent.isExecuted() == false")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public Object inputEmptyBox(IClassifyInEvent inputEvent) {
		String boxId = inputEvent.getInputCode();
		JobBatch batch = inputEvent.getJobBatch();

		Long domainId = batch.getDomainId();
		
		Object retValue = this.inputEmptyBucket(domainId, batch, true, boxId);
		
		inputEvent.setResult(retValue);
		inputEvent.setExecuted(true);
		
		return retValue;
	}

	/**
	 * 2-3. 투입 : 배치 작업에 공 트레이 투입
	 * 
	 * @param inputEvent
	 * @return
	 */
	@Override
	@EventListener(condition = "#inputEvent.getInputType() == 'tray' and #inputEvent.isForInspection() == false and #inputEvent.isExecuted() == false")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public Object inputEmptyTray(IClassifyInEvent inputEvent) {
		String trayCd = inputEvent.getInputCode();
		JobBatch batch = inputEvent.getJobBatch();		
		
		Long domainId = batch.getDomainId();
		
		Object retValue = this.inputEmptyBucket(domainId, batch, false, trayCd);
		
		inputEvent.setResult(retValue);
		inputEvent.setExecuted(true);
		
		return retValue;
	}
	
	
	/**
	 * 박스 또는 트레이 투입
	 * @param domainId
	 * @param batch
	 * @param isBox
	 * @param bucketCd
	 * @return
	 */
	public Object inputEmptyBucket(Long domainId, JobBatch batch, boolean isBox, String bucketCd, Object... params) {
		
		// 1. 투입 가능한 버킷인지 체크 ( 박스 or 트레이 )
		//  - 박스 타입이면 박스 타입에 락킹 - 즉 동일 박스 타입의 박스는 동시에 하나씩만 투입 가능하다.
		//  - 트레이 타입이면 버킷에 락킹 - 하나의 버킷은 당연히 한 번에 하나만 투입가능하다.
		IBucket bucket = this.vaildInputBucketByBucketCd(domainId, batch, bucketCd, isBox, true);
		
		
		// 2. 박스 투입 전 체크 - 주문 번호 조회 
		String orderNo = this.beforeInputEmptyBucket(domainId, batch, isBox, bucket);

		// 3. 마지막 투입 조회
		//JobInput latestInput = this.dpsJobStatusService.findLatestInput(batch);
		// 3.1. 지시기 색상 결정 
		// TODO : 지시기 색상 찾기 서비스 필요 ( 일단은 버킷 색상으로 ... )
		String indColor = bucket.getBucketColor();
		
		// 4. 주문 번호로 매핑된 작업을 모두 조회
		List<JobInstance> jobList = AnyEntityUtil.searchEntitiesBy(domainId, false, JobInstance.class, null, "batchId,jobType,orderNo", batch.getId(), batch.getJobType(),orderNo);

		if(ValueUtil.isEmpty(jobList)) {
			// 투입 가능한 주문이 없습니다.
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_INPUT"));
		}
		
		// 5. boxPackId 생성 
		String boxPackId = AnyValueUtil.newUuid36();
		
		// 6. 투입
		this.doInputEmptyBucket(domainId, batch, orderNo, bucket, indColor, boxPackId);
		
		// 7. 박스 BoxPack, BoxItem 생성
		this.createBoxPackData(domainId, batch, bucket, orderNo, boxPackId);
		
		// 8. 박스 투입 후 액션 
		this.afterInputEmptyBucket(domainId, batch, bucket, orderNo);
		// 9. 투입 정보 리턴
		return jobList;
	}
	
	
	
	/**
	 * 단포 박스 또는 트레이 투입
	 * @param domainId
	 * @param batch
	 * @param isBox
	 * @param bucketCd
	 * @return
	 */
	public Object inputSinglePackEmptyBucket(Long domainId, JobBatch batch, boolean isBox, String skuCd, String bucketCd, Object... params) {
		// 1. 단포 전용 호기 Lock
		Query condition = AnyOrmUtil.newConditionForExecution(domainId);
		condition.addFilter("areaCd", batch.getAreaCd());
		condition.addFilter("stageCd", batch.getStageCd());
		condition.addFilter("rackType", DpsCodeConstants.DPS_RACK_TYPE_OT );
		this.queryManager.selectListWithLock(Rack.class, condition);
		
		// 2. 버킷  조회 ( 박스 or 트레이 )
		IBucket bucket = this.vaildInputBucketByBucketCd(domainId, batch, bucketCd, isBox, false);
		
		// 3. 버킷  투입 전 체크 - 작업 ID  
		JobInstance job = this.beforeInputSinglePackEmptyBucket(domainId, batch, isBox, bucket);

		if(ValueUtil.isEmpty(job)) {
			// 투입 가능한 주문이 없습니다.
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_INPUT"));
		}
		
		// 3.1 기존 작업에 대한 재작업인 경우 
		if(ValueUtil.isEqualIgnoreCase(job.getBoxId(), bucket.getBucketCd())) {
			return job;
		}
		
		// 4. boxPackId 생성 
		String boxPackId = AnyValueUtil.newUuid36();
		
		// 5. 투입
		this.doInputSinglePackEmptyBucket(domainId, batch, job.getOrderNo(), bucket, bucket.getBucketColor(), boxPackId);

		// 6. 박스 BoxPack, BoxItem 생성
		this.createBoxPackData(domainId, batch, bucket, job.getOrderNo(), boxPackId);
		
		// 7. 박스 투입 후 액션 
		this.afterInputEmptyBucket(domainId, batch, bucket, job.getOrderNo());
		
		job.setStatus("P");
		job.setBoxId(bucket.getBucketCd());
		
		return job;
	}

	
	/***********************************************************************************************/
	/*   소분류   */
	/***********************************************************************************************/


	/**
	 * 3-3. 소분류 : 피킹 작업 확정 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	@EventListener(condition = "#exeEvent.getClassifyAction() == 'ok' and #exeEvent.isExecuted() == false and #exeEvent.getJobType() == 'DPS' ")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void confirmPick(IClassifyRunEvent exeEvent) {
		
		JobBatch batch = exeEvent.getJobBatch();
		Long domainId = batch.getDomainId();
		
		// 1. JobInstance 조회 
		JobInstance job = exeEvent.getJobInstance();
		
		// 2. 확정 처리 
		this.confirmPick(domainId,batch,job,exeEvent.getResQty());
		
		exeEvent.setExecuted(true);
	}
	
	/**
	 * 작업 확정 처리 
	 * @param domainId
	 * @param batch
	 * @param job
	 * @param resQty
	 */
	public void confirmPick(Long domainId, JobBatch batch, JobInstance job, int resQty) {
		
		if(ValueUtil.isEqualIgnoreCase(job.getStatus(), DpsConstants.JOB_STATUS_PICKING) == false) {
			throw new ElidomServiceException("확정 처리는 피킹중 상태에서만 가능합니다.");
		}
		
		// 2. Cell 조회 
		Cell cell = null;
		
		// 2.1 합포의 경우에만 CELL 사용 
		if(ValueUtil.isEqualIgnoreCase(job.getOrderType(), DpsCodeConstants.DPS_ORDER_TYPE_MT)) {
			cell = AnyEntityUtil.findEntityBy(domainId, true, Cell.class, null, "equipType,equipCd,cellCd", job.getEquipType(),job.getEquipCd(),job.getSubEquipCd());
		}
		
		// 3. 작업 처리 전 액션 
		int pickQty = this.beforeConfirmPick(domainId, batch, job, cell, resQty);
		
		if(pickQty > 0) {
			// 4. 분류 작업 처리
			this.doConfirmPick(domainId, batch, job, cell, pickQty);
			// 5. 작업 처리 후 액션
			this.afterComfirmPick(domainId, batch, job, cell, pickQty);
		}
	}

	/**
	 * 3-4. 소분류 : 피킹 취소 (예정 수량보다 분류 처리할 실물이 작아서 처리할 수 없는 경우 취소 처리)
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	@EventListener(condition = "#exeEvent.getClassifyAction() == 'cancel' and #exeEvent.isExecuted() == false and #exeEvent.getJobType() == 'DPS' ")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void cancelPick(IClassifyRunEvent exeEvent) {
		exeEvent.setExecuted(true);
	}

	/**
	 * 3-5. 소분류 : 수량을 조정하여 분할 피킹 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	@EventListener(condition = "#exeEvent.getClassifyAction() == 'modify' and #exeEvent.isExecuted() == false and #exeEvent.getJobType() == 'DPS' ")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public int splitPick(IClassifyRunEvent exeEvent) {
		exeEvent.setExecuted(true);
		return 0;
	}
}
