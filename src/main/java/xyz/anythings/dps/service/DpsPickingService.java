package xyz.anythings.dps.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.ifc.IBucket;
import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.base.service.api.IPickingService;
import xyz.anythings.dps.DpsConstants;
import xyz.elidom.exception.server.ElidomRuntimeException;
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

	@Autowired 
	DpsJobStatusService dpsJobStatusService ;
	
	
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
		String orderId = this.beforeInputEmptyBucket(domainId, batch, isBox, bucket);

		
		// 3. 마지막 투입 조회
		JobInput input = this.dpsJobStatusService.findLatestInput(batch);
		
		
		// 3.1. 지시기 색상 결정 
		// TODO : 지시기 색상 찾기 서비스 필요 ( 일단은 버킷 색상으로 ... )
		String mpiColor = bucket.getBucketColor();
		
		/*
		// 4. 주문 번호로 매핑된 작업을 모두 조회
		JobParams condition = new JobParams();
		condition.setDomainId(domainId);
		condition.setJobType(batch.getJobType());
		condition.setBatchId(batch.getId());
		condition.setOrderId(orderId);
		List<JobProcess> jobList = this.searchJobListForMpiOn(condition);

		if(ValueUtil.isEmpty(jobList)) {
			// 투입 가능한 주문이 없습니다.
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_INPUT"));
		}
		
		// 5. 투입
		JobInputSeq input = this.doInput(batch, orderId, boxId, mpiColor, jobList);
		// 6. 박스 생성
		this.getMpsConfigurableService().startBoxing(this, jobList, null);
		// 7. 주문 상태 및 시간 업데이트
		this.afterInput(batch, input, jobList);
		*/
		// 8. 투입 정보 리턴
//		return jobList;
		return null;
		
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
		
		// 3. 박스와 매핑하고자 하는 주문 정보를 조회한다.
		String orderId = this.findNextMappingOrder(domainId, batch, bucket.getBucketType());
		if(ValueUtil.isEmpty(orderId)) {
			// 박스에 할당할 주문 정보가 존재하지 않습니다
			throw new ElidomRuntimeException(MessageUtil.getMessage("MPS_NO_ORDER_TO_ASSIGN_BOX"));
		}

		return orderId;		
	}
	
	private String findNextMappingOrder(Long domainId, JobBatch batch, String bucketType){
		
		return null;
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
