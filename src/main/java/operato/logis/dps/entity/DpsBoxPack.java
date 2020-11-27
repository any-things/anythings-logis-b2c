package operato.logis.dps.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.AbstractStamp;

/**
 * dps_box_packs 뷰 용 (읽기 전용)
 * 
 * @author shortstop
 */
@Table(name = "dps_box_packs", ignoreDdl = true, idStrategy = GenerationRule.NONE)
public class DpsBoxPack extends AbstractStamp {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 886823091901247512L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;
	
	@Column(name = OrmConstants.TABLE_FIELD_DOMAIN_ID)
	private Long domainId;

	@Column (name = "batch_id", length = 40, nullable = false)
	private String batchId;
	
	@Column (name = "stage_cd", length = 30)
	private String stageCd;

	@Column (name = "job_date", nullable = false, length = 10)
	private String jobDate;

	@Column (name = "job_seq", nullable = false, length = 10)
	private String jobSeq;

	@Column (name = "com_cd", nullable = false, length = 30)
	private String comCd;
	
	@Column (name = "equip_cd", length = 30)
	private String equipCd;

	/**
	 * 주문 유형
	 */
	@Column (name = "order_type", length = 20)
	private String orderType;
	
	/**
	 * 원주문 번호
	 */
	@Column (name = "cust_order_no", length = 40)
	private String custOrderNo;
	
	/**
	 * 주문 번호
	 */
	@Column (name = "order_no", nullable = false, length = 40)
	private String orderNo;
	
	/**
	 * 송장 번호
	 */
	@Column (name = "invoice_id", length = 40)
	private String invoiceId;
	
	/**
	 * 박스 유형
	 */
	@Column (name = "box_type_cd", length = 30)
	private String boxTypeCd;
	
	/**
	 * 투입 순번
	 */
	@Column (name = "input_seq", length = 10)
	private Integer inputSeq;
	
	/**
	 * 박스 ID
	 */
	@Column (name = "box_id", length = 30)
	private String boxId;
	
	/**
	 * 순수 박스 중량 값
	 */
	@Column (name = "box_net_wt", length = 15)
	private Float boxNetWt;
	
	/**
	 * 박스 계산 중량 값
	 */
	@Column (name = "box_expect_wt", length = 15)
	private Float boxExpectWt;
	
	/**
	 * 박스 측정 중량 값
	 */
	@Column (name = "box_real_wt", length = 15)
	private Float boxRealWt;

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
	
	/**
	 * 검수 수량
	 */
	@Column (name = "inspected_qty", length = 10)
	private Integer inspectedQty;

	/**
	 * 소 분류 용
	 */
	@Column (name = "class_cd", length = 40)
	private String classCd;
	
	/**
	 * 방면 분류 용
	 */
	@Column (name = "box_class_cd", length = 40)
	private String boxClassCd;

	/**
	 * 피킹 작업 상태 - 작업 대기 > 투입 > 피킹 시작 > 피킹 완료 > 주문 완료 > 검수 완료 > 출고 완료
	 */
	@Column (name = "status", length = 10)
	private String status;
	
	/**
	 * 중량 검수 상태
	 */
	@Column (name = "auto_insp_status", length = 1)
	private String autoInspStatus;
	
	/**
	 * 수기 검수 상태
	 */
	@Column (name = "manual_insp_status", length = 1)
	private String manualInspStatus;
	
	/**
	 * 실적 전송 상태
	 */
	@Column (name = "report_status", length = 1)
	private String reportStatus;
	
	/**
	 * 박스 투입 시각
	 */
	@Column (name = "input_at", length = 22)
	private String inputAt;

	/**
	 * 피킹 시작 시각
	 */
	@Column (name = "pick_started_at", length = 22)
	private String pickStartedAt;

	/**
	 * 박싱 완료 시각
	 */
	@Column (name = "boxed_at", length = 22)
	private String boxedAt;
	
	/**
	 * 자동 검수 (예: 중량 검수) 시각
	 */
	@Column (name = "auto_inspected_at", length = 22)
	private String autoInspectedAt;

	/**
	 * 수기 검수 시각
	 */
	@Column (name = "manual_inspected_at", length = 22)
	private String manualInspectedAt;
	
	/**
	 * 최종 출고 시각
	 */
	@Column (name = "final_out_at", length = 22)
	private String finalOutAt;
	
	/**
	 * 실적 전송 시각
	 */
	@Column (name = "reported_at", length = 22)
	private String reportedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getDomainId() {
		return domainId;
	}

