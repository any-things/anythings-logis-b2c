package operato.logis.dps.query.store;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * DPS 피킹 쿼리 스토어
 * 
 * @author shortstop
 */
@Component
public class DpsPickQueryStore extends AbstractQueryStore {
	
	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/dps/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/dps/query/ansi/"; 
	}

	/**
	 * 다음 맵핑할 작업 정보 조회
	 * 
	 * @return
	 */
	public String getFindNextMappingJobQuery() {
		return this.getQueryByPath("pick/FindNextMappingJob");
	}
	
	/**
	 * 피킹 작업 리스트를 조회
	 * 
	 * @return
	 */
	public String getSearchPickingJobListQuery() {
		return this.getQueryByPath("pick/SearchPickingJobList");
	}

}
