package xyz.anythings.dps.service;

import java.util.List;

import org.springframework.stereotype.Component;

import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.IndConfigSet;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobConfigSet;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.WorkCell;
import xyz.anythings.base.service.impl.AbstractBoxingService;

/**
 * DPS 박스 처리 서비스   
 * @author yang
 *
 */
@Component("dpsBoxingService")
public class DpsBoxingService extends AbstractBoxingService {

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
	public Boolean assignBoxToCell(JobBatch batch, String cellCd, String boxId, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object resetBoxToCell(JobBatch batch, String cellCd, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BoxPack fullBoxing(JobBatch batch, WorkCell workCell, List<JobInstance> jobList, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BoxPack partialFullboxing(JobBatch batch, WorkCell workCell, List<JobInstance> jobList, Integer fullboxQty,
			Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BoxPack> batchBoxing(JobBatch batch) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BoxPack cancelFullboxing(BoxPack box) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
