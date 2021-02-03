package operato.logis.dps.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import operato.logis.dps.DpsConstants;
import operato.logis.dps.query.store.DpsBoxQueryStore;
import operato.logis.dps.service.util.DpsBatchJobConfigUtil;
import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.WorkCell;
import xyz.anythings.base.service.api.IBoxingService;
import xyz.anythings.base.service.impl.AbstractBoxingService;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

/**
 * DPS 박스 처리 서비스
 * 
 * @author yang
 */
@Component("dpsBoxingService")
public class DpsBoxingService extends AbstractBoxingService implements IBoxingService {

	/**
	 * 박스 처리 쿼리 스토어
	 */
	@Autowired
	protected DpsBoxQueryStore boxQueryStore;

	/**
	 * 1-1. 분류 모듈 정보 : 분류 서비스 모듈의 작업 유형 (DAS, RTN, DPS, QPS) 리턴
	 * 
	 * @return
	 */
	@Override
	public String getJobType() {
		return DpsConstants.JOB_TYPE_DPS;
	}
	
	@Override
	public boolean isUsedBoxId(JobBatch batch, String boxId, boolean exceptionWhenBoxIdUsed) {
		// 1. 박스 아이디 유니크 범위 설정
		String uniqueScope = DpsBatchJobConfigUtil.getBoxIdUniqueScope(batch, DpsConstants.BOX_ID_UNIQUE_SCOPE_GLOBAL);
		
		// 2. 파라미터 셋팅 
		Map<String, Object> params = ValueUtil.newMap("domainId,boxId,batchId,uniqueScope", batch.getDomainId(), boxId, batch.getId(), uniqueScope);
		
		// 3. 중복 박스 ID가 존재하는지 쿼리
		String qry = this.boxQueryStore.getBoxIdUniqueCheckQuery();
		
		// 4. 존재하지 않으면 사용 가능
		boolean usedBoxId = this.queryManager.selectBySql(qry, params, Integer.class) > 0;
		
		// 5. 사용한 박스이고 exceptionWhenBoxIdUsed가 true이면 예외 발생
		if(usedBoxId && exceptionWhenBoxIdUsed) {
			throw new ElidomRuntimeException("박스 ID [" + boxId + "]는 이미 사용한 박스입니다.");
		}
		
		return usedBoxId;
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
		Map<String, Object> condition = ValueUtil.newMap("domainId,batchId,orderNo", batch.getDomainId(), batch.getId(), jobList.get(0).getOrderNo());
		BoxPack boxPack = this.queryManager.selectByCondition(BoxPack.class, condition);
		return boxPack;
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

}
