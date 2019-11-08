package xyz.anythings.dps.service;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.ifc.IBucket;
import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.base.service.api.IPickingService;
import xyz.anythings.base.util.LogisEntityUtil;
import xyz.anythings.dps.DpsCodeConstants;
import xyz.anythings.dps.DpsConstants;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.ValueUtil;


/**
 * DPS 박스 처리 포함한 피킹 서비스 트랜잭션 구현 
 * @author yang
 *
 */
@Component("dpsPickingService")
public class DpsPickingService extends DpsClassificationService implements IPickingService{

	/***********************************************************************************************/
	/*   버킷 투입    */
	/***********************************************************************************************/

	
	/**
	 * 2-2. 투입 : 배치 작업에 공 박스 투입
	 * 
	 * @param inputEvent
	 * @return
	 */
	@Override
	@EventListener(condition = "#inputEvent.getInputType() == 'box', inputEvent.isForInspection() == false")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public Object inputEmptyBox(IClassifyInEvent inputEvent) {
		String boxId = inputEvent.getInputCode();
		JobBatch batch = inputEvent.getJobBatch();
		
		Long domainId = batch.getDomainId();
		
		return this.inputEmptyBucket(domainId, batch, true, boxId);
	}

	/**
	 * 2-3. 투입 : 배치 작업에 공 트레이 투입
	 * 
	 * @param inputEvent
	 * @return
	 */
	@Override
	@EventListener(condition = "#inputEvent.getInputType() == 'tray', inputEvent.isForInspection() == false")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public Object inputEmptyTray(IClassifyInEvent inputEvent) {
		String trayCd = inputEvent.getInputCode();
		JobBatch batch = inputEvent.getJobBatch();		
		
		Long domainId = batch.getDomainId();
		
		return this.inputEmptyBucket(domainId, batch, false, trayCd);
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
		List<JobInstance> jobList = LogisEntityUtil.searchEntitiesBy(domainId, false, JobInstance.class, null, "batchId,jobType,orderNo", batch.getId(), batch.getJobType(),orderNo);

		if(ValueUtil.isEmpty(jobList)) {
			// 투입 가능한 주문이 없습니다.
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_INPUT"));
		}
		
		// 5. boxPackId 생성 
		String boxPackId = AnyValueUtil.newUuid36();
		
		// 5. 투입
		JobInput jobInput = this.doInputEmptyBucket(domainId, batch, orderNo, bucket, indColor, jobList, boxPackId);
		
		// 6. 박스 BoxPack, BoxItem 생성
		this.createBoxPackData(domainId, batch, bucket, orderNo, boxPackId);
		
		// 7. 박스 투입 후 액션 
		this.afterInputEmptyBucket(domainId, batch, jobInput, jobList);
		// 8. 투입 정보 리턴
		return jobList;
	}
	
	private void createBoxPackData(Long domainId, JobBatch batch, IBucket bucket, String orderNo, String boxPackId) {
		// 2. boxItems 생성 
		this.dpsBoxingService.createBoxItemsDataByOrder(domainId, batch, orderNo, boxPackId);
		
		// 3. boxItem 정보로 boxPack 생성  
		this.dpsBoxingService.createBoxPackDataByBoxItems(domainId, batch, orderNo, bucket.getBucketType(), bucket.getBucketTypeCd(), boxPackId);
	}
	
	/**
	 * 박스 투입 후 액션 
	 * @param domainId
	 * @param batch
	 * @param jobInput
	 * @param jobList
	 */
	private void afterInputEmptyBucket(Long domainId, JobBatch batch, JobInput jobInput, List<JobInstance> jobList) {
		// 투입된 호기에만 리프레쉬 메시지 전송
	}
	
