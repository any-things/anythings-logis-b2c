package operato.logis.dps.query.store;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * DPS 박싱 관련 쿼리 스토어
 * 
 * @author shortstop
 */
@Component
public class DpsBoxQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/dps/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/dps/query/ansi/";
	}
	
	/**
	 * JobInput 기준으로 박스 ID 유니크 여부를 확인 하는 쿼리
	 * 
	 * @return
	 */
	public String getFindLatestBoxOfCellQuery() {
		return this.getQueryByPath("box/FindLatestBoxOfCell");
	}
	
	/**
	 * JobInput 기준으로 박스 ID 유니크 여부를 확인 하는 쿼리
	 * 
	 * @return
	 */
	public String getBoxIdUniqueCheckQuery() {
		return this.getQueryByPath("box/BoxIdUniqueCheck");
	}

}
