package xyz.anythings.dps.service.util;

import xyz.anythings.base.service.util.StageJobConfigUtil;

/**
 * DPS 관련 Stage 공통 설정 프로파일
 * 
 * @author shortstop
 */
public class DpsStageJobConfigUtil extends StageJobConfigUtil {
	
	/*job.dps.input.box.type					투입 박스 유형
	job.dps.preproces.cell.mapping.field		셀에 할당할 대상 필드 (매장, 상품, 주문번호 …) 
	job.dps.station.wait-pool.count				작업 스테이션에 대기할 박스 최대 개수 (-1이면 무한대)
	job.dps.batch.split-by-rack.enabled			호기별로 배치 분리 처리 여부
	job.dps.sku.popula.rank.calc.days			SKU 물량 Rank 선정 기준 데이터 범위 (일자)
	job.dps.pick.1box.enabled					완박스 바로 출고 대상 분류 여부
	job.dps.pick.1sku.1pcs.enabled				단수 대상 분류 여부
	job.dps.pick.1sku.npcs.enabled				단포 대상 분류 여부
	job.dps.supple.recommend-cell.enabled		추천 로케이션 사용 여부
	job.dps.pick.with-inspection.enabled		피킹과 동시에 검수 처리할 것인지 여부*/
	
}
