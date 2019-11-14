package xyz.anythings.dps.service;

import java.util.List;

import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobConfigSet;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.WorkCell;
import xyz.anythings.base.service.impl.AbstractBoxingService;
import xyz.anythings.dps.DpsConstants;

/**
 * DPS 박스 처리 서비스   
 * @author yang
 *
 */
@Component("dpsBoxingService")
public class DpsBoxingService extends AbstractBoxingService {

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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
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
	public BoxPack partialFullboxing(JobBatch batch, WorkCell workCell, List<JobInstance> jobList, Integer fullboxQty,
			Object... params) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}
}
