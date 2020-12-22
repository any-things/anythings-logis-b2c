package operato.logis.dps.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.BoxType;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.Order;
import xyz.anythings.base.entity.OrderSampler;
import xyz.anythings.base.entity.Rack;
import xyz.anythings.base.entity.SKU;
import xyz.anythings.base.entity.Stage;
import xyz.anythings.base.event.order.SampleOrderEvent;
import xyz.anythings.base.util.LogisBaseUtil;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 샘플 주문 생성 서비스
 * 
 * @author shortstop
 */
@Component
public class DpsSampleOrderService {
	
	/**
	 * 보관 존 리스트
	 */
	private static final String[] STORE_ZONE_ARR = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O" };
	/**
	 * 쿼리 매니저
	 */
	@Autowired
	private IQueryManager queryManger;
	
	/**
	 * 샘플 오더 생성
	 * 
	 * @param event
	 * @return 생성한 배치 ID
	 */
	@EventListener(classes = SampleOrderEvent.class, condition = "#event.jobType == 'DPS'")
	public void createSampleOrder(SampleOrderEvent event) {
		
		OrderSampler orderSampler = event.getOrderSampler();
		this.checkCreatableOrder(orderSampler);
		JobBatch batch = this.newSampleJobBatch(orderSampler);
		this.createSampleBatchOrders(batch, orderSampler);
		this.createJobBatch(batch, orderSampler);
	}
	
	/**
	 * 샘플 배치 생성이 가능한 지 체크
	 * 
	 * @param sampler
	 */
	private void checkCreatableOrder(OrderSampler sampler) {
		if(ValueUtil.isEmpty(sampler.getStageCd())) {
			throw ThrowUtil.newValidationErrorWithNoLog("스테이지 정보가 빈 값입니다.");
		}
		
		if(ValueUtil.isEmpty(sampler.getComCd())) {
			throw ThrowUtil.newValidationErrorWithNoLog("고객사 정보가 빈 값입니다.");
		}
		
		if(ValueUtil.isNotEqual(sampler.getStatus(), LogisConstants.JOB_STATUS_WAIT)) {
			throw ThrowUtil.newValidationErrorWithNoLog("해당 정보는 대기 상태가 아니라서 실행이 불가합니다."); 
		}
		
		String jobDate = sampler.getJobDate();
		Integer batchSeq = sampler.getJobSeq();
		
		Query condition = AnyOrmUtil.newConditionForExecution(sampler.getDomainId());
		condition.addFilter("jobDate", jobDate);
		condition.addFilter("jobSeq", batchSeq);
		
		int count = this.queryManger.selectSize(JobBatch.class, condition);
		if(count > 0) {
			throw ThrowUtil.newValidationErrorWithNoLog("작업일자 [" + jobDate + "], 작업차수 [" + batchSeq + "]에 해당하는 작업배치는 이미 존재합니다.");
		}
	}

	/**
	 * 샘플 작업 배치 생성
	 * 
	 * @param sampler
	 * @return
	 */
	private JobBatch newSampleJobBatch(OrderSampler sampler) {
		
		Long domainId = sampler.getDomainId();
		JobBatch batch = new JobBatch();
		batch.setDomainId(domainId);
		batch.setId(this.newBatchId(sampler));
		batch.setBatchGroupId(batch.getId());
		batch.setWmsBatchNo(batch.getId());
		batch.setWcsBatchNo(ValueUtil.toString(sampler.getJobSeq()));

		batch.setJobDate(sampler.getJobDate());
		batch.setJobSeq(ValueUtil.toString(sampler.getJobSeq()));
		batch.setJobType(sampler.getJobType());
		batch.setComCd(sampler.getComCd());
		batch.setStageCd(sampler.getStageCd());
		batch.setEquipType(sampler.getEquipType());
		
		Stage stage = AnyEntityUtil.findEntityByCode(domainId, true, Stage.class, "stageCd", sampler.getStageCd());
		batch.setAreaCd(stage.getAreaCd());
		
		if(ValueUtil.isEqualIgnoreCase(sampler.getEquipType(), LogisConstants.EQUIP_TYPE_RACK) && ValueUtil.isNotEmpty(sampler.getEquipCd())) {
			Rack rack = Rack.findByRackCd(sampler.getDomainId(), sampler.getEquipCd(), false);
			if(rack != null) {
				batch.setEquipGroupCd(rack.getEquipGroupCd());
				batch.setEquipCd(sampler.getEquipCd());
				batch.setEquipNm(rack.getRackNm());
			}
		}
		
		batch.setParentOrderQty(sampler.getTotalOrderQty());
		batch.setBatchOrderQty(sampler.getTotalOrderQty());
		batch.setResultOrderQty(0);
		batch.setResultBoxQty(0);
		batch.setLastInputSeq(0);
		batch.setEquipRuntime(0.0f);
		batch.setProgressRate(0.0f);
		batch.setUph(0.0f);
		return batch;
	}
	
	/**
	 * 작업 배치 저장
	 * 
	 * @param batch
	 * @param sampler
	 */
	private void createJobBatch(JobBatch batch, OrderSampler sampler) {
		
		// 배치 정보 저장 
		String sql = "select sum(order_qty) as order_qty from orders where domain_id = :domainId and batch_id = :batchId";
		int totalPcs = this.queryManger.selectBySql(sql, ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId()), Integer.class);
		batch.setParentPcs(totalPcs);
		batch.setBatchPcs(totalPcs);
		batch.setStatus(JobBatch.STATUS_READY);
		this.queryManger.insert(batch);
		
		// 샘플 정보 상태 완료 처리
		sampler.setStatus(LogisConstants.JOB_STATUS_FINISH);
		this.queryManger.update(sampler);
	}
	
	/**
	 * 새로운 배치 ID 생성 
	 * 
	 * @param sampler
	 * @return
	 */
	private String newBatchId(OrderSampler sampler) {
		return LogisBaseUtil.newJobBatchId(sampler.getDomainId(), sampler.getStageCd());
	}
	
	/**
	 * 샘플 배치 주문을 생성한다.
	 * 
	 * @param batch
	 * @param sampler
	 * @return
	 */
	private int createSampleBatchOrders(JobBatch batch, OrderSampler sampler) {
		
		Long domainId = batch.getDomainId();
		int totalOrderCount = sampler.getTotalOrderQty();
		List<Order> totalOrders = new ArrayList<Order>(totalOrderCount);
		List<SKU> skuList = this.searchTargetSkuList(domainId, batch.getComCd(), sampler.getTotalSkuQty());
		List<BoxType> boxTypeList = this.queryManger.selectList(BoxType.class, AnyOrmUtil.newConditionForExecution(domainId));
		
		for(int i = 1 ; i <= totalOrderCount ; i++) {
			List<Order> orders = this.createOrder(batch, sampler, i, totalOrders.size(), boxTypeList, skuList);
			totalOrders.addAll(orders);
			
			if(orders.size() == 1000) {
				this.queryManger.insertBatch(orders);
				orders.clear();
			}
		}
		
		if(ValueUtil.isNotEmpty(totalOrders)) {
			this.queryManger.insertBatch(totalOrders);
		}
		
		return totalOrderCount;
	}
	
	/**
	 * 주문 대상 SKU 조회 
	 * 
	 * @param domainId
	 * @param comCd
	 * @param totalSkuCount
	 * @return
	 */
	private List<SKU> searchTargetSkuList(Long domainId, String comCd, int totalSkuCount) {
		if(totalSkuCount > 1000) {
			totalSkuCount = 1000;
		}
		
		String sql = "select * from sku where domain_id = :domainId and com_cd = :comCd and sku_nm is not null and sku_barcd is not null and length(sku_barcd) >= 6 order by sku_cd";
		return this.queryManger.selectListBySql(sql, ValueUtil.newMap("domainId,comCd", domainId, comCd), SKU.class, 1, totalSkuCount);
	}
	
	/**
	 * 주문 생성 
	 * 
	 * @param batch
	 * @param sampler
	 * @param orderIndex
	 * @param orderLineIdx
	 * @param boxTypeList
	 * @param skuList
	 * @return
	 */
	private List<Order> createOrder(JobBatch batch, OrderSampler sampler, int orderIndex, int orderLineSeq, List<BoxType> boxTypeList, List<SKU> skuList) {
		
		// 주문 유형
		boolean isSinglePack = ValueUtil.isEqualIgnoreCase(sampler.getOrderType(), "OT");
		
		// 박스 유형, 송장 번호 
		String boxTypeCd = null;
		String invoiceId = null;
		
		// 박스 유형이 필요하면 박스 유형 선택 
		if(sampler.getNeedBoxTypeFlag()) {
			BoxType boxType = boxTypeList.get(orderIndex % boxTypeList.size());
			boxTypeCd = boxType.getBoxTypeCd();
		}
		
		// 송장이 필요하면 송장 발번 
		if(sampler.getNeedInvoiceFlag()) {
			invoiceId = this.newInvoiceId(batch, orderIndex);
		}
		
		// 생성 주문 리스트 
		List<Order> orderList = new ArrayList<Order>();
		int orderLineIdx = 1;
		int remainOrderQty = sampler.getMaxOrderQty();
		
		// 남은 주문 수량이 있다면 주문 라인 추가 
		while(remainOrderQty > 0) {
			SKU sku = this.pickSkuForOrderLine(skuList, orderLineSeq);
			Order order = this.createOrderLine(batch, sampler, sku, invoiceId, boxTypeCd, orderIndex, orderLineIdx, remainOrderQty);
			orderList.add(order);
			remainOrderQty = remainOrderQty - order.getOrderQty();
			orderLineIdx++;
			orderLineSeq++;
			
			if(isSinglePack) {
				remainOrderQty = 0;
			}
		}
		
		// 생성한 주문 리스트 리턴
		return orderList;
	}
	
	/**
	 * 주문 라인 생성
	 * 
	 * @param batch
	 * @param sampler
	 * @param sku
	 * @param invoiceId
	 * @param boxTypeCd
	 * @param orderIndex
	 * @param orderLineIdx
	 * @param remainOrderQty
	 * @return
	 */
	private Order createOrderLine(JobBatch batch, OrderSampler sampler, SKU sku, String invoiceId, String boxTypeCd, int orderIndex, int orderLineIdx, int remainOrderQty) {
		
		Order order = ValueUtil.populate(batch, new Order());
		order.setId(null);
		order.setBatchId(batch.getId());
		order.setOrderType(sampler.getOrderType());
		order.setOrderDate(sampler.getOrderDate());
		order.setCustOrderNo(this.newCustOrderId(sampler, orderIndex));
		order.setCustOrderLineNo(ValueUtil.toString(orderLineIdx));
		order.setOrderNo(this.newOrderId(sampler, orderIndex));
		order.setOrderLineNo(ValueUtil.toString(orderLineIdx));
		order.setOrderDetailId(order.getOrderNo() + order.getOrderLineNo());
		order.setAreaCd(batch.getAreaCd());
		order.setInvoiceId(invoiceId);
		order.setBoxTypeCd(boxTypeCd);
		order.setComCd(sku.getComCd());
		order.setSkuCd(sku.getSkuCd());
		order.setSkuBarcd(sku.getSkuBarcd());
		order.setSkuNm(sku.getSkuNm());
		order.setBoxInQty(sku.getBoxInQty());
		int orderQty = (int)(Math.random() * remainOrderQty) + 1;
		order.setOrderQty(orderQty);
		order.setPickedQty(0);
		order.setBoxedQty(0);
		int packType = (int)(Math.random() * 2) + 1;
		order.setPackType(ValueUtil.toString(packType));
		order.setEquipCd(batch.getEquipCd());
		order.setFromZoneCd(sku.getSkuDesc());
		order.setFromCellCd(sku.getCellCd());
		return order;
	}
	
	/**
	 * 주문 번호 생성
	 * 
	 * @param sampler
	 * @param orderIndex
	 * @return
	 */
	private String newOrderId(OrderSampler sampler, int orderIndex) {
		String orderPrefix = ValueUtil.isNotEmpty(sampler.getOrderIdPrefix()) ? sampler.getOrderIdPrefix() : "O";
		String jobDate = sampler.getJobDate().replaceAll(LogisConstants.DASH, LogisConstants.EMPTY_STRING);
		return orderPrefix + jobDate + sampler.getJobSeq() + StringUtils.leftPad(ValueUtil.toString(orderIndex), 5, "0");
	}
	
	/**
	 * 원 주문 번호 생성
	 * 
	 * @param sampler
	 * @param orderIndex
	 * @return
	 */
	private String newCustOrderId(OrderSampler sampler, int orderIndex) {
		String orderPrefix = ValueUtil.isNotEmpty(sampler.getCustOrderPrefix()) ? sampler.getCustOrderPrefix() : "CO";
		String jobDate = sampler.getJobDate().replaceAll(LogisConstants.DASH, LogisConstants.EMPTY_STRING);
		return orderPrefix + jobDate + sampler.getJobSeq() + StringUtils.leftPad(ValueUtil.toString(orderIndex), 5, "0");
	}

	/**
	 * 새로운 송장 번호 발행
	 * 
	 * @param batch
	 * @param orderIndex
	 * @return
	 */
	private String newInvoiceId(JobBatch batch, int orderIndex) {
		return batch.getJobDate().replaceAll(LogisConstants.DASH, LogisConstants.EMPTY_STRING).substring(4) + StringUtils.leftPad("" + batch.getJobSeq(), 2, "0") + StringUtils.leftPad("" + orderIndex, 6, "0");
	}
	
	/**
	 * 주문 라인 생성을 위한 SKU 선택
	 * 
	 * @param skuList
	 * @param orderLineSeq
	 * @return
	 */
	private SKU pickSkuForOrderLine(List<SKU> skuList, int orderLineSeq) {
		
		int zoneIdx = orderLineSeq % STORE_ZONE_ARR.length;
		String storeZone = STORE_ZONE_ARR[zoneIdx];
		
		SKU sku = skuList.get(orderLineSeq % skuList.size());
		sku.setSkuDesc(storeZone + "1");
		sku.setCellCd("01-" + StringUtils.leftPad("" + orderLineSeq, 5, "0"));
		return sku;
	}

}