	public void setDomainId(Long domainId) {
		this.domainId = domainId;
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public String getStageCd() {
		return stageCd;
	}

	public void setStageCd(String stageCd) {
		this.stageCd = stageCd;
	}

	public String getJobDate() {
		return jobDate;
	}

	public void setJobDate(String jobDate) {
		this.jobDate = jobDate;
	}

	public String getJobSeq() {
		return jobSeq;
	}

	public void setJobSeq(String jobSeq) {
		this.jobSeq = jobSeq;
	}

	public String getComCd() {
		return comCd;
	}

	public void setComCd(String comCd) {
		this.comCd = comCd;
	}

	public String getEquipCd() {
		return equipCd;
	}

	public void setEquipCd(String equipCd) {
		this.equipCd = equipCd;
	}

	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public String getCustOrderNo() {
		return custOrderNo;
	}

	public void setCustOrderNo(String custOrderNo) {
		this.custOrderNo = custOrderNo;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public String getInvoiceId() {
		return invoiceId;
	}

	public void setInvoiceId(String invoiceId) {
		this.invoiceId = invoiceId;
	}

	public String getBoxTypeCd() {
		return boxTypeCd;
	}

	public void setBoxTypeCd(String boxTypeCd) {
		this.boxTypeCd = boxTypeCd;
	}

	public Integer getInputSeq() {
		return inputSeq;
	}

	public void setInputSeq(Integer inputSeq) {
		this.inputSeq = inputSeq;
	}

	public String getBoxId() {
		return boxId;
	}

	public void setBoxId(String boxId) {
		this.boxId = boxId;
	}

	public Float getBoxNetWt() {
		return boxNetWt;
	}

	public void setBoxNetWt(Float boxNetWt) {
		this.boxNetWt = boxNetWt;
	}

	public Float getBoxExpectWt() {
		return boxExpectWt;
	}

	public void setBoxExpectWt(Float boxExpectWt) {
		this.boxExpectWt = boxExpectWt;
	}

	public Float getBoxRealWt() {
		return boxRealWt;
	}

	public void setBoxRealWt(Float boxRealWt) {
		this.boxRealWt = boxRealWt;
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

	public Integer getInspectedQty() {
		return inspectedQty;
	}

	public void setInspectedQty(Integer inspectedQty) {
		this.inspectedQty = inspectedQty;
	}

	public String getClassCd() {
		return classCd;
	}

	public void setClassCd(String classCd) {
		this.classCd = classCd;
	}

	public String getBoxClassCd() {
		return boxClassCd;
	}

	public void setBoxClassCd(String boxClassCd) {
		this.boxClassCd = boxClassCd;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAutoInspStatus() {
		return autoInspStatus;
	}

	public void setAutoInspStatus(String autoInspStatus) {
		this.autoInspStatus = autoInspStatus;
	}

	public String getManualInspStatus() {
		return manualInspStatus;
	}

	public void setManualInspStatus(String manualInspStatus) {
		this.manualInspStatus = manualInspStatus;
	}

	public String getReportStatus() {
		return reportStatus;
	}

	public void setReportStatus(String reportStatus) {
		this.reportStatus = reportStatus;
	}

	public String getInputAt() {
		return inputAt;
	}

	public void setInputAt(String inputAt) {
		this.inputAt = inputAt;
	}

	public String getPickStartedAt() {
		return pickStartedAt;
	}

	public void setPickStartedAt(String pickStartedAt) {
		this.pickStartedAt = pickStartedAt;
	}

	public String getBoxedAt() {
		return boxedAt;
	}

	public void setBoxedAt(String boxedAt) {
		this.boxedAt = boxedAt;
	}

	public String getAutoInspectedAt() {
		return autoInspectedAt;
	}

	public void setAutoInspectedAt(String autoInspectedAt) {
		this.autoInspectedAt = autoInspectedAt;
	}

	public String getManualInspectedAt() {
		return manualInspectedAt;
	}

	public void setManualInspectedAt(String manualInspectedAt) {
		this.manualInspectedAt = manualInspectedAt;
	}

	public String getFinalOutAt() {
		return finalOutAt;
	}

	public void setFinalOutAt(String finalOutAt) {
		this.finalOutAt = finalOutAt;
	}

	public String getReportedAt() {
		return reportedAt;
	}

	public void setReportedAt(String reportedAt) {
		this.reportedAt = reportedAt;
	}

}
