package operato.logis.dps.model;

import java.util.ArrayList;
import java.util.List;

import xyz.elidom.core.entity.Code;
import xyz.elidom.core.entity.CodeDetail;
import xyz.elidom.core.rest.CodeController;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.ValueUtil;
import xyz.elidom.util.BeanUtil;

/**
 * DPS 용 출고 검수 모델 
 * 
 * @author shortstop
 */
public class DpsInspection {
	
	/**
	 * 박스 ID
	 */
	private String id;
	/**
	 * 도메인 ID
	 */
	private Long domainId;
	/**
	 * 배치 ID
	 */
	private String batchId;
	/**
	 * 스테이션 코드
	 */
	private String stageCd;
	/**
	 * 작업 일자
	 */
	private String jobDate;
	/**
	 * 작업 순번
	 */
	private String jobSeq;
	/**
	 * 고객사 코드
	 */
	private String comCd;
	/**
	 * 설비 코드
	 */
	private String equipCd;
	/**
	 * 주문 유형
	 */
	private String orderType;
	/**
	 * 원주문 번호
	 */
	private String custOrderNo;
	/**
	 * 주문 번호
	 */
	private String orderNo;
	/**
	 * 송장 번호
	 */
	private String invoiceId;
	/**
	 * 박스 유형
	 */
	private String boxType;
	/**
	 * 박스 유형 코드
	 */
	private String boxTypeCd;
	/**
	 * 투입 순번
	 */
	private Integer inputSeq;
	/**
	 * 박스 ID
	 */
	private String boxId;
	/**
	 * 순수 박스 중량 값
	 */
	private Float boxNetWt;
	/**
	 * 박스 계산 중량 값
	 */
	private Float boxExpectWt;
	/**
	 * 박스 측정 중량 값
	 */
	private Float boxRealWt;
	/**
	 * 피킹 예정 수량
	 */
	private Integer pickQty;
	/**
	 * 피킹 완료 수량
	 */
	private Integer pickedQty;
	/**
	 * 소 분류 용
	 */
	private String classCd;
	/**
	 * 방면 분류 용
	 */
	private String boxClassCd;
	/**
	 * 피킹 작업 상태 - W 작업 대기 > I 투입 > P 피킹 시작 > F 피킹 완료 > B 주문 완료 > E 검수 완료 > O 출고 완료
	 */
	private String status;
	/**
	 * 피킹 작업 상태 - W 작업 대기 > I 투입 > P 피킹 시작 > F 피킹 완료 > B 주문 완료 > E 검수 완료 > O 출고 완료
	 */
	private String statusStr;
	/**
	 * 중량 검수 상태
	 */
	private String autoInspStatus;
	/**
	 * 수기 검수 상태
	 */
	private String manualInspStatus;
	/**
	 * 실적 전송 상태
	 */
	private String reportStatus;
	/**
	 * 박스 투입 시각
	 */
	private String inputAt;
	/**
	 * 피킹 시작 시각
	 */
	private String pickStartedAt;
	/**
	 * 박싱 완료 시각
	 */
	private String boxedAt;
	/**
	 * 자동 검수 (예: 중량 검수) 시각
	 */
	private String autoInspectedAt;
	/**
	 * 수기 검수 시각
	 */
	private String manualInspectedAt;
	/**
	 * 최종 출고 시각
	 */
	private String finalOutAt;
	/**
	 * 실적 전송 시각
	 */
	private String reportedAt;
	/**
	 * 내품 내역
	 */
	private List<DpsInspItem> items;
	
	public DpsInspection() {
	}

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

	public String getBoxType() {
		return boxType;
	}

	public void setBoxType(String boxType) {
		this.boxType = boxType;
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

	public String getStatusStr() {
		if(this.statusStr == null && this.status != null) {
			Code code = BeanUtil.get(CodeController.class).findOne(SysConstants.SHOW_BY_NAME_METHOD, "JOB_STATUS");
			if(code != null) {
				List<CodeDetail> codeItems = code.getItems();
				for(CodeDetail item : codeItems) {
					if(ValueUtil.isEqualIgnoreCase(item.getName(), this.status)) {
						this.statusStr = item.getDescription();
						break;
					}
				}
			}
		}
		
		return statusStr;
	}

	public void setStatusStr(String statusStr) {
		this.statusStr = statusStr;
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

	public List<DpsInspItem> getItems() {
		return items;
	}

	public void setItems(List<DpsInspItem> items) {
		this.items = items;
	}
	
	public void addItem(DpsInspItem item) {
		if(this.items == null) {
			this.items = new ArrayList<DpsInspItem>();
		}
		
		this.items.add(item);
	}

}
