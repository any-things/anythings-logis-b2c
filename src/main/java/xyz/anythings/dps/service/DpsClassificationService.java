package xyz.anythings.dps.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobConfigSet;
import xyz.anythings.base.event.ICategorizeEvent;
import xyz.anythings.base.event.IClassifyErrorEvent;
import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyOutEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.base.model.Category;
import xyz.anythings.base.query.store.BatchQueryStore;
import xyz.anythings.base.service.impl.AbstractClassificationService;
import xyz.anythings.dps.DpsConstants;
import xyz.elidom.util.ValueUtil;

/**
 * DPS Picking 트랜잭션 서비스 구현 
 * @author yang
 *
 */
@Component("dpsClassificationService")
public class DpsClassificationService extends AbstractClassificationService {
	
	@Autowired
	DpsJobStatusService dpsJobStatusService;
	
	@Autowired
	DpsBoxingService dpsBoxingService;
	
	@Autowired
	BatchQueryStore batchQueryStore;
	
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
	 * 1-2. 분류 모듈 정보 : 작업 배치별 작업 설정 정보
	 * 
	 * @param batchId
	 * @return
	 */
	@Override
	public JobConfigSet getJobConfigSet(String batchId) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 1-3. 분류 모듈 정보 : 작업 배치별 표시기 설정 정보
	 * 
	 * @param batchId
	 * @return
	 */
	/*@Override
	public IndConfigSet getIndConfigSet(String batchId) {
		// TODO Auto-generated method stub
		return null;
	}*/

	/**
	 * 2-1. 중분류 이벤트
	 *  
	 * @param event
	 * @return
	 */
	@Override
	public Category categorize(ICategorizeEvent event) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 3-1. 투입 ID로 유효성 체크 및 투입 유형을 찾아서 리턴 
	 * 
	 * @param domainId
	 * @param inputId
	 * @param params
	 * @return LogisCodeConstants.CLASSIFICATION_INPUT_TYPE_...
	 */
	@Override
	public String checkInput(Long domainId, String inputId, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 3-2. 분류 설비에 투입 처리
	 * 
	 * @param inputEvent
	 */
	@Override
	public void input(IClassifyInEvent inputEvent) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 4-1. 소분류 : 분류 처리 작업
	 * 
	 * @param exeEvent 분류 처리 이벤트
	 * @return
	 */
	@Override
	public Object classify(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 4-2. 소분류 : 분류 처리 결과 처리 (DAS, DPS, 반품 - 풀 박스 처리 후 호출, 소터 - 단위 상품별 분류 처리 시 I/F로 넘어온 후 호출)
	 * 
	 * @param outputEvent
	 * @return
	 */
	@Override
	public Object output(IClassifyOutEvent outputEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 5-1. 기타 : 배치 내 모든 분류 작업이 완료되었는지 여부 
	 * 
	 * @param batchId
	 * @return
	 */
	@Override
	public boolean checkEndClassifyAll(String batchId) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 5-2. 기타 : 분류 서비스 모듈별 작업 시작 중 추가 처리
	 * 
	 * @param batch
	 */
	@Override
	public void batchStartAction(JobBatch batch) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 5-3. 기타 : 분류 서비스 모듈별 작업 마감 중 추가 처리
	 * 
	 * @param batch
	 */
	@Override
	public void batchCloseAction(JobBatch batch) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 5-4. 기타 : 분류 작업 처리시 에러 핸들링
	 * 
	 * @param errorEvent
	 */
	@Override
	public void handleClassifyException(IClassifyErrorEvent errorEvent) {
		// TODO Auto-generated method stub
		
	}

	
	/**
	 * 다음 맵핑 할 작업을 찾는다.
	 * @param domainId
	 * @param batch
	 * @param bucketType
	 * @return
	 */
	protected String findNextMappingJob(Long domainId, JobBatch batch, String boxTypeCd, String mapColumn, String orderType){
		// TODO : order / shop .... 여러타입에 대한 구현 필요 
		// 실제 DPS 기준 주문 가공 프로세스임.
		// 주문 처리 순서를 정할수 있는 부분으로 쿼리에 대한 수정으로 해결 가능 ? 
		
		// 1. 쿼리 
		String qry =  this.batchQueryStore.getFindNextMappingJobQuery();
		
		// 2. 파라민터 
		Map<String,Object> params = ValueUtil.newMap("domainId,mapColumn,batchId,orderType,boxTypeCd"
				, domainId, mapColumn, batch.getId(), orderType, boxTypeCd);
		
		// 3. 조회 ( 맵핑 기준에 따라 결과가 달라짐 )
		return this.queryManager.selectBySql(qry, params, String.class);
	}
}
