package xyz.anythings.dps.service.util;

import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.service.util.BatchJobConfigUtil;

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
 * 	job.dps.station.wait-pool.count				작업 스테이션에 대기할 박스 최대 개수 (-1이면 무한대)
 * 	job.dps.batch.split-by-rack.enabled			호기별로 배치 분리 처리 여부
 * 	job.dps.sku.popula.rank.calc.days			SKU 물량 Rank 선정 기준 데이터 범위 (일자)
 * 	job.dps.pick.1box.enabled					완박스 바로 출고 대상 분류 여부
 * 	job.dps.pick.1sku.1pcs.enabled				단수 대상 분류 여부
 * 	job.dps.pick.1sku.npcs.enabled				단포 대상 분류 여부
 * 	job.dps.supple.recommend-cell.enabled		추천 로케이션 사용 여부
 * 	job.dps.pick.with-inspection.enabled		피킹과 동시에 검수 처리할 것인지 여부
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
		// job.dps.input.box.type						
		return null;
	}
	
	/**
	 * 셀에 할당할 대상 필드 (매장, 상품, 주문번호 …)
	 * 
	 * @param batch
	 * @return
	 */
	public static String getCellMappingTargetField(JobBatch batch) {
		// job.dps.preproces.cell.mapping.field						
		return null;
	}
	
	/**
	 * 작업 스테이션에 대기할 박스 최대 개수 (-1이면 무한대)
	 * 
	 * @param batch
	 * @return
	 */
	public static int getStationWaitPoolCount(JobBatch batch) {
		// job.dps.station.wait-pool.count						
		return 0;
	}
	
	/**
	 * 전체 랙을 하나의 배치로 운영할 지, 랙 별로 배치를 분리해서 운영할 지 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isSeparatedBatchByRack(JobBatch batch) {
		// job.dps.batch.split-by-rack.enabled						
		return false;
	}
	
	/**
	 * SKU 물량 Rank 선정 기준 데이터 범위 (일자)
	 * 
	 * @param batch
	 * @return
	 */
	public static String getSkuRankingTargetDays(JobBatch batch) {
		//job.dps.sku.popula.rank.calc.days
		return null;
	}
	
	/**
	 * 완박스 바로 출고 대상 분류 여부
	 * TODO 삭제
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isSingleOneBoxClassEnabled(JobBatch batch) {
		//job.dps.pick.1box.enabled
		return false;
	}
	
	/**
	 * 단수 대상 분류 여부
	 * TODO 삭제
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isSingleSku1PcsClassEnabled(JobBatch batch) {
		//job.dps.pick.1sku.1pcs.enabled
		return false;
	}
	
	/**
	 * 단포 대상 분류 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isSingleSkuNpcsClassEnabled(JobBatch batch) {
		//job.dps.pick.1sku.npcs.enabled
		return false;
	}
	
	/**
	 * 추천 로케이션 사용 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isRecommendCellEnabled(JobBatch batch) {
		//job.dps.supple.recommend-cell.enabled		
		return false;
	}
	
	/**
	 * 피킹과 동시에 검수 처리할 것인지 여부
	 * 
	 * @param batch
	 * @return
	 */
	public static boolean isPickingWithInspectionEnabled(JobBatch batch) {
		//job.dps.pick.with-inspection.enabled		
		return false;
	}

}
