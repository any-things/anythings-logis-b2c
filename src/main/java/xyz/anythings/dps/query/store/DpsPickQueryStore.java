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
	 * 피킹 작업 리스트를 조회
	 * 
	 * @return
	 */
	public String getSearchPickingJobListQuery() {
		return this.getQueryByPath("pick/SearchPickingJobList");
	}
	
	/**
	 * 단포 작업 화면 서머리 정보 조회 쿼리
	 * 
	 * @return
	 */
	public String getSinglePackInformQuery() {
		return this.getQueryByPath("pick/PickSinglePackInform");
	}
	
	/**
	 * 소분류 처리를 주문 데이터에 반영 하기 위한 업데이트 대상 리스트 조회 쿼리
	 * 
	 * @return
	 */
	/*public String getFindOrderQtyUpdateListQuery() {
		return this.getQueryByPath("pick/FindOrderQtyUpdateList");
	}*/

}
