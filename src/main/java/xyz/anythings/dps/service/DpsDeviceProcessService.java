package xyz.anythings.dps.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.entity.Rack;
import xyz.anythings.base.event.rest.DeviceProcessRestEvent;
import xyz.anythings.base.model.BaseResponse;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.model.EquipBatchSet;
import xyz.anythings.base.query.store.BatchQueryStore;
import xyz.anythings.base.rest.DeviceProcessController;
import xyz.anythings.base.service.impl.ConfigSetService;
import xyz.anythings.dps.model.DpsBatchSummary;
import xyz.anythings.dps.service.util.DpsServiceUtil;
import xyz.anythings.sys.service.AbstractExecutionService;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * dps 디바이스 이벤트 처리 서비스 
 * @author yang
 *
 */
@Component
public class DpsDeviceProcessService extends AbstractExecutionService{
	
	/**
	 * 설정 셋 서비스
	 */
	@Autowired
	private ConfigSetService configSetService;

	
	@Autowired
	BatchQueryStore batchQueryStore;
	
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
		DpsBatchSummary summary = this.getBatchSummary(equipType,equipCd,limit,page);

		// 3. 이벤트 처리 결과 셋팅 
		event.setReturnResult(new BaseResponse(true,"", summary));
		event.setExecuted(true);
	}
	
	
	/**
	 * DPS 버킷 투입
	 * BOX or Tray
	 * @param event
	 */
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

		Long domainId = Domain.currentDomainId();
		
		// 1.equipType / Cd 로 설비 및 배치 조회 
		EquipBatchSet equipBatchSet = DpsServiceUtil.findBatchByEquip(domainId, equipType, equipCd);
		Rack rack = (Rack)equipBatchSet.getEquipEntity();
		JobBatch batch = equipBatchSet.getBatch();
		
		// 2. 버킷 타입에 따라 버킷 조회 및 버킷 락 
		DpsServiceUtil.vaildInputBucketByBucketCd(domainId, batch, bucketCd, inputType);
		
		
		
		
		
		// 12. 배치 서머리 조회 
		DpsBatchSummary summary = this.getBatchSummary(equipType,equipCd,limit,page);

		// 13. 이벤트 처리 결과 셋팅 
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
	private DpsBatchSummary getBatchSummary(String equipType, String equipCd, int limit, int page) {
		// 1. 작업 진행율 조회  
		BatchProgressRate rate = BeanUtil.get(DeviceProcessController.class).batchProgressRate(equipType, equipCd);
		
		// 2. Input List 조회 
		Page<JobInput> inputItems = BeanUtil.get(DeviceProcessController.class).searchInputList(equipType, equipCd, page, limit, null);
		
		// 3. 투입 가능 박스 수량 조회 
		String inputableBoxQuery = this.batchQueryStore.getRackDpsBatchInputableBoxQuery();
		Integer inputableBox = this.queryManager.selectBySql(inputableBoxQuery, ValueUtil.newMap("domainId,rackCd",Domain.currentDomainId(),equipCd), Integer.class);
		
		return new DpsBatchSummary(rate,inputItems,inputableBox);
	}
}
