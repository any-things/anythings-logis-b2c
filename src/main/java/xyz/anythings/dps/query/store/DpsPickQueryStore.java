package xyz.anythings.dps.query.store;

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
		this.basePath = "xyz/anythings/dps/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "xyz/anythings/dps/query/ansi/"; 
	}	

	/**
	 * DPS 피킹 작업 리스트를 조회
	 * 
	 * @return
	 */
	public String getSearchPickingJobListQuery() {
		return this.getQueryByPath("pick/SearchPickingJobList");
	}
	
}
