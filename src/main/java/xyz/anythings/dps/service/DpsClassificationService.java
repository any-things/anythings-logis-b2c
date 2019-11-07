package xyz.anythings.dps.service;

import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.IndConfigSet;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobConfigSet;
import xyz.anythings.base.event.ICategorizeEvent;
import xyz.anythings.base.event.IClassifyErrorEvent;
import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyOutEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.base.model.Category;
import xyz.anythings.base.service.impl.AbstractClassificationService;

/**
 * DPS Picking 트랜잭션 서비스 구현 
 * @author yang
 *
 */
@Component("dpsClassificationService")
public class DpsClassificationService extends AbstractClassificationService {

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
