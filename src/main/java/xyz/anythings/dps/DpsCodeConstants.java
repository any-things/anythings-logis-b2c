package xyz.anythings.dps;

import xyz.anythings.base.LogisCodeConstants;

/**
 * DPS 모듈 상수 정의 
 * 
 * @author yang
 */
public class DpsCodeConstants extends LogisCodeConstants {		
	
	
	/**
	 * DPS 가공 ( 박스투입 맵핑 ) 기준 주문 
	 */
	public static final String DPS_PREPROCESS_COL_ORDER = "ORDER";	
	/**
	 * DPS 가공 ( 박스투입 맵핑 ) 기준 매장  
	 */
	public static final String DPS_PREPROCESS_COL_SHOP = "SHOP";
	
	
	/**
	 * DPS 대상 분류 : 합포 
	 */
	public static final String DPS_ORDER_TYPE_MT = "MT";
	/**
	 * DPS 대상 분류 : 단포 
	 */
	public static final String DPS_ORDER_TYPE_OT = "MT";
}
