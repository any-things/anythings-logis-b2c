package xyz.anythings.dps.service.util;

import xyz.anythings.base.service.util.StageJobConfigUtil;
import xyz.anythings.dps.DpsConfigConstants;
import xyz.anythings.dps.DpsConstants;
import xyz.elidom.sys.util.ValueUtil;

/**
 * DPS 관련 Stage 공통 설정 프로파일
 * 
 * @author shortstop
 */
public class DpsStageJobConfigUtil extends StageJobConfigUtil {
	
	/**
	 * SKU 물량 Rank 선정 기준 데이터 범위 (일자)
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static String getSkuRankingTargetDays(String stageCd) {
		// dps.sku.popula.rank.calc.days
		return getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_SKU_POPULAR_RANK_CALC_DAYS, true);
	}

	/**
	 * DPS 배치 분리 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isSeparatedBatchByRack(String stageCd) {
		// dps.batch.split-by-rack.enabled
		String boolVal = getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_BATCH_SPLIT_BY_RACK_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * DPS 작업 스테이션에 대기할 박스 최대 개수 (-1이면 무한대)
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static int getStationWaitPoolCount(String stageCd) {
		// dps.station.wait-pool.count
		String intVal = getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_STATION_WAIT_POOL_COUNT, true);
		return ValueUtil.toInteger(intVal);
	}
	
	/**
	 * DPS 추천 로케이션 사용 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isRecommendCellEnabled(String stageCd) {
		// dps.supple.recommend-cell.enabled
		String boolVal = getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_SUPPLE_RECOMMEND_CELL_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * DPS 투입 박스 유형
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static String getInputBoxType(String stageCd) {
		// dps.input.box.type						
		return getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_INPUT_BOX_TYPE, true);
	}
	
	/**
	 * DPS 단포 대상 분류 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isSingleSkuNpcsClassEnabled(String stageCd) {
		// dps.pick.1sku.npcs.enabled					
		String boolVal = getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_PICK_1SKU_NPCS_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * DPS 피킹과 동시에 검수 처리할 것인지 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isPickingWithInspectionEnabled(String stageCd) {
		// dps.pick.with-inspection.enabled		
		String boolVal = getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_PICK_WITH_INSPECTION_ENABLED, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	/**
	 * DPS 작업 할당 스케줄러 사용 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isJobAssignSchedulingEnabled(String stageCd) {
		// dps.job-assign.scheduling.enabled		
		String boolVal = getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_JOB_ASSIGN_SCHEDULING_ENABLE, true);
		return ValueUtil.toBoolean(boolVal);
	}
	
	
	/**
	 * DPS 박스에에 할당할 대상 필드 (매장, 상품, 주문번호 …)
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static String getBoxMappingTargetField(String stageCd) {
		// dps.preproces.cell.mapping.field
		return getConfigValue(stageCd, DpsConstants.MODULE_DEFAULT_JOB_TYPE, DpsConfigConstants.DPS_PREPROCESS_CELL_MAPPING_FIELD, true);
	}

}
