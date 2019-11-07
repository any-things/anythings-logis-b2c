package xyz.anythings.dps.service.util;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.service.util.LogisServiceUtil;
import xyz.anythings.dps.DpsConstants;
import xyz.elidom.util.ValueUtil;

public class DpsServiceUtil extends LogisServiceUtil{
	
	/**
	 * 버킷이 투입 가능한 버킷인 지 확인 및 해당 버킷에 Locking
	 * @param domainId
	 * @param batch
	 * @param bucketCd
	 * @param inputType
	 */
	public static void vaildInputBucketByBucketCd(Long domainId, JobBatch batch, String bucketCd, String inputType) {
		
		// 1. 트레이 타입이면 버킷에 락킹 - 하나의 버킷은 당연히 한 번에 하나만 투입가능하다.
		if(ValueUtil.isEqualIgnoreCase(DpsConstants.BUCKET_INPUT_TYPE_TRAY, inputType)) {
			checkVaildTray(domainId, bucketCd, true);
		// 2. 박스 타입이면 박스 타입에 락킹 - 즉 동일 박스 타입의 박스는 동시에 하나씩만 투입 가능하다.
		} else {
			String boxTypeCd = getBoxTypeByBoxId(batch, bucketCd);
			findBoxType(domainId, boxTypeCd, true, true);
		}
	}
}