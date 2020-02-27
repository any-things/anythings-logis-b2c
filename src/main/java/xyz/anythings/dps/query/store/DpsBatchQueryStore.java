package xyz.anythings.dps.query.store;

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
		this.basePath = "xyz/anythings/dps/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "xyz/anythings/dps/query/ansi/"; 		
	}
	
	/**
	 * 투입 가능 박스 리스트 조회 
	 * 
	 * @return
	 */
	public String getBatchInputableBoxByTypeQuery() {
		return this.getQueryByPath("batch/BatchInputableBoxByType");
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
		return this.getQueryByPath("batch/InputList");
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
	public String getBatchBoxInputTabsQuery() {
		return this.getQueryByPath("batch/BatchBoxInputTabs");
	}
	
	/**
	 * 작업의 투입 탭 상세 리스트 쿼리
	 * 
	 * @return
	 */
	public String getBatchBoxInputTabDetailQuery() {
		return this.getQueryByPath("batch/BatchBoxInputTabDetail");
	}

}
