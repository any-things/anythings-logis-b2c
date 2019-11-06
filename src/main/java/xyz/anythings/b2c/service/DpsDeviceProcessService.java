package xyz.anythings.b2c.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.b2c.B2CConstants;
import xyz.anythings.b2c.model.B2CBatchSummary;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.Rack;
import xyz.anythings.base.event.rest.DeviceProcessRestEvent;
import xyz.anythings.base.model.BaseResponse;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.model.EquipBatchSet;
import xyz.anythings.base.query.store.BatchQueryStore;
import xyz.anythings.base.rest.DeviceProcessController;
import xyz.anythings.base.service.util.LogisServiceUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * dps 디바이스 이벤트 처리 서비스 
 * @author yang
 *
 */
@Component
public class DpsDeviceProcessService {
	
	@Autowired
	BatchQueryStore batchQueryStore;
	
	@Autowired
	IQueryManager queryManager;

	/**
	 * DPS 배치 작업 진행 율 조회 
	 * 진행율 + 투입 순서리스트 
	 * @param event
	 */
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/batch_summary','dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void batchSummaryEventProcess(DeviceProcessRestEvent event) {
		// 1. 파라미터 
		String equipType = event.getRequestParams().get("equipType").toString();
		String equipCd = event.getRequestParams().get("equipCd").toString();
		int limit = ValueUtil.toInteger(event.getRequestParams().get("limit"));
		int page = ValueUtil.toInteger(event.getRequestParams().get("page"));
		
		// 2. 배치 서머리 조회 
		B2CBatchSummary summary = this.getBatchSummary(equipType,equipCd,limit,page);

		// 3. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true,"", summary));
		event.setExecuted(true);
	}
	
	
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/input_bucket','dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void inputBucket(DeviceProcessRestEvent event) {
		// 1. 파라미터 
		String equipType = event.getRequestParams().get("equipType").toString();
		String equipCd = event.getRequestParams().get("equipCd").toString();
		String bucketCd = event.getRequestParams().get("bucketCd").toString();
		String inputType = event.getRequestParams().get("inputType").toString();
		
		int limit = ValueUtil.toInteger(event.getRequestParams().get("limit"));
		int page = ValueUtil.toInteger(event.getRequestParams().get("page"));

		long domainId = Domain.currentDomainId();
		
		EquipBatchSet equipBatchSet = LogisServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		
		// 2. 투입 타입에 따라 분기 
		if(ValueUtil.isEqualIgnoreCase(B2CConstants.BUCKET_INPUT_TYPE_BOX, inputType)){
			// 2.1 투입 타입이 박스 
			// job.cmm.box.box_id.unique.scope
			Rack rack = (Rack)equipBatchSet.getEquipEntity();
			
			
		} else {
			// 2.2 투입 타입이 트레이 
			// TrayBox.class
			
			
			// LogisServiceUtil
			
		}
		
		// 2. 배치 서머리 조회 
		B2CBatchSummary summary = this.getBatchSummary(equipType,equipCd,limit,page);

		// 3. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true,"", summary));
		event.setExecuted(true);
	}
	
	
	/**
	 * B2C 배치에 대한 진행율 을 조회 한다. 
	 * @param equipType
	 * @param equipCd
	 * @param limit
	 * @param page
	 * @return
	 */
	private B2CBatchSummary getBatchSummary(String equipType, String equipCd, int limit, int page) {
		// 1. 작업 진행율 조회  
		BatchProgressRate rate = BeanUtil.get(DeviceProcessController.class).batchProgressRate(equipType, equipCd);
		
		// 2. Input List 조회 
		Page<JobInput> inputItems = BeanUtil.get(DeviceProcessController.class).searchInputList(equipType, equipCd, page, limit, null);
		
		// 3. 투입 가능 박스 수량 조회 
		String inputableBoxQuery = this.batchQueryStore.getRackDpsBatchInputableBoxQuery();
		Integer inputableBox = this.queryManager.selectBySql(inputableBoxQuery, ValueUtil.newMap("domainId,rackCd",Domain.currentDomainId(),equipCd), Integer.class);
		
		return new B2CBatchSummary(rate,inputItems,inputableBox);
	}
}
