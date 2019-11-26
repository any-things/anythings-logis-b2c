package xyz.anythings.dps.model;

/**
 * 단포 작업 화면 summary 모델 s
 * @author yang
 *
 */
public class DpsSinglePackInform {
	/**
	 * 선택된 상품에 대한 전체 박스 수 (주문 수 )
	 */
	private int totCnt;
	/**
	 * 선택된 상품에 대해 처리된 박스 수 ( 주문 수)
	 */
	private int compCnt;
	/**
	 * 단포 작업 예정 PCS 
	 */
	private int pickQty;
	/**
	 * 예정 수량의 박스 타입 
	 */
	private String boxTypeCd;
	/**
	 * 예정 PCS , 박스 타입의 주문수량 
	 */
	private int totOrderCnt;
	/**
	 *  예정 PCS , 박스타입의 완료 수량 
	 */
	private int compOrderCnt;
	/**
	 * 현재 선택된 예정 PCS, 박스타입 
	 */
	private boolean selectedJob;
	
	public int getTotCnt() {
		return totCnt;
	}
	public void setTotCnt(int totCnt) {
		this.totCnt = totCnt;
	}
	public int getCompCnt() {
		return compCnt;
	}
	public void setCompCnt(int compCnt) {
		this.compCnt = compCnt;
	}
	public int getPickQty() {
		return pickQty;
	}
	public void setPickQty(int pickQty) {
		this.pickQty = pickQty;
	}
	public String getBoxTypeCd() {
		return boxTypeCd;
	}
	public void setBoxTypeCd(String boxTypeCd) {
		this.boxTypeCd = boxTypeCd;
	}
	public int getTotOrderCnt() {
		return totOrderCnt;
	}
	public void setTotOrderCnt(int totOrderCnt) {
		this.totOrderCnt = totOrderCnt;
	}
	public int getCompOrderCnt() {
		return compOrderCnt;
	}
	public void setCompOrderCnt(int compOrderCnt) {
		this.compOrderCnt = compOrderCnt;
	}
	public boolean isSelectedJob() {
		return selectedJob;
	}
	public void setSelectedJob(boolean selectedJob) {
		this.selectedJob = selectedJob;
	}
}
