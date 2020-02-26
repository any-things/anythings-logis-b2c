package xyz.anythings.dps.query.store;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * DPS 배치 쿼리 스토어
 * 
 * @author yang
 */
@Component
public class DpsBatchQueryStore extends AbstractQueryStore {
	
	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "xyz/anythings/dps/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "xyz/anythings/dps/query/ansi/"; 
	}	

	/**
	 * DPS 투입 가능 박스 리스트 조회 
	 * 
	 * @return
	 */
	public String getBatchInputableBoxByTypeQuery() {
		return this.getQueryByPath("batch/BatchInputableBoxByType");
	}
	
}
