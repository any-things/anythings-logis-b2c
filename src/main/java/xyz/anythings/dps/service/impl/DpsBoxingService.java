package xyz.anythings.dps.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.WorkCell;
import xyz.anythings.base.query.store.BoxQueryStore;
import xyz.anythings.base.service.impl.AbstractBoxingService;
import xyz.anythings.dps.DpsConstants;
import xyz.anythings.dps.service.api.IDpsBoxingService;
import xyz.anythings.dps.service.util.DpsBatchJobConfigUtil;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.elidom.sys.entity.User;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 박스 처리 서비스
 * 
 * @author yang
 */
@Component("dpsBoxingService")
public class DpsBoxingService extends AbstractBoxingService implements IDpsBoxingService {

	/**
	 * 박스 처리 쿼리 스토어
	 */
	@Autowired
	protected BoxQueryStore boxQueryStore;

	/**
	 * 1-1. 분류 모듈 정보 : 분류 서비스 모듈의 작업 유형 (DAS, RTN, DPS, QPS) 리턴 
	 * 
	 * @return
	 */
	@Override
	public String getJobType() {
		return DpsConstants.JOB_TYPE_DPS;
	}
	
	/**
	 * 박싱 처리
	 * 
	 * @param batch
	 * @param workCell b2b인 경우 필요, b2c인 경우 불필요
	 * @param jobList
	 * @param params
	 * @return
	 */
	@Override
	public BoxPack fullBoxing(JobBatch batch, WorkCell workCell, List<JobInstance> jobList, Object... params) {
		// 1. 박스 유형 (BOX / TRAY)
		String boxType = DpsBatchJobConfigUtil.getInputBoxType(batch);
		JobInstance job = jobList.get(0);
		String orderNo = job.getOrderNo();
		String boxTypeCd = job.getBoxTypeCd();
		String boxPackId = (params != null && params.length > 0) ? ValueUtil.toString(params[0]) : AnyValueUtil.newUuid36();
		
		// 2. boxItems 생성 
		this.createBoxItemsByOrder(batch, orderNo, boxPackId);
		
		// 3. boxItem 정보로 boxPack 생성  
		return this.createBoxPackByBoxItems(batch, orderNo, boxType, boxTypeCd, boxPackId);
	}

	/**
	 * BoxPack 정보 주문 정보의 ID를 기준으로 박스 내품 수량을 업데이트
	 * 
	 * @param domainId
	 * @param boxPackId
	 * @param idsOfOrders
	 * @param toStatus
	 * @param updatePassFlag
	 */
	@Override
	public void updateBoxItemsAfterPick(Long domainId, String boxPackId, List<String> idsOfOrders, String toStatus, boolean updatePassFlag) {
		String qry = this.boxQueryStore.getUpdateBoxItemDataByOrderQuery();
		Map<String, Object> param = ValueUtil.newMap("domainId,boxPackId,orderIds,status", domainId, boxPackId, idsOfOrders, toStatus);
		if(updatePassFlag) {
			param.put("updatePassFlag", true);
		}
		
		this.queryManager.executeBySql(qry, param);
	}
	
	/**
	 * 2-1. 작업 준비 : 셀에 박스를 할당
	 * 
	 * @param batch
	 * @param cellCd
	 * @param boxId
	 * @param params
	 * @return
	 */
	@Override
	public Boolean assignBoxToCell(JobBatch batch, String cellCd, String boxId, Object... params) {
		// DPS에서는 구현 필요 없음
		return null;
	}
	
	/**
	 * 2-2. 작업 준비 : 셀에 할당된 박스 ID 해제
	 * 
	 * @param batch
	 * @param cellCd
	 * @param params
	 * @return
	 */
	@Override
	public Object resetBoxToCell(JobBatch batch, String cellCd, Object... params) {
		// DPS에서는 구현 필요 없음
		return null;
	}
	
	/**
	 * 박싱 취소
	 * 
	 * @param box
	 * @return
	 */
	@Override
	public BoxPack cancelFullboxing(BoxPack box) {
		// DPS에서 박싱 취소 없음
		return null;
	}

	/**
	 * 수량 조절 후 박싱 처리
	 * 
	 * @param batch
	 * @param workCell b2b인 경우 필요, b2c인 경우 불필요
	 * @param jobList
	 * @param fullboxQty
	 * @param params
	 * @return
	 */
	@Override
	public BoxPack partialFullboxing(JobBatch batch, WorkCell workCell, List<JobInstance> jobList, Integer fullboxQty, Object... params) {
		// DPS에서는 구현 필요 없음
		return null;
	}

	/**
	 * 작업 배치에 대해서 박싱 작업이 안 된 모든 박스의 박싱을 완료한다.
	 * 
	 * @param batch
	 * @return
	 */
	@Override
	public List<BoxPack> batchBoxing(JobBatch batch) {
		// DPS에서는 구현 필요 없음
		return null;
	}
	
	/**
	 * 배치, 주문 번호로 박스 내품 데이터를 생성
	 * 
	 * @param batch
	 * @param orderNo
	 * @param boxPackId
	 */
	protected void createBoxItemsByOrder(JobBatch batch, String orderNo, String boxPackId) {
		String qry = this.boxQueryStore.getCreateBoxItemsDataByOrderQuery();
		Map<String, Object> param = 
				ValueUtil.newMap("domainId,batchId,orderNo,userId,boxPackId", batch.getDomainId(), batch.getId(), orderNo, User.currentUser().getId(), boxPackId);
		this.queryManager.executeBySql(qry, param);
	}
	
	/**
	 * 배치, 주문 번호로 박스 데이터를 생성 
	 * 
	 * @param batch
	 * @param orderNo
	 * @param boxType
	 * @param boxTypeCd
	 * @param boxPackId
	 */
	protected BoxPack createBoxPackByBoxItems(JobBatch batch, String orderNo, String boxType, String boxTypeCd, String boxPackId) {
		String qry = this.boxQueryStore.getCreateBoxPackDataByBoxItemsQuery();
		Map<String, Object> param = 
				ValueUtil.newMap("domainId,batchId,orderNo,userId,boxPackId,boxType,boxTypeCd"
				, batch.getDomainId(), batch.getId(), orderNo, User.currentUser().getId(), boxPackId, boxType, boxTypeCd);
		this.queryManager.executeBySql(qry, param);
		return this.queryManager.select(BoxPack.class, boxPackId);
	}

}
