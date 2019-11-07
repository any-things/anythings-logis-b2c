package xyz.anythings.dps.service;

import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.service.api.IStationBasedBoxPickingService;

/**
 * DPS 작업 스테이션 기반의 박스 처리 포함한 피킹 서비스 트랜잭션 구현 
 * @author yang
 *
 */
@Component("dpsStationBasedBoxPickingService")
public class DpsStationBasedBoxPickingService extends DpsBoxPickingService implements IStationBasedBoxPickingService{
	
	/**
	 * 3-8. 소분류 : 스테이션에 투입된 주문별 피킹 작업 완료 여부 체크
	 * 
	 * @param batch
	 * @param stationCd
	 * @param job
	 * @return
	 */
	@Override
	public boolean checkStationJobsEnd(JobBatch batch, String stationCd, JobInstance job) {
		// TODO Auto-generated method stub
		return false;
	}
}
