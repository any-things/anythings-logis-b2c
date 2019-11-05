package xyz.anythings.b2c.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.event.rest.DeviceProcessRestEvent;
import xyz.anythings.base.model.BaseResponse;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.anythings.base.query.store.BatchQueryStore;
import xyz.anythings.base.rest.DeviceProcessController;
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
	@EventListener(classes=DeviceProcessRestEvent.class, condition = "#event.checkCondition('/batch_summary/','dps')")
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void getBatchSummary(DeviceProcessRestEvent event) {
		// 파라미터 
		String equipType = event.getRequestParams().get("equipType").toString();
		String equipCd = event.getRequestParams().get("equipCd").toString();
		int limit = ValueUtil.toInteger(event.getRequestParams().get("limit"));
		int page = ValueUtil.toInteger(event.getRequestParams().get("page"));
		
		// 1. 작업 진행율 조회  
		BatchProgressRate rate = BeanUtil.get(DeviceProcessController.class).batchProgressRate(equipType, equipCd);
		
		// 2. Input List 조회 
		Page<JobInput> inputItems = BeanUtil.get(DeviceProcessController.class).searchInputList(equipType, equipCd, page, limit, "A");
		
		// 3. 투입 가능 박스 수량 조회 
		String inputableBoxQuery = this.batchQueryStore.getRackDpsBatchInputableBoxQuery();
		Integer inputableBox = this.queryManager.selectBySql(inputableBoxQuery, ValueUtil.newMap("domainId,rackCd",Domain.currentDomainId(),equipCd), Integer.class);
		
		Map<String,Object> ret = ValueUtil.newMap("rate,input_list,inputable_buckets", rate, inputItems, inputableBox);
		BaseResponse response = new BaseResponse(true,"");
		response.setResult(ret);
		
		event.setReturnResult(response);
		event.setExecuted(true);
	}
}
