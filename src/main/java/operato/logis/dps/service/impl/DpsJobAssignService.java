package operato.logis.dps.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.dps.model.DpsJobAssign;
import operato.logis.dps.query.store.DpsAssignQueryStore;
import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.Order;
import xyz.anythings.base.entity.Stock;
import xyz.anythings.base.service.impl.AbstractLogisService;
import xyz.anythings.sys.event.model.ErrorEvent;
import xyz.anythings.sys.service.ICustomService;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 작업 할당 서비스
 * 
 * @author shortstop
 */
@Component("dpsJobAssignService")
public class DpsJobAssignService extends AbstractLogisService {
	/**
	 * 작업 할당에서 제외할 주문 처리 커스텀 서비스
	 */
	private static final String CUSTOM_SERVICE_SKIP_ORDERS = "diy-dps-assign-skip-orders";
	/**
	 * 현재 할당 작업이 진행 중인지 여부
	 */
	//private boolean assignJobRunning = false;
	/**
	 * DPS Query Store
	 */
	@Autowired
	private DpsAssignQueryStore dpsAssignQueryStore;
	/**
	 * 커스텀 서비스
	 */
	@Autowired
	private ICustomService customService;
	
	/**
	 * 작업 할당 서비스
	 * 
	 * @param domainId
	 */
	@Transactional
	public void assignDpsJobs(Long domainId) {
		
		// 스케줄링 활성화 여부 && 이전 작업이 진행 중인 여부 체크
		if(!this.isJobEnabeld()) {
			return;
		}
		
		// 작업 중 플래그 Up
		//this.assignJobRunning = true;
		
		// 1. 진행 중인 배치 리스트 조회
		List<JobBatch> batchList = this.searchRunningBatchList(domainId);
		
		if(ValueUtil.isEmpty(batchList)) {
			return;
		}
		
		// 2. 현재 진행 중인 작업 배치 별 작업 할당 처리
		try {
			for(JobBatch batch : batchList) {
				// TODO 별도 할당 로직을 돌리는 경우 처리 (커스텀 서비스)...
				this.standardAssignJobs(batch);
			}
		} catch(Exception e) {
			// 3. 예외 처리
			ErrorEvent errorEvent = new ErrorEvent(domainId, "JOB_DPS_ASSIGN_ERROR", e, null, true, true);
			this.eventPublisher.publishEvent(errorEvent);
		}
		
		// 4. 작업 중 플래그 리셋
		//this.assignJobRunning = false;
	}
	
	/**
	 * Scheduler 활성화 여부
	 * 
	 * @return
	 */
	protected boolean isJobEnabeld() {
		return true;
	}
	
	/**
	 * 현재 진행 중인 DPS 배치 리스트 조회
	 * 
	 * @param domainId
	 * @return
	 */
	private List<JobBatch> searchRunningBatchList(Long domainId) {
		Query condition = AnyOrmUtil.newConditionForExecution(domainId);
		condition.addFilter("status", JobBatch.STATUS_RUNNING);
		condition.addFilter("jobType", LogisConstants.JOB_TYPE_DPS);
		condition.addOrder("jobDate", false);
		return this.queryManager.selectList(JobBatch.class, condition);
	}
	
