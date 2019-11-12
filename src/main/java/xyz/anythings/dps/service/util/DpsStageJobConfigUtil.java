package xyz.anythings.dps.service.util;

import xyz.anythings.base.service.util.StageJobConfigUtil;

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
		return null;
	}

	/**
	 * DPS 배치 분리 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isSeparatedBatchByRack(String stageCd) {
		// dps.batch.split-by-rack.enabled
		return false;
	}
	
	/**
	 * DPS 작업 스테이션에 대기할 박스 최대 개수 (-1이면 무한대)
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static String getStationWaitPoolCount(String stageCd) {
		// dps.station.wait-pool.count
		return null;
	}
	
	/**
	 * DPS 추천 로케이션 사용 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isRecommendCellEnabled(String stageCd) {
		// dps.supple.recommend-cell.enabled
		return false;
	}
	
	/**
	 * DPS 투입 박스 유형
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static String getInputBoxType(String stageCd) {
		// dps.input.box.type						
		return null;
	}
	
	/**
	 * DPS 단포 대상 분류 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isSingleSkuNpcsClassEnabled(String stageCd) {
		// dps.pick.1sku.npcs.enabled					
		return false;
	}
	
	/**
	 * DPS 피킹과 동시에 검수 처리할 것인지 여부
	 * 
	 * @param stageCd 스테이지 코드
	 * @return
	 */
	public static boolean isPickingWithInspectionEnabled(String stageCd) {
		// dps.pick.with-inspection.enabled		
		return false;
	}
}
