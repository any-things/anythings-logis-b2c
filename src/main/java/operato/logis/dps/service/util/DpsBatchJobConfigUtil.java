package operato.logis.dps.service.util;

import operato.logis.dps.DpsConfigConstants;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.service.util.BatchJobConfigUtil;
import xyz.elidom.sys.util.ValueUtil;

/**
 * DPS 관련 작업 배치 관련 설정 프로파일
 * 
 * 작업 설정 프로파일 컨셉
 * 1. 스테이지마다 기본 설정 프로파일이 존재하고 기본 설정 프로파일은 default_flag = true인 것이다.
 * 2. job.cmm으로 시작하는 항목은 모두 기본 설정 프로파일에 추가가 이미 되어 있다. -> 없으면 해당 설정 항목 조회시 에러 발생해야 함
 * 3. default_flag가 false인 설정 프로파일은 기본 설정 프로파일의 모든 값을 복사하여 가지고 있어서 조회시 자기가 가진 정보로 조회한다. 없으면 기본 설정을 찾는다.
 * 4. 작업 배치에서 조회할 내용이 아닌 설정은 (성격상 작업 배치가 결정되지 않은 시점에 필요한 설정) Setting 정보에 존재한다.
 * 
 * DPS 작업 설정 항목
 * 	job.dps.input.box.type						투입 박스 유형
 * 	job.dps.preproces.cell.mapping.field		셀에 할당할 대상 필드 (매장, 상품, 주문번호…)
 *  job.dps.box.out.class.field					방면분류를 위한 주문 매핑 필드 (주문의 box_class_cd에 복사할 필드명)
 * 	job.dps.station.wait-pool.count				작업 스테이션에 대기할 박스 최대 개수 (-1이면 무한대)
 * 	job.dps.batch.split-by-rack.enabled			호기별로 배치 분리 처리 여부
 * 	job.dps.sku.popula.rank.calc.days			SKU 물량 Rank 선정 기준 데이터 범위 (일자)
 * 	job.dps.pick.1box.enabled					완박스 바로 출고 대상 분류 여부
 * 	job.dps.pick.1sku.1pcs.enabled				단수 대상 분류 여부
 * 	job.dps.pick.1sku.npcs.enabled				단포 대상 분류 여부
 * 	job.dps.supple.recommend-cell.enabled		추천 로케이션 사용 여부
 * 	job.dps.pick.with-inspection.enabled		피킹과 동시에 검수 처리할 것인지 여부
 *  job.dps.preproces.cell.mapping.field        박스 요청 프로세스 사용 여부
 * 
 * @author shortstop
 */
public class DpsBatchJobConfigUtil extends BatchJobConfigUtil {

	/**
	 * 투입 박스 유형 - box / tray
	 * 
	 * @param batch
	 * @return
	 */
	public static String getInputBoxType(JobBatch batch) {
		// dps.input.box.type
		return getConfigValue(batch, DpsConfigConstants.DPS_INPUT_BOX_TYPE, true);
	}
	
	/**
	 * 소분류를 위해 박스에 할당할 대상 필드 (주문의 class_cd에 복사할 필드명 - 매장, 상품, 주문번호 …)
	 * 
	 * @param batch
	 * @return
	 */
	public static String getBoxMappingTargetField(JobBatch batch) {
		// dps.preproces.cell.mapping.field
		return getConfigValue(batch, DpsConfigConstants.DPS_PREPROCESS_CELL_MAPPING_FIELD, true);
	}
		
	/**
	 * 방면분류를 위한 주문 매핑 필드 (주문의 box_class_cd에 복사할 필드명)
	 * 
	 * @param batch
	 * @return
	 */
	public static String getBoxOutClassTargetField(JobBatch batch) {
		// dps.box.out.class.field
		return getBoxOutClassTargetField(batch, true);
	}
	
	/**
	 * 방면분류를 위한 주문 매핑 필드 (주문의 box_class_cd에 복사할 필드명)
	 * 
	 * @param batch
	 * @param exceptionWhenEmptyValue
	 * @return
	 */
	public static String getBoxOutClassTargetField(JobBatch batch, boolean exceptionWhenEmptyValue) {
		// dps.box.out.class.field
		return getConfigValue(batch, DpsConfigConstants.DPS_BOX_OUT_CLASS_FIELD, exceptionWhenEmptyValue);
	}
	
