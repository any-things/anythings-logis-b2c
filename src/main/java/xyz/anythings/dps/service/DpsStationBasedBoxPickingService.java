package xyz.anythings.dps.service;

import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.IndConfigSet;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobConfigSet;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.event.ICategorizeEvent;
import xyz.anythings.base.event.IClassifyErrorEvent;
import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyOutEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.base.model.Category;
import xyz.anythings.base.service.api.IBoxingService;
import xyz.anythings.base.service.impl.AbstractStationBasedBoxPickingService;

/**
 * DPS 작업 스테이션 기반의 박스 처리 포함한 피킹 서비스 트랜잭션 구현 
 * @author yang
 *
 */
@Component("dpsStationBasedBoxPickingService")
public class DpsStationBasedBoxPickingService extends AbstractStationBasedBoxPickingService {

	@Override
	public boolean checkStationJobsEnd(JobBatch batch, String stationCd, JobInstance job) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IBoxingService getBoxingService(Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BoxPack fullBoxing(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BoxPack cancelBoxing(Long domainId, String boxPackId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkBoxingEnd(JobBatch batch, String orderId, String boxId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object inputEmptyBox(IClassifyInEvent inputEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object inputEmptyTray(IClassifyInEvent inputEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void confirmPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancelPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int splitPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int undoPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getJobType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JobConfigSet getJobConfigSet(String batchId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndConfigSet getIndConfigSet(String batchId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Category categorize(ICategorizeEvent event) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String checkInput(Long domainId, String inputId, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void input(IClassifyInEvent inputEvent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object classify(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object output(IClassifyOutEvent outputEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkEndClassifyAll(String batchId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void batchStartAction(JobBatch batch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void batchCloseAction(JobBatch batch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleClassifyException(IClassifyErrorEvent errorEvent) {
		// TODO Auto-generated method stub
		
	}

}
