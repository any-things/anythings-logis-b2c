package xyz.anythings.dps.service.api;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.service.api.IPickingService;

/**
 * 피킹 서비스 트랜잭션 API 
 * 
 * 	1. 분류 모듈 정보
 * 		1) 
 * 	2. 투입
 * 		1) 
 * 	3. 소분류 처리
 * 		1) 단포 처리 API 등 추가
 */
public interface IDpsPickingService extends IPickingService {

	/**
	 * 피킹 확정
	 * 
	 * @param batch
	 * @param job
	 * @param resQty
	 */
	public void confirmPick(JobBatch batch, JobInstance job, int resQty);
	
	/**
	 * 박스 투입
	 * 
	 * @param batch
	 * @param isBox
	 * @param bucketCd
	 * @param params
	 * @return
	 */
	public Object inputEmptyBucket(JobBatch batch, boolean isBox, String bucketCd, Object... params);
	
	/**
	 * 단포 박스 투입 
	 * 
	 * @param batch
	 * @param isBox
	 * @param skuCd
	 * @param bucketCd
	 * @param params
	 * @return
	 */
	public Object inputSinglePackEmptyBucket(JobBatch batch, boolean isBox, String skuCd, String bucketCd, Object... params);
	
}