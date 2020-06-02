package operato.logis.dps.query.store;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * DPS 출고 검수용 쿼리 스토어
 * 
 * @author shortstop
 */
@Component
public class DpsInspectionQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/dps/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/dps/query/ansi/"; 
	}

	/**
	 * 검수 정보 조회
	 * 
	 * @return
	 */
	public String getFindInspectionQuery() {
		return this.getQueryByPath("inspection/FindInspection");
	}
	
	/**
	 * 검수 항목 조회
	 * 
	 * @return
	 */
	public String getSearchInspectionItemsQuery() {
		return this.getQueryByPath("inspection/SearchInspectionItems");
	}

}
