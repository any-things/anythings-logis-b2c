package operato.logis.dps.model;

/**
 * DPS 작업 할당을 위한 데이터 모델
 * 
 * @author shortstop
 */
public class DpsJobAssign {

	/**
	 * 재고 ID
	 */
	private String stockId;
	/**
	 * 작업 배치 ID
	 */
	private String batchId;
	/**
	 * 주문 할당 순위
	 */
	private Integer ranking;
	/**
	 * 설비 유형
	 */
	private String equipType;
	/**
	 * 설비 코드
	 */
	private String equipCd;
	/**
	 * 주문 번호
	 */
	private String orderNo;
	/**
	 * 셀 번호
	 */
	private String cellCd;
	/**
	 * 표시기 코드
	 */
	private String indCd;
	/**
	 * 고객사 코드
	 */
	private String comCd;
	/**
	 * 상품 코드
	 */
	private String skuCd;
	/**
	 * 주문 수량
	 */
	private Integer orderQty;
	/**
	 * 재고 적치 수량 
	 */
	private Integer loadQty;
	/**
	 * 재고 적치 수량 
	 */
	private Integer loadSumByCell;
	/**
	 * 할당 가능한 지 여부
	 */
	private Integer checkAssignable;
	
	public String getStockId() {
		return stockId;
	}
	
	public void setStockId(String stockId) {
		this.stockId = stockId;
	}
	
	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public Integer getRanking() {
		return ranking;
	}
	
	public void setRanking(Integer ranking) {
		this.ranking = ranking;
	}
	
	public String getEquipType() {
		return equipType;
	}

	public void setEquipType(String equipType) {
		this.equipType = equipType;
	}

	public String getEquipCd() {
		return equipCd;
	}

	public void setEquipCd(String equipCd) {
		this.equipCd = equipCd;
	}

	public String getOrderNo() {
		return orderNo;
	}
	
	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}
	
	public String getCellCd() {
		return cellCd;
	}
	
	public void setCellCd(String cellCd) {
		this.cellCd = cellCd;
	}
	
	public String getIndCd() {
		return indCd;
	}

	public void setIndCd(String indCd) {
		this.indCd = indCd;
	}

	public String getComCd() {
		return comCd;
	}
	
	public void setComCd(String comCd) {
		this.comCd = comCd;
	}
	
	public String getSkuCd() {
		return skuCd;
	}
	
	public void setSkuCd(String skuCd) {
		this.skuCd = skuCd;
	}
	
	public Integer getOrderQty() {
		return orderQty;
	}

	public void setOrderQty(Integer orderQty) {
		this.orderQty = orderQty;
	}

	public Integer getLoadQty() {
		return loadQty;
	}
	
	public void setLoadQty(Integer loadQty) {
		this.loadQty = loadQty;
	}
	
	public Integer getLoadSumByCell() {
		return loadSumByCell;
	}
	
	public void setLoadSumByCell(Integer loadSumByCell) {
		this.loadSumByCell = loadSumByCell;
	}

	public Integer getCheckAssignable() {
		return checkAssignable;
	}

	public void setCheckAssignable(Integer checkAssignable) {
		this.checkAssignable = checkAssignable;
	}

}