	/**
	 * 박스 혹은 트레이를 작업에 투입 
	 * @param domainId
	 * @param batch
	 * @param orderNo
	 * @param bucket
	 * @param indColor
	 * @param jobList
	 * @return
	 */
	private JobInput doInputEmptyBucket(Long domainId, JobBatch batch, String orderNo, IBucket bucket, String indColor, List<JobInstance> jobList, String boxPackId) {
		// 1. 다음 투입 번호
		int newSeq = this.dpsJobStatusService.findNextInputSeq(batch);

		// 2. 새로운 투입 정보 생성
		JobInput jobInput = new JobInput();
		jobInput.setDomainId(domainId);
		jobInput.setBatchId(batch.getId());
		jobInput.setEquipType(batch.getEquipType());
		jobInput.setEquipCd(batch.getEquipCd());
		jobInput.setInputSeq(newSeq);
		jobInput.setComCd(batch.getComCd());
		jobInput.setOrderNo(orderNo);
		jobInput.setBoxId(bucket.getBucketCd());
		jobInput.setBoxType(bucket.getBucketType());
		jobInput.setColorCd(indColor);
		jobInput.setInputType(DpsCodeConstants.JOB_INPUT_TYPE_PCS); // TODO : config 처리 필요 
		jobInput.setStatus(DpsCodeConstants.JOB_INPUT_STATUS_WAIT);

		this.queryManager.insert(jobInput);
		
		// 3. 주문 - 박스 ID 매핑 
		String currentTime = DateUtil.currentTimeStr();
		for(JobInstance job : jobList) {
			job.setInputSeq(batch.getLastInputSeq());
			job.setBoxId(bucket.getBucketCd());
			job.setColorCd(indColor);
			job.setStatus(DpsConstants.JOB_STATUS_INPUT);
			job.setInputAt(currentTime);
			job.setBoxPackId(boxPackId);
		}
		
		this.queryManager.updateBatch(jobList, "inputSeq","boxId","colorCd","status","inputAt","boxPackId");

		// 4. 새로운 투입 순서 리턴 
		return jobInput;
	}
	
	
	/**
	 * 버킷 투입 전 액션 
	 * @param domainId
	 * @param batch
	 * @param isBox
	 * @param bucket
	 * @return
	 */
	private String beforeInputEmptyBucket(Long domainId, JobBatch batch, boolean isBox, IBucket bucket) {
		
		// 1.버킷의 투입 가능 여부 확인 
		boolean usedBox = false;
		if(isBox) {
			// 1.1 박스는 쿼리를 해서 확인 
			usedBox = this.checkUniqueBoxId(domainId, batch, bucket.getBucketCd());
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
		
		// TODO 현재는 주문에만 맵핑 하도록 구현 
		// 추후 맵핑 컬럼 적용 필요함. 
		// 3. 박스와 매핑하고자 하는 작업 정보를 조회한다.
		String nextOrderId = this.findNextMappingJob(domainId, batch, bucket.getBucketTypeCd()
				, DpsCodeConstants.DPS_PREPROCESS_COL_ORDER, DpsCodeConstants.DPS_ORDER_TYPE_MT);
		
		if(ValueUtil.isEmpty(nextOrderId)) {
			// 박스에 할당할 주문 정보가 존재하지 않습니다
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_ASSIGN_BOX"));
		}
		
		// 3. 이미 처리된 주문인지 한 번 더 체크
		if(LogisEntityUtil.selectSizeByEntity(domainId, JobInput.class, "batchId,orderNo", batch.getId(), nextOrderId)>0) {
			// 주문 은(는) 이미 투입 상태입니다
			throw new ElidomRuntimeException(ThrowUtil.translateMessage("A_ALREADY_B_STATUS", "terms.label.order", "terms.label.input"));
		}
		
		return nextOrderId;		
	}
	
	/***********************************************************************************************/
	/*   소분류   */
	/***********************************************************************************************/


	/**
	 * 3-1. 소분류 : 피킹 작업 확정 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	public void confirmPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 3-2. 소분류 : 피킹 취소 (예정 수량보다 분류 처리할 실물이 작아서 처리할 수 없는 경우 취소 처리)
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	public void cancelPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 3-3. 소분류 : 수량을 조정하여 분할 피킹 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	public int splitPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * 3-4. 소분류 : 피킹 확정 처리된 작업 취소
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	public int undoPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return 0;
	}
}
