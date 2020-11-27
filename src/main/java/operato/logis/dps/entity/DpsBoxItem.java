package operato.logis.dps.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.AbstractStamp;

/**
 * dps_box_items 뷰 용 (읽기 전용)
 * 
 * @author shortstop
 */
@Table(name = "dps_box_items", ignoreDdl = true, idStrategy = GenerationRule.NONE)
public class DpsBoxItem extends AbstractStamp {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 886823091901249876L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;
	
	/**
	 * DPS 박스 ID
	 */
	@Column (name = "dps_box_pack_id", length = 40, nullable = false)
	private String dpsBoxPackId;
	
	/**
	 * 주문 번호
	 */
	@Column (name = "order_no", nullable = false, length = 40)
	private String orderNo;
	
	/**
	 * 작업 스테이션
	 */
	@Column (name = "station_cd", length = 30)
	private String stationCd;
	
	/**
	 * 셀 코드
	 */
	@Column (name = "sub_equip_cd", length = 30)
	private String subEquipCd;

	/**
	 * 상품 코드
	 */
	@Column (name = "sku_cd", nullable = false, length = 30)
	private String skuCd;
	
	/**
	 * 상품 바코드
	 */
	@Column (name = "sku_barcd", length = 30)
	private String skuBarcd;

	/**
	 * 상품 명
	 */
	@Column (name = "sku_nm", length = 200)
	private String skuNm;
	
	/**
	 * 상품 표준 중량
	 */
	@Column (name = "sku_wt", length = 15)
	private Float skuWt;

	/**
	 * 피킹 예정 수량
	 */
	@Column (name = "pick_qty", length = 10)
	private Integer pickQty;

	/**
	 * 피킹 완료 수량
	 */
	@Column (name = "picked_qty", length = 10)
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
