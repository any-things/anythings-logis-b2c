package xyz.anythings.dps.service;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.ifc.IBucket;
import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.dps.service.impl.AbstractDpsPickingService;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.elidom.exception.server.ElidomRuntimeException;
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
	@EventListener(condition = "#inputEvent.getInputType() == 'box' and inputEvent.isForInspection() == false")
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
	@EventListener(condition = "#inputEvent.getInputType() == 'tray' and inputEvent.isForInspection() == false")
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
		this.doInputEmptyBucket(domainId, batch, orderNo, bucket, indColor, jobList, boxPackId);
		
		// 7. 박스 BoxPack, BoxItem 생성
		this.createBoxPackData(domainId, batch, bucket, orderNo, boxPackId);
		
		// 8. 박스 투입 후 액션 
		this.afterInputEmptyBucket(domainId, batch, bucket, jobList);
		// 9. 투입 정보 리턴
		return jobList;
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
	public void confirmPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
	}

	/**
	 * 3-4. 소분류 : 피킹 취소 (예정 수량보다 분류 처리할 실물이 작아서 처리할 수 없는 경우 취소 처리)
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	public void cancelPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
	}

	/**
	 * 3-5. 소분류 : 수량을 조정하여 분할 피킹 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	public int splitPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return 0;
	}


}