	/**
	 * 표준 작업 할당 로직 
	 * 
	 * @param batch
	 */
	private void standardAssignJobs(JobBatch batch) {
		// 1. 작업 배치 내 모든 재고 중에 가장 많은 재고 순으로 상품별 재고 수량 조회
		List<Stock> stockList = this.searchStocksForAssign(batch);

		// 2. 재고가 없다면 스킵
		if(ValueUtil.isEmpty(stockList)) {
			return;
		}
		
		// 별도 트랜잭션 처리를 위해 컴포넌트로 호출하기 위함
		DpsJobAssignService dpsJobAssignSvc = BeanUtil.get(DpsJobAssignService.class);

		// 3. 트랜잭션 분리를 위해 자신을 레퍼런스, 트랜잭션 분리는 각 주문 단위 ...
		List<String> skipOrderList = this.searchSkipOrders(batch);

		// 4. 배치 내 SKU가 적치된 재고 수량을 기준으로 많은 재고 조회
		for(Stock stock : stockList) {
			// 4.1 현재 시점에 특정 상품의 할당 가능한 재고 총 수량 계산
			Integer stockQty = this.calcTotalStockQty(batch, stock);

			// 4.2 재고 총 수량 체크 
			if(stockQty == null || stockQty < 1) {
				continue;
			}

			// 4.3 재고의 상품이 필요한 주문번호 검색
			List<Order> orders = this.searchOrdersForStock(batch, stock, stockQty, skipOrderList);

			// 4.4 할당이 필요한 주문번호가 없다면 스킵
			if(ValueUtil.isEmpty(orders)) {
				continue;
			}

			// 4.5 할당이 필요한 주문 번호별로 ...
			for(Order order : orders) {
				// 4.5.1 남은 재고 수량이 주문 수량보다 적으면 해당 상품에 대한 주문 할당 처리는 종료하면서 처리 못하는 주문 번호 리스트에 추가
				if(stockQty < order.getOrderQty()) {
					skipOrderList.add(order.getOrderNo());
					break;
				}

				// 4.5.2 해당 주문 별로 주문별 상품별 가용 재고 조회
				List<DpsJobAssign> candidates = this.searchAssignableCandidates(batch, order.getOrderNo());
				
				// 4.5.3 가용 재고가 없으면 스킵 
				if(ValueUtil.isEmpty(candidates)) {
					continue;
				}

				// 4.5.4 주문별 상품별 가용 재고 조회 할당 여부 판별 후 할당
				try {
					stockQty = dpsJobAssignSvc.assignJobsByStock(stock, order, stockQty, candidates, skipOrderList);
				} catch(Exception e) {
					ErrorEvent errorEvent = new ErrorEvent(batch.getDomainId(), "JOB_ASSIGN_ERROR", e, null, true, true);
					this.eventPublisher.publishEvent(errorEvent);
				}
			}
		}
	}

	/**
	 * 작업 배치 내 모든 재고 중에 가장 많은 재고 순으로 조회
	 * 
	 * @param batch
	 * @return
	 */
	private List<Stock> searchStocksForAssign(JobBatch batch) {
		String sql = this.dpsAssignQueryStore.getStockForJobAssignQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipType,equipCd,activeFlag", batch.getDomainId(), batch.getId(), batch.getEquipType(), batch.getEquipCd(), true);
		return this.queryManager.selectListBySql(sql, params, Stock.class, 0, 0);
	}
	
	/**
	 * 스킵 처리할 주문 리스트 조회
	 * 
	 * @param batch
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String> searchSkipOrders(JobBatch batch) {
		Object retList = this.customService.doCustomService(batch.getDomainId(), CUSTOM_SERVICE_SKIP_ORDERS, ValueUtil.newMap("batch", batch));
		return (retList == null) ? new ArrayList<String>(1) : (List)retList;
	}

	/**
	 * 현재 시점에 특정 상품의 할당 가능한 재고 총 수량 계산
	 * 
	 * @param batch
	 * @param stock
	 * @return
	 */
	private Integer calcTotalStockQty(JobBatch batch, Stock stock) {
		String sql = "SELECT SUM(S.LOAD_QTY) FROM STOCKS S WHERE S.DOMAIN_ID = :domainId AND S.EQUIP_CD = :equipCd AND S.EQUIP_TYPE = :equipType AND S.SKU_CD = :skuCd AND S.ACTIVE_FLAG = :activeFlag AND (S.LOAD_QTY IS NOT NULL AND S.LOAD_QTY > 0)";
		Map<String, Object> params = ValueUtil.newMap("domainId,equipType,equipCd,skuCd,activeFlag", batch.getDomainId(), batch.getEquipType(), batch.getEquipCd(), stock.getSkuCd(), true);
		return this.queryManager.selectBySql(sql, params, Integer.class);
	}
	
	/**
	 * 해당 재고가 필요한 주문 조회
	 * 
	 * @param batch
	 * @param stock
	 * @param stockQty
	 * @param skipOrderList
	 * @return
	 */
	private List<Order> searchOrdersForStock(JobBatch batch, Stock stock, int stockQty, List<String> skipOrderList) {
		String sql = this.dpsAssignQueryStore.getSearchOrderForStockQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,skuCd,stockQty,skipOrderIdList", batch.getDomainId(), batch.getId(), stock.getSkuCd(), stockQty, ValueUtil.isEmpty(skipOrderList) ? null : skipOrderList);
		return this.queryManager.selectListBySql(sql, params, Order.class, 0, 0);
	}

