package xyz.anythings.dps.model;

import java.util.List;

import xyz.anythings.base.entity.JobInstance;

/**
 * 단포 작업 정보 
 * 현자 작업 정보 및 전체 summary 정보 
 * @author yang
 *
 */
public class DpsSinglePackJobInform {
	/**
	 * 작업 현황 정보 
	 */
	private List<DpsSinglePackInform> summary;
	/**
	 * 현재 작업 정보 
	 */
	private JobInstance job;
	
	public DpsSinglePackJobInform(List<DpsSinglePackInform> summary, JobInstance job) {
		this.summary = summary;
		this.job = job;
	}
	
	public List<DpsSinglePackInform> getSummary() {
		return summary;
	}
	public void setSummary(List<DpsSinglePackInform> summary) {
		this.summary = summary;
	}
	public JobInstance getJob() {
		return job;
	}
	public void setJob(JobInstance job) {
		this.job = job;
	}
}