	/**
	 * 작업 스테이션에 대기할 박스 최대 개수 (-1이면 무한대)
	 * 
	 * @param batch
	 * @return
	 */
	public static int getStationWaitPoolCount(JobBatch batch) {
		// dps.station.wait-pool.count
		String intVal = getConfigValue(batch, DpsConfigConstants.DPS_STATION_WAIT_POOL_COUNT, true);
		return ValueUtil.toInteger(intVal);
	}
	
	/**
	 * 전체 랙을 하나의 배치로 운영할 지, 랙 별로 배치를 분리해서 운영할 지 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isSeparatedBatchByRack(JobBatch batch) {
		// dps.batch.split-by-rack.enabled
		String boolVal = getConfigValue(batch, DpsConfigConstants.DPS_BATCH_SPLIT_BY_RACK_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * SKU 물량 Rank 선정 기준 데이터 범위 (일자)
	 * 
	 * @param batch
	 * @return
	 */
	public static String getSkuRankingTargetDays(JobBatch batch) {
		// dps.sku.popula.rank.calc.days
		return getConfigValue(batch, DpsConfigConstants.DPS_SKU_POPULAR_RANK_CALC_DAYS, true);
	}
	
	/**
	 * 완박스 바로 출고 대상 분류 여부
	 * TODO 삭제
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isSingleOneBoxClassEnabled(JobBatch batch) {
		// dps.pick.1box.enabled
		String boolVal = getConfigValue(batch, DpsConfigConstants.DPS_PICK_1BOX_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * 단수 대상 분류 여부
	 * TODO 삭제
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isSingleSku1PcsClassEnabled(JobBatch batch) {
		// dps.pick.1sku.1pcs.enabled
		String boolVal = getConfigValue(batch, DpsConfigConstants.DPS_PICK_1SKU_1PCS_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * 단포 대상 분류 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isSingleSkuNpcsClassEnabled(JobBatch batch) {
		// dps.pick.1sku.npcs.enabled
		String boolVal = getConfigValue(batch, DpsConfigConstants.DPS_PICK_1SKU_NPCS_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * 추천 로케이션 사용 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isRecommendCellEnabled(JobBatch batch) {
		// dps.supple.recommend-cell.enabled
		String boolVal = getConfigValue(batch, DpsConfigConstants.DPS_SUPPLE_RECOMMEND_CELL_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * 피킹과 동시에 검수 처리할 것인지 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isPickingWithInspectionEnabled(JobBatch batch) {
		// dps.pick.with-inspection.enabled
		String boolVal = getConfigValue(batch, DpsConfigConstants.DPS_PICK_WITH_INSPECTION_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * DPS 박스 투입시 박스 ID 에서 박스 타입을 추출 하기 위한 SPLIT 인덱스 정보
	 * 
	 * @param batch
	 * @return
	 */
	public static String getBoxTypeSplitByBoxId(JobBatch batch) {
		// dps.input.box.type.split.index
		String splitIndex = getConfigValue(batch, DpsConfigConstants.DPS_INPUT_BOX_TYPE_SPLIT_INDEX, false);
		return splitIndex;
	}

	/**
	 * 박스 요청 프로세스 사용 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isBoxRequestProcessEnabled(JobBatch batch) {
		// dps.preprocess.box-request.enabled
		return isBoxRequestProcessEnabled(batch, true);
	}
	
	/**
	 * 박스 요청 프로세스 사용 여부
	 * 
	 * @param batch
	 * @param exceptionWhenEmptyValue
	 * @return
	 */
	public static boolean isBoxRequestProcessEnabled(JobBatch batch, boolean exceptionWhenEmptyValue) {
		// dps.preprocess.box-request.enabled
		String boolVal = getConfigValue(batch, DpsConfigConstants.DPS_PREPROCESS_BOX_REQUEST_ENABLED, exceptionWhenEmptyValue);
		return ValueUtil.toBoolean(boolVal);
	}
}
