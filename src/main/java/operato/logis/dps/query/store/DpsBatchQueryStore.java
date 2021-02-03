package operato.logis.dps.query.store;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * DPS 배치 쿼리 스토어
 * 
 * @author shortstop
 */
@Component
public class DpsBatchQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/dps/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/dps/query/ansi/";
	}
	
	/**
	 * 상위 시스템으로 부터 수신해야 할 주문 서머리 정보를 조회
	 * 
	 * @return
	 */
	public String getOrderSummaryToReceive() {
		return this.getQueryByPath("batch/OrderSummaryToReceive");
	}
	
	/**
	 * 작업 배치의 작업 진행율 조회
	 *
	 * @return
	 */
	public String getBatchProgressRateQuery() {
		return this.getQueryByPath("batch/BatchProgressRate");
	}
	
	/**
	 * 투입 가능 박스 리스트 조회 
	 * 
	 * @return
	 */
	public String getBatchInputableBoxByTypeQuery() {
		return this.getQueryByPath("batch/InputableBoxListByType");
	}

	/**
	 * 투입 가능 박스 수 쿼리
	 * 
	 * @return
	 */
	public String getBatchInputableBoxQuery() {
		return this.getQueryByPath("batch/InputableBoxCount");
	}
	
	/**
	 * 투입 리스트 쿼리
	 * 
	 * @return
	 */
	public String getBatchInputListQuery() {
		return this.getQueryByPath("batch/InputSeqList");
	}
	
	/**
	 * 투입 데이터 생성 쿼리
	 * 
	 * @return
	 */
	public String getBatchNewInputDataQuery() {
		return this.getQueryByPath("batch/NewInputData");
	}
	
	/**
	 * 작업 테이블에 박스 ID 및 투입 순서 정보 업데이트 쿼리
	 * 
	 * @return
	 */
	public String getBatchMapBoxIdAndSeqQuery() {
		return this.getQueryByPath("batch/BoxIdAndSeqMapping");
	}
	
	/**
	 * 작업의 투입 탭 리스트 쿼리
	 * 
	 * @return
	 */
	public String getBatchBoxInputTabListQuery() {
		return this.getQueryByPath("batch/BoxInputTabList");
	}
	
	/**
	 * 작업의 투입 탭 상세 리스트 쿼리
	 * 
	 * @return
	 */
	public String getBatchBoxInputTabDetailQuery() {
		return this.getQueryByPath("batch/BoxInputTabDetails");
	}

	/**
	 * 작업 마감을 위한 작업 데이터 요약 정보 조회
	 *
	 * @return
	 */
	public String getDpsBatchResultSummaryQuery() {
		return this.getQueryByPath("batch/BatchResultSummary");
	}
	
	/**
	 * 작업 지시 합포 대상 분류를 위한 쿼리 조회
	 *
	 * @return
	 */
	public String getDpsClassifyMultiOrders() {
		return this.getQueryByPath("batch/ClassifyMultiOrders");
	}
	
	/**
	 * 작업 지시 주문 정보 업데이트 쿼리 조회
	 *
	 * @return
	 */
	public String getDpsOrderStatusByInstruct() {
		return this.getQueryByPath("batch/UpdateOrderStatusByInstruct");
	}
	
	/**
	 * 작업 지시 배치 주문 수 업데이트 쿼리 조회
	 *
	 * @return
	 */
	public String getDpsUpdateOrderCount() {
		return this.getQueryByPath("batch/UpdateBatchOrderCount");
	}
}
