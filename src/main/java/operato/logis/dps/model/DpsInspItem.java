package operato.logis.dps.model;

/**
 * DPS 검수 항목 모델
 * 
 * @author shortstop
 */
public class DpsInspItem {
	
	/**
	 * 박스 내품 ID
	 */
	private String id;
	/**
	 * DPS 박스 ID
	 */
	private String dpsBoxPackId;
	/**
	 * 주문 번호
	 */
	private String orderNo;
	/**
	 * 작업 스테이션
	 */
	private String stationCd;
	/**
	 * 셀 코드
	 */
	private String subEquipCd;
	/**
	 * 상품 코드
	 */
	private String skuCd;
	/**
	 * 상품 바코드
	 */
	private String skuBarcd;
	/**
	 * 상품 명
	 */
	private String skuNm;
	/**
	 * 상품 표준 중량
	 */
	private Float skuWt;
	/**
	 * 피킹 예정 수량
	 */
	private Integer pickQty;
	/**
	 * 피킹 완료 수량
	 */
	private Integer pickedQty;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDpsBoxPackId() {
		return dpsBoxPackId;
	}

	public void setDpsBoxPackId(String dpsBoxPackId) {
		this.dpsBoxPackId = dpsBoxPackId;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public String getStationCd() {
		return stationCd;
	}

	public void setStationCd(String stationCd) {
		this.stationCd = stationCd;
	}

	public String getSubEquipCd() {
		return subEquipCd;
	}

	public void setSubEquipCd(String subEquipCd) {
		this.subEquipCd = subEquipCd;
	}

	public String getSkuCd() {
		return skuCd;
	}

	public void setSkuCd(String skuCd) {
		this.skuCd = skuCd;
	}

	public String getSkuBarcd() {
		return skuBarcd;
	}

	public void setSkuBarcd(String skuBarcd) {
		this.skuBarcd = skuBarcd;
	}

	public String getSkuNm() {
		return skuNm;
	}

	public void setSkuNm(String skuNm) {
		this.skuNm = skuNm;
	}

	public Float getSkuWt() {
		return skuWt;
	}

	public void setSkuWt(Float skuWt) {
		this.skuWt = skuWt;
	}

	public Integer getPickQty() {
		return pickQty;
	}

	public void setPickQty(Integer pickQty) {
		this.pickQty = pickQty;
	}

	public Integer getPickedQty() {
		return pickedQty;
	}

	public void setPickedQty(Integer pickedQty) {
		this.pickedQty = pickedQty;
	}

}
