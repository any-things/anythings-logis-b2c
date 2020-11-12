package operato.logis.dps.service.impl;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import operato.logis.dps.DpsCodeConstants;
import operato.logis.dps.DpsConstants;
import operato.logis.dps.service.api.IDpsPickingService;
import operato.logis.dps.service.util.DpsBatchJobConfigUtil;
import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.Cell;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.Rack;
import xyz.anythings.base.entity.ifc.IBucket;
import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.gw.service.util.BatchIndConfigUtil;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.ValueUtil;
import xyz.elidom.util.DateUtil;

/**
 * DPS 박스 처리 포함한 피킹 서비스 트랜잭션 구현
 * 
 * @author yang
 */
@Component("dpsPickingService")
public class DpsPickingService extends AbstractPickingService implements IDpsPickingService {

	/************************************************************************************************/
	/*   									버킷 투입													*/ 
	/************************************************************************************************/

	/**
	 * 2-2. 분류 설비에 박스 투입 처리
	 * 
	 * @param inputEvent
	 */
	@Override
	public Object input(IClassifyInEvent inputEvent) {
		
		String boxId = inputEvent.getInputCode();
		JobBatch batch = inputEvent.getJobBatch();
		String boxType = DpsBatchJobConfigUtil.getInputBoxType(batch);
		boolean isBox = ValueUtil.isEqualIgnoreCase(boxType, DpsCodeConstants.BOX_TYPE_BOX);
		Object retValue = this.inputEmptyBucket(batch, isBox, boxId);
		inputEvent.setResult(retValue);
		inputEvent.setExecuted(true);
		return retValue;
	}
	
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
		Object retValue = this.inputEmptyBucket(batch, true, boxId);
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
		Object retValue = this.inputEmptyBucket(batch, false, trayCd);
		inputEvent.setResult(retValue);
		inputEvent.setExecuted(true);
		return retValue;
	}
	
	/**
	 * 합포 박스 또는 트레이 투입
	 * 
	 * @param batch
	 * @param isBox
	 * @param bucketCd
	 * @param params
	 * @return
	 */
	@Override
	public Object inputEmptyBucket(JobBatch batch, boolean isBox, String bucketCd, Object... params) {
				
		// 1. 투입 가능한 버킷인지 체크 (박스 or 트레이) 
		//    -> 박스 타입이면 박스 타입에 락킹 (즉 동일 박스 타입의 박스는 동시에 하나씩만 투입 가능) / 트레이 타입이면 버킷에 락킹 (하나의 버킷은 한 번에 하나만 투입 가능)
		IBucket bucket = this.vaildInputBucketByBucketCd(batch, bucketCd, isBox, true);
		
		// 2. 박스 투입 전 체크 - 주문 번호 조회 
		String orderNo = this.beforeInputEmptyBucket(batch, isBox, bucket);

		// 3. 표시기 색상 결정
		String indColor = ValueUtil.isEmpty(bucket.getBucketColor()) ? BatchIndConfigUtil.getDpsJobColor(batch.getId()) : bucket.getBucketColor();
		
		// 4. 주문 번호로 매핑된 작업을 모두 조회
		if(this.dpsJobStatusService == null) this.getJobStatusService(batch);
		List<JobInstance> jobList = this.dpsJobStatusService.searchPickingJobList(batch, null, orderNo);

		if(ValueUtil.isEmpty(jobList)) {
			// 투입 가능한 주문이 없습니다.
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_INPUT"));
		}
		
		// 5. 작업 데이터에 박스 ID 설정
		for(JobInstance job : jobList) {
			job.setBoxId(bucketCd);
		}
				
		// 6. 박스 마스터 & 내품 내역 생성
		if(this.dpsBoxingService == null) this.getBoxingService();
		BoxPack box = this.dpsBoxingService.fullBoxing(batch, null, jobList);
		
		// 7. 투입
		this.doInputEmptyBucket(batch, orderNo, bucket, indColor, box.getId());
		
		// 8. 박스 투입 후 액션 
		this.afterInputEmptyBucket(batch, bucket, orderNo);
		
		// 9. 투입 정보 리턴
		return jobList;
	}
	
	/**
	 * 단포 박스 또는 트레이 투입
	 * 
	 * @param batch
	 * @param isBox
	 * @param skuCd
	 * @param bucketCd
	 * @param params
	 * @return
	 */
	@Override
	public Object inputSinglePackEmptyBucket(JobBatch batch, boolean isBox, String skuCd, String bucketCd, Object... params) {
		
		// 1. 단포 전용 호기 Lock
		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId());
		condition.addFilter("areaCd", batch.getAreaCd());
		condition.addFilter("stageCd", batch.getStageCd());
		condition.addFilter("rackType", DpsCodeConstants.DPS_RACK_TYPE_OT);
		this.queryManager.selectListWithLock(Rack.class, condition);
		
		// 2. 버킷 조회 (박스 or 트레이)
		IBucket bucket = this.vaildInputBucketByBucketCd(batch, bucketCd, isBox, false);
		
		// 3. 버킷 투입 전 체크 - 작업 ID  
		JobInstance job = this.beforeInputSinglePackEmptyBucket(batch, isBox, bucket);

		if(job == null) {
			// 투입 가능한 주문이 없습니다.
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_INPUT"));
		}
		
		// 4. 기존 작업에 대한 재 작업인 경우 
		if(ValueUtil.isEqualIgnoreCase(job.getBoxId(), bucket.getBucketCd())) {
			return job;
		}
		
		// 5. 작업 정보 업데이트
		String indColor = ValueUtil.isEmpty(bucket.getBucketColor()) ? BatchIndConfigUtil.getDpsJobColor(batch.getId()) : bucket.getBucketColor();
		job.setColorCd(indColor);
		job.setBoxTypeCd(bucket.getBucketTypeCd());
		job.setBoxId(bucket.getBucketCd());
		job.setBoxPackId(AnyValueUtil.newUuid36());
		job.setStatus(LogisConstants.JOB_STATUS_PICKING);
		job.setInputAt(DateUtil.currentTimeStr());
		this.queryManager.update(job, "colorCd", "boxTypeCd", "boxId", "boxPackId", "status", "inputAt", "updaterId", "updatedAt");
		
		// 6. 박스 마스터 & 내품 내역 생성
		this.getBoxingService().fullBoxing(batch, null, ValueUtil.toList(job), job.getBoxPackId());
		
		// 7. 박스 투입 후 액션 
		this.afterInputEmptyBucket(batch, bucket, job.getOrderNo());
		
		// 8. 작업 리턴
		return job;
	}
	
	/***********************************************************************************************/
	/*   									소분류   												   */
	/***********************************************************************************************/

	/**
	 * 3-3. 소분류 : 피킹 작업 확정 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	@EventListener(condition = "#exeEvent.getClassifyAction() == 'ok' and #exeEvent.isExecuted() == false and #exeEvent.getJobType() == 'DPS'")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void confirmPick(IClassifyRunEvent exeEvent) {
		
		JobBatch batch = exeEvent.getJobBatch();
		// 1. JobInstance 조회 
		JobInstance job = exeEvent.getJobInstance();
		// 2. 확정 처리 
		this.confirmPick(batch, job, exeEvent.getResQty());
		// 3. 실행 여부 체크
		exeEvent.setExecuted(true);
	}
	
	/**
	 * 작업 확정 처리
	 * 
	 * @param batch
	 * @param job
	 * @param resQty
	 */
	@Override
	public void confirmPick(JobBatch batch, JobInstance job, int resQty) {
		// 1. 작업 상태 체크
		if(ValueUtil.isNotEqual(job.getStatus(), DpsConstants.JOB_STATUS_PICKING)) {
			throw new ElidomServiceException("확정 처리는 피킹중 상태에서만 가능합니다.");
		}
		
		// 2. 합포의 경우에 CELL 사용 
		Long domainId = batch.getDomainId();
		Cell cell = (ValueUtil.isEqualIgnoreCase(job.getOrderType(), DpsCodeConstants.DPS_ORDER_TYPE_MT)) ? 
				    AnyEntityUtil.findEntityBy(domainId, true, Cell.class, null, "equipType,equipCd,cellCd", job.getEquipType(), job.getEquipCd(), job.getSubEquipCd()) : null;
				
		// 3. 작업 처리 전 액션 
		int pickQty = this.beforeConfirmPick(batch, job, cell, resQty);
		
		if(pickQty > 0) {
			// 4. 분류 작업 처리
			this.doConfirmPick(batch, job, cell, pickQty);
			// 5. 작업 처리 후 액션
			this.afterComfirmPick(batch, job, cell, pickQty);
		}
	}

	/**
	 * 3-4. 소분류 : 피킹 취소 (예정 수량보다 분류 처리할 실물이 작아서 처리할 수 없는 경우 취소 처리)
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	@EventListener(condition = "#exeEvent.getClassifyAction() == 'cancel' and #exeEvent.isExecuted() == false and #exeEvent.getJobType() == 'DPS'")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void cancelPick(IClassifyRunEvent exeEvent) {
		// TODO
		exeEvent.setExecuted(true);
	}

	/**
	 * 3-5. 소분류 : 수량을 조정하여 분할 피킹 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	@EventListener(condition = "#exeEvent.getClassifyAction() == 'modify' and #exeEvent.isExecuted() == false and #exeEvent.getJobType() == 'DPS'")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public int splitPick(IClassifyRunEvent exeEvent) {
		// TODO 
		exeEvent.setExecuted(true);
		return 0;
	}

}