	/**
	 * 작업 할당에 필요한 주문 및 재고 조합 정보 조회
	 *  
	 * @param batch
	 * @param orderNo
	 * @return
	 */
	private List<DpsJobAssign> searchAssignableCandidates(JobBatch batch, String orderNo) {
		String sql = this.dpsAssignQueryStore.getSearchAssignCandidatesQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,equipGroupCd,equipType,equipCd,orderNo", batch.getDomainId(), batch.getId(), batch.getEquipGroupCd(), batch.getEquipType(), batch.getEquipCd(), orderNo);
		return this.queryManager.selectListBySql(sql, params, DpsJobAssign.class, 0, 0);
	}

	/**
	 * 주문별 작업 할당 처리
	 * 
	 * @param stock
	 * @param order
	 * @param stockQty
	 * @param skipOrderList
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int assignJobsByStock(Stock stock, Order order, int stockQty, List<DpsJobAssign> candidates, List<String> skipOrderList) {
		// 1. 주문별 주문 수량 초기화
		int orderQty = 0;
		
		// 2. 주문 할당 대상별로 ...
		for(DpsJobAssign candidate : candidates) {
			// 2.1 할당할 수 있는 수량이 아니면 스킵
			if(candidate.getCheckAssignable() != 0) {
				skipOrderList.add(candidate.getOrderNo());
				break;
			}
			
			// 2.2 주문 라인 내 첫 번째 순위인 경우에 - 주문 상품별 첫 번째 순위인 경우 (주문 수량)
			if(candidate.getRanking() == 1) {
				orderQty = candidate.getOrderQty();
				
				// 주문 내 상품 정보가 stock의 상품 정보와 같은 경우는 stockQty를 업데이트
				if(ValueUtil.isEqual(candidate.getSkuCd(), stock.getSkuCd())) {
					stockQty = stockQty - orderQty;
				}
			}

			// 2.3 최종 작업 할당 - 할당 로케이션이 여러 개의 경우에는 할당 후 남은 주문 수량 리턴
			if(orderQty > 0) {
				// 2.3.1 주문 수량이 0 보다 큰 경우에만 할당 데이터 (DpsJobInstance) 생성
				orderQty = this.assignJob(order.getDomainId(), candidate, stockQty, orderQty, skipOrderList);
			}
		}
		
		// 3. 작업 할당 후 남은 재고 수량
		return stockQty;
	}
	
	/**
	 * 주문별 주문 라인별 작업 할당 처리 
	 * 
	 * @param candidate
	 * @param stockQty
	 * @param orderQty
	 * @param skipOrderList
	 * @return
	 */
	public int assignJob(Long domainId, DpsJobAssign candidate, int stockQty, int orderQty, List<String> skipOrderList) {
		// 1. 할당 수량 초기화 
		int assignQty = (orderQty > candidate.getLoadQty()) ? candidate.getLoadQty() : orderQty;
		
		// 2. JobInstance 데이터 생성 
		String dpsJobQry = this.dpsAssignQueryStore.getAssignJobInstanceQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,orderNo,skuCd,cellCd,assignQty,equipType,equipCd,indCd,colorCd", domainId, candidate.getBatchId(), candidate.getOrderNo(), candidate.getSkuCd(), candidate.getCellCd(), assignQty, candidate.getEquipType(), candidate.getEquipCd(), candidate.getIndCd(), null);
		this.queryManager.executeBySql(dpsJobQry.toString(), params);
		
		// 3. 주문 정보에 작업 할당 상태 업데이트
		String sql = "UPDATE ORDERS SET STATUS = 'A', UPDATED_AT = now() WHERE DOMAIN_ID = :domainId AND BATCH_ID = :batchId AND CLASS_CD = :orderNo AND SKU_CD = :skuCd AND STATUS = 'W'";
		this.queryManager.executeBySql(sql, params);
		
		// 4. 재고 업데이트
		Stock s = AnyEntityUtil.findEntityByIdWithLock(false, Stock.class, candidate.getStockId(), "id", "equip_type", "equip_cd", "cell_cd", "com_cd", "sku_cd", "alloc_qty", "load_qty");
		if(s != null) {
			s.assignJob(assignQty);
		}
		
		// 5. 할당 가능 수량을 제외한 주문 수량 리턴 
		return orderQty - assignQty;
	}

}
