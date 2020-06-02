package operato.logis.dps.model;

import xyz.anythings.base.entity.SKU;

/**
 * DPS 검수 항목 모델
 * 
 * @author shortstop
 */
public class DpsInspItem {
	/**
	 * 상품 코드 
	 */
	private String skuCd;
	/**
	 * 상품 명 
	 */
	private String skuNm;
	/**
	 * 상품 바코드 
	 */
	private String skuBarcd;
	/**
	 * 피킹 수량 
	 */
	private Integer pickedQty;
	/**
	 * 검수 확인 수량
	 */
	private Integer confirmQty;
	/**
	 * 상품 중량
	 */
	private Float skuWeight;
	
	public DpsInspItem() {
	}
	
	public DpsInspItem(String skuCd, String skuNm, String skuBarcd, Integer pickedQty, Float skuWeight) {
		this.skuCd = skuCd;
		this.skuNm = skuNm;
		this.skuBarcd = skuBarcd;
		this.pickedQty = pickedQty;
		this.setSkuWeight(skuWeight);
	}
	
	public DpsInspItem(String skuCd, String skuNm, String skuBarcd, Integer pickedQty) {
		this.skuCd = skuCd;
		this.skuNm = skuNm;
		this.skuBarcd = skuBarcd;
		this.pickedQty = pickedQty;
	}
	
	public DpsInspItem(SKU sku, Integer pickedQty) {
		this.skuCd = sku.getSkuCd();
		this.skuNm = sku.getSkuNm();
		this.skuBarcd = sku.getSkuBarcd();
		this.pickedQty = pickedQty;
	}
	
	public String getSkuCd() {
		return skuCd;
	}

	public void setSkuCd(String skuCd) {
		this.skuCd = skuCd;
	}

	public String getSkuNm() {
		return skuNm;
	}

	public void setSkuNm(String skuNm) {
		this.skuNm = skuNm;
	}

	public String getSkuBarcd() {
		return skuBarcd;
	}

	public void setSkuBarcd(String skuBarcd) {
		this.skuBarcd = skuBarcd;
	}

	public Integer getPickedQty() {
		return pickedQty;
	}

	public void setPickedQty(Integer pickedQty) {
		this.pickedQty = pickedQty;
	}

	public Integer getConfirmQty() {
		return confirmQty;
	}

	public void setConfirmQty(Integer confirmQty) {
		this.confirmQty = confirmQty;
	}

	public Float getSkuWeight() {
		return skuWeight;
	}

	public void setSkuWeight(Float skuWeight) {
		this.skuWeight = skuWeight;
	}

}
