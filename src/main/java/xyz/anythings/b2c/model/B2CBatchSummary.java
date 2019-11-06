package xyz.anythings.b2c.model;

import xyz.anythings.base.entity.JobInput;
import xyz.anythings.base.model.BatchProgressRate;
import xyz.elidom.dbist.dml.Page;

/**
 * B2C 배치 서머리 정보 리턴 모델 
 * @author yang
 *
 */
public class B2CBatchSummary{
	
	/**
	 * 배치 진행율 
	 */
	private BatchProgressRate rate;
	
	/**
	 * 배치 작업 투입 리스트 
	 */
	private Page<JobInput> inputList;
	
	/**
	 * 투입 가능 박스수량 
	 */
	private Integer inputableBuckets;
	
	public B2CBatchSummary(BatchProgressRate rate, Page<JobInput> inputList, Integer inputableBuckets) {
		this.rate = rate;
		this.inputList = inputList;
		this.inputableBuckets = inputableBuckets;
	}

	public BatchProgressRate getRate() {
		return rate;
	}

	public void setRate(BatchProgressRate rate) {
		this.rate = rate;
	}

	public Page<JobInput> getInputList() {
		return inputList;
	}

	public void setInputList(Page<JobInput> inputList) {
		this.inputList = inputList;
	}

	public Integer getInputableBuckets() {
		return inputableBuckets;
	}

	public void setInputableBuckets(Integer inputableBuckets) {
		this.inputableBuckets = inputableBuckets;
	}
}