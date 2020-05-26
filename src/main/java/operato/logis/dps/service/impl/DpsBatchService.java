package operato.logis.dps.service.impl;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import operato.logis.dps.query.store.DpsBatchQueryStore;
import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.Order;
import xyz.anythings.base.event.main.BatchCloseEvent;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.service.api.IBatchService;
import xyz.anythings.base.service.impl.AbstractLogisService;
import xyz.anythings.sys.event.model.SysEvent;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 작업 배치 서비스
 * 
 * @author shortstop
 */
@Component("dpsBatchService")
public class DpsBatchService extends AbstractLogisService implements IBatchService {

	/**
	 * DPS 쿼리 스토어
	 */
	@Autowired
	private DpsBatchQueryStore dpsQueryStore;
		
	@Override
	public void isPossibleCloseBatch(JobBatch batch, boolean closeForcibly) {
		// 1. 배치 마감 전 처리 이벤트 전송
		BatchCloseEvent event = new BatchCloseEvent(batch, SysEvent.EVENT_STEP_BEFORE);
		event = (BatchCloseEvent)this.eventPublisher.publishEvent(event);
		
		// 2. 이벤트 취소라면 ...
		if(event.isAfterEventCancel()) {
			return;
		}
		
		// 1. 작업 배치 상태 체크
		if(ValueUtil.isNotEqual(batch.getStatus(), JobBatch.STATUS_RUNNING)) {
			// 진행 중인 작업배치가 아닙니다
			throw ThrowUtil.newStatusIsNotIng("terms.label.job_batch");
		}

		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId());
		condition.addFilter(new Filter("batchId", batch.getId()));

		// 2. batchId별 수신 주문이 존재하는지 체크
		int count = this.queryManager.selectSize(Order.class, condition);
		if(count == 0) {
			// 해당 배치의 주문정보가 없습니다 --> 주문을 찾을 수 없습니다.
			throw ThrowUtil.newNotFoundRecord("terms.label.order");
		}

		// 3. batchId별 작업 실행 데이터 체크
		count = this.queryManager.selectSize(JobInstance.class, condition);
		if(count == 0) {
			// 해당 배치의 작업실행 정보가 없습니다 --> 작업을 찾을 수 없습니다.
			throw ThrowUtil.newNotFoundRecord("terms.label.job");
		}

		// 4. batchId별 작업 실행 데이터 중에 완료되지 않은 것이 있는지 체크
		if(!closeForcibly) {
			condition.addFilter("status", OrmConstants.IN, LogisConstants.JOB_STATUS_WIPC);
			if(this.queryManager.selectSize(JobInstance.class, condition) > 0) {
				// {0} 등 {1}개의 호기에서 작업이 끝나지 않았습니다.
				String msg = MessageUtil.getMessage("ASSORTING_NOT_FINISHED_IN_RACKS", "{0} 등 {1}개의 호기에서 작업이 끝나지 않았습니다.", ValueUtil.toList(batch.getEquipCd(), "1"));
				throw ThrowUtil.newValidationErrorWithNoLog(msg);
			}
		}
	}

	@Override
	public void closeBatch(JobBatch batch, boolean forcibly) {
		// 1. 작업 마감 가능 여부 체크 
		this.isPossibleCloseBatch(batch, forcibly);

		// 2. 배치 마감 후 처리 이벤트 전송
		BatchCloseEvent event = new BatchCloseEvent(batch, SysEvent.EVENT_STEP_AFTER);
		event = (BatchCloseEvent)this.eventPublisher.publishEvent(event);
		
		// 3. 이벤트 취소라면 ...
		if(event.isAfterEventCancel()) {
			return;
		}
		
		// 4. 해당 배치에 대한 고정식이 아닌 호기들에 소속된 로케이션을 모두 찾아서 리셋
		this.resetRacksAndWorkCells(batch);

		// 5. OREDER_PREPROCESS 삭제
		this.deletePreprocess(batch);

		// 6. JobBatch 상태 변경
		this.updateJobBatchFinished(batch, new Date());
		
		// 7. 분류 서비스 배치 마감 API 호출 
		this.serviceDispatcher.getAssortService(batch).batchCloseAction(batch);
	}

	@Override
	public void isPossibleCloseBatchGroup(Long domainId, String batchGroupId, boolean closeForcibly) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int closeBatchGroup(Long domainId, String batchGroupId, boolean forcibly) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void isPossibleCancelBatch(JobBatch batch) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 해당 배치의 랙, 작업 셀 정보 리셋
	 *
	 * @param batch
	 */
	protected void resetRacksAndWorkCells(JobBatch batch) {
		Map<String, Object> params = ValueUtil.newMap("domainId,equipCd,batchId", batch.getDomainId(), batch.getEquipCd(), batch.getId());
	  	this.queryManager.executeBySql("UPDATE RACKS SET STATUS = null, BATCH_ID = null WHERE DOMAIN_ID = :domainId AND RACK_CD = :equipCd", params);
	  	this.queryManager.executeBySql("DELETE FROM WORK_CELLS WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId", params);
	}
	
	/**
	 * 해당 배치의 Free Cell 정보 리셋
	 *
	 * @param batch
	 */
	protected void resetFreeCells(JobBatch batch) {
		Map<String, Object> params = ValueUtil.newMap("domainId,equipType,equipCd", batch.getDomainId(), batch.getEquipType(), batch.getEquipCd());
	  	this.queryManager.executeBySql("UPDATE STOCKS SET STOCK_QTY = 0, LOAD_QTY = 0, ALLOC_QTY = 0, PICKED_QTY = 0 WHERE DOMAIN_ID = :domainId AND EQUIP_TYPE = :equipType AND EQUIP_CD = :equipCd", params);
	}
	
	/**
	 * 주문 가공 정보를 모두 삭제한다.
	 *
	 * @param batch
	 * @return
	 */
	protected void deletePreprocess(JobBatch batch) {
		this.queryManager.executeBySql("DELETE FROM ORDER_PREPROCESSES WHERE BATCH_ID= :batchId", ValueUtil.newMap("batchId", batch.getId()));
	}
	
	/**
	 * 작업 배치를 마감 처리
	 * 
	 * @param batch
	 * @param finishedAt
	 */
	protected void updateJobBatchFinished(JobBatch batch, Date finishedAt) {
		// 배치 마감을 위한 물량 주문 대비 최종 실적 요약 정보 조회
		String query = this.dpsQueryStore.getDpsBatchResultSummaryQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId());
		BatchProgressRate finalResult = this.queryManager.selectBySql(query, params, BatchProgressRate.class);
		
		// 작업 배치에 최종 결과 업데이트
		batch.setResultOrderQty(finalResult.getActualOrder());
		batch.setResultPcs(finalResult.getActualPcs());
		batch.setUph(finalResult.getUph()); 
		batch.setProgressRate(finalResult.getRatePcs());
		batch.setStatus(JobBatch.STATUS_END);
		batch.setFinishedAt(finishedAt);
		
		this.queryManager.update(batch, "resultOrderQty", "resultPcs", "progressRate", "uph", "status", "finishedAt");
	}

}
