package operato.logis.dps.query.store;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * DPS 할당 관련 쿼리 스토어
 * 
 * @author shortstop
 */
@Component
public class DpsAssignQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/dps/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/dps/query/ansi/"; 
	}

	/**
	 * 작업을 할당할 재고 조회
	 * 
	 * @return
	 */
	public String getStockForJobAssignQuery() {
		return this.getQueryByPath("assign/StocksForJobAssign");
	}

	/**
	 * 재고 조회를 위한 주문 조회
	 * 
	 * @return
	 */
	public String getSearchOrderForStockQuery() {
		return this.getQueryByPath("assign/SearchOrdersForStock");
	}

	/**
	 * 작업 할당 대상 조회
	 * 
	 * @return
	 */
	public String getSearchAssignCandidatesQuery() {
		return this.getQueryByPath("assign/SearchAssignCandidates");
	}

	/**
	 * 작업 생성 쿼리
	 * 
	 * @return
	 */
	public String getAssignJobInstanceQuery() {
		return this.getQueryByPath("assign/AssignJobInstance");
	}

}
