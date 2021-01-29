package operato.logis.dps.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import operato.logis.dps.query.store.DpsBatchQueryStore;
import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.Cell;
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
		
		// 3. 작업 배치 상태 체크
		if(ValueUtil.isNotEqual(batch.getStatus(), JobBatch.STATUS_RUNNING)) {
			// 진행 중인 작업배치가 아닙니다
			throw ThrowUtil.newStatusIsNotIng("terms.label.job_batch");
		}

		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId());
		condition.addFilter(new Filter("batchId", batch.getId()));

		// 4. batchId별 수신 주문이 존재하는지 체크
		int count = this.queryManager.selectSize(Order.class, condition);
		if(count == 0) {
			// 해당 배치의 주문정보가 없습니다 --> 주문을 찾을 수 없습니다.
			throw ThrowUtil.newNotFoundRecord("terms.label.order");
		}

		// 5. batchId별 작업 실행 데이터 체크
		count = this.queryManager.selectSize(JobInstance.class, condition);
		if(count == 0) {
			// 해당 배치의 작업실행 정보가 없습니다 --> 작업을 찾을 수 없습니다.
			throw ThrowUtil.newNotFoundRecord("terms.label.job");
		}

		// 6. batchId별 작업 실행 데이터 중에 완료되지 않은 것이 있는지 체크
		if(!closeForcibly) {
			condition.addFilter("status", OrmConstants.IN, LogisConstants.JOB_STATUS_WIPC);
			if(this.queryManager.selectSize(JobInstance.class, condition) > 0) {
				// {0} 등 {1}개의 호기에서 작업이 끝나지 않았습니다.
				String msg = MessageUtil.getMessage("ASSORTING_NOT_FINISHED_IN_RACKS", "{0} 등 {1}개의 랙에서 작업이 끝나지 않았습니다.", ValueUtil.toList(batch.getEquipCd(), "1"));
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
		
		// 4. 재고 리셋
		this.resetFreeCells(batch);
		
		// 5. 해당 배치에 대한 고정식이 아닌 호기들에 소속된 로케이션을 모두 찾아서 리셋
		this.resetRacksAndWorkCells(batch);

		// 6. OREDER_PREPROCESS 삭제
		this.deletePreprocess(batch);

		// 7. JobBatch 상태 변경
		this.updateJobBatchFinished(batch, new Date());
		
		// 8. 분류 서비스 배치 마감 API 호출
		this.serviceDispatcher.getClassificationService(batch).batchCloseAction(batch);
	}

	@Override
	public void isPossibleCloseBatchGroup(Long domainId, String batchGroupId, boolean closeForcibly) {
		// 1. 작업 배치 상태 체크
		String sql = "select id from job_batches where domain_id = :domainId and batch_group_id = :batchGroupId and status = :runStatus";
		Map<String, Object> params = ValueUtil.newMap("domainId,batchGroupId,runStatus", domainId, batchGroupId, JobBatch.STATUS_RUNNING);
		int runCount = this.queryManager.selectSizeBySql(sql, params);
		
		if(runCount == 0) {
			// 진행 중인 작업배치가 아닙니다
			throw ThrowUtil.newStatusIsNotIng("terms.label.job_batch");
		}

		// 2. batchId별 작업 실행 데이터 중에 완료되지 않은 것이 있는지 체크
		if(!closeForcibly) {
			// batchId별 작업 실행 데이터 체크
			sql = "select distinct equip_cd from job_instances where domain_id = :domainId and batch_id in (select id from job_batches where domain_id = :domainId and batch_group_id = :batchGroupId) and status in (:statuses) order by equip_cd asc";
			params.put("statuses", LogisConstants.JOB_STATUS_WIPC);
			List<String> equipCdList = this.queryManager.selectListBySql(sql, params, String.class, 0, 0);
			
			// batchId별 작업 실행 데이터 체크
			if(!equipCdList.isEmpty()) {
				// {0} 등 {1}개의 호기에서 작업이 끝나지 않았습니다.
				String msg = MessageUtil.getMessage("ASSORTING_NOT_FINISHED_IN_RACKS", "{0} 등 {1}개의 호기에서 작업이 끝나지 않았습니다.", ValueUtil.toList(equipCdList.get(0), ValueUtil.toString(equipCdList.size())));
				throw ThrowUtil.newValidationErrorWithNoLog(msg);
			}
		}
	}

	@Override
	public int closeBatchGroup(Long domainId, String batchGroupId, boolean forcibly) {
		String sql = "select id from job_batches where domain_id = :domainId and batch_group_id = :batchGroupId and status = :runStatus";
		Map<String, Object> params = ValueUtil.newMap("domainId,batchGroupId,runStatus", domainId, batchGroupId, JobBatch.STATUS_RUNNING);
		List<JobBatch> batchList = this.queryManager.selectListBySql(sql, params, JobBatch.class, 0, 0);
		
		for(JobBatch batch : batchList) {
			this.closeBatch(batch, forcibly);
		}
		
		return 0;
	}

	@Override
	public void isPossibleCancelBatch(JobBatch batch) {
		// 작업 지시 취소 조건 체크
		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId());
		condition.addFilter("batchId", batch.getId());
		condition.addFilter(new Filter("pickedQty", OrmConstants.GREATER_THAN, 0));
		
		if(this.queryManager.selectSize(JobInstance.class, condition) > 0) {
			throw ThrowUtil.newValidationErrorWithNoLog(true, "MPS_NOT_ALLOWED_CANCEL_AFTER_START_JOB"); // 분류 작업시작 이후여서 취소가 불가능합니다
		}
	}

	/**
	 * 해당 배치의 랙, 작업 셀 정보 리셋
	 *
	 * @param batch
	 */
	protected void resetRacksAndWorkCells(JobBatch batch) {
		Map<String, Object> params = ValueUtil.newMap("domainId,equipCd,batchId", batch.getDomainId(), batch.getEquipCd(), batch.getId());
	  	this.queryManager.executeBySql("UPDATE RACKS SET STATUS = null, BATCH_ID = null WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId", params);
	  	this.queryManager.executeBySql("DELETE FROM WORK_CELLS WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId", params);
	}
	
	/**
	 * 해당 배치의 Free Cell 정보 리셋
	 *
	 * @param batch
	 */
	protected void resetFreeCells(JobBatch batch) {
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType,fixedFlag", batch.getDomainId(), batch.getId(), batch.getEquipType(), false);
		this.queryManager.executeBySql("UPDATE STOCKS SET SKU_CD = null, SKU_BARCD = null, SKU_NM = null, STOCK_QTY = 0, LOAD_QTY = 0, ALLOC_QTY = 0, PICKED_QTY = 0 WHERE DOMAIN_ID = :domainId AND EQUIP_TYPE = :equipType AND EQUIP_CD in (select rack_cd from racks where domain_id = :domainId and batch_id = :batchId) AND (FIXED_FLAG IS NULL OR FIXED_FLAG = :fixedFlag)", params);
	}
	
	/**
	 * 주문 가공 정보를 모두 삭제한다.
	 *
	 * @param batch
	 * @return
	 */
	protected void deletePreprocess(JobBatch batch) {
		this.queryManager.executeBySql("DELETE FROM ORDER_PREPROCESSES WHERE BATCH_ID = :batchId", ValueUtil.newMap("batchId", batch.getId()));
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
		batch.setResultPcs(finalResult.getActualPcs());
		batch.setResultOrderQty(finalResult.getActualOrder());
		batch.setResultBoxQty(finalResult.getActualSku());
		batch.setUph(finalResult.getUph());
		batch.setProgressRate(finalResult.getRateOrder());
		batch.setEquipRuntime(finalResult.getRateSku());
		batch.setStatus(JobBatch.STATUS_END);
		batch.setFinishedAt(finishedAt);
		this.queryManager.update(batch, "resultOrderQty", "resultBoxQty", "resultPcs", "progressRate", "uph", "equipRuntime", "status", "finishedAt");
	}

	@Override
	public void isPossibleChangeEquipment(JobBatch batch, String toEquipCd) {
		// 1. 랙이 다른 랙인지 체크
		String fromRackCd = batch.getEquipCd();
		if(ValueUtil.isNotEmpty(fromRackCd) && ValueUtil.isEqualIgnoreCase(batch.getEquipCd(), toEquipCd)) {
			throw ThrowUtil.newValidationErrorWithNoLog("현재 배치의 랙과 다른 랙을 선택하세요.");
		}
		
		// 2. 배치 상태 체크
		String status = batch.getStatus();
		if(ValueUtil.isNotEqual(status, JobBatch.STATUS_WAIT) && ValueUtil.isNotEqual(status, JobBatch.STATUS_RECEIVE)) {
			throw ThrowUtil.newValidationErrorWithNoLog("작업 진행 전에만 랙 전환이 가능합니다.");
		}
		
		// 3. From Cell Count
		String sql = "select count(*) from cells where domain_id = :domainId and equip_type = :equipType and equip_cd = :equipCd and active_flag = :activeFlag";
		Map<String, Object> condition = ValueUtil.newMap("domainId,equipType,equipCd,activeFlag", batch.getDomainId(), batch.getEquipType(), batch.getEquipCd(), true);
		int fromCellCount = this.queryManager.selectBySql(sql, condition, Integer.class);
		
		// 4. To Cell Count
		sql = "select count(*) from cells where domain_id = :domainId and equip_type = :equipType and equip_cd = :equipCd and active_flag = :activeFlag";
		condition.put("equipCd", toEquipCd);
		int toCellCount = this.queryManager.selectBySql(sql, condition, Integer.class);
		
		// 5. 셀 개수 체크
		if(fromCellCount != toCellCount) {
			throw ThrowUtil.newValidationErrorWithNoLog("전환할 랙의 셀 개수와 이전 랙의 셀 개수가 다릅니다.");
		}
	}

	@Override
	public void changeEquipment(JobBatch batch, String toEquipCd) {
		
		Long domainId = batch.getDomainId();
		Query condition = AnyOrmUtil.newConditionForExecution(domainId);
		condition.addSelect("cell_cd");
		condition.addFilter("equipType", batch.getEquipType());
		condition.addFilter("equipCd", batch.getEquipCd());
		condition.addOrder("cellSeq", true);
		
		// 1. From 랙을 셀 순서대로 조회
		List<Cell> fromCellList = this.queryManager.selectList(Cell.class, condition);
		
		// 2. To 랙을 셀 순서대로 조회
		condition.removeFilter("equipCd");
		condition.addFilter("equipCd", toEquipCd);
		List<Cell> toCellList = this.queryManager.selectList(Cell.class, condition);
		
		// 3. 주문 가공 / 주문의 From Cell과 To Cell을 그대로 이동
		String preprocessSql = "UPDATE ORDER_PREPROCESSES SET EQUIP_CD = :toEquipCd, SUB_EQUIP_CD = :toCellCd WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId AND SUB_EQUIP_CD = :fromCellCd";
		String orderSql = "UPDATE ORDERS SET EQUIP_CD = :toEquipCd, SUB_EQUIP_CD = :toCellCd WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId AND SUB_EQUIP_CD = :fromCellCd";
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,toEquipCd", domainId, batch.getId(), toEquipCd);
		
		for(int i = 0 ; i < fromCellList.size() ; i++) {
			Cell fromCell = fromCellList.get(i);
			Cell toCell = toCellList.get(i);
			params.put("fromCellCd", fromCell.getCellCd());
			params.put("toCellCd", toCell.getCellCd());
			
			this.queryManager.executeBySql(preprocessSql, params);
			this.queryManager.executeBySql(orderSql, params);
		}
		
		// 5. 작업 배치 변경
		batch.setEquipCd(toEquipCd);
		this.queryManager.update(batch, "equipCd", "updaterId", "updatedAt");
	}

}
