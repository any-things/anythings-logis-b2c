package operato.logis.dps.service.api;

import java.util.List;

import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.model.InspectionItem;
import xyz.anythings.base.model.OutInspection;

/**
 * DPS 출고 검수용 서비스
 * 
 * @author shortstop
 */
public interface IDpsInspectionService {
	
	/**
	 * 투입 박스 유형 (박스, 버킷)에 따라 박스 조회
	 * 
	 * @param domainId
	 * @param rackCd
	 * @param inputType - 주문 번호, 송장 번호, 박스 ID, 버킷 ID
	 * @param inputId - ID
	 * @return
	 */
	public BoxPack findBoxByInput(Long domainId, String rackCd, String inputType, String inputId);
	
	/**
	 * 버킷 코드로 박스 조회
	 * 
	 * @param domainId
	 * @param rackCd
	 * @param bucketCd
	 * @return
	 */
	public BoxPack findBoxByBucket(Long domainId, String rackCd, String bucketCd);
	
	/**
	 * 박스 ID로 박스 조회
	 * 
	 * @param domainId
	 * @param rackCd
	 * @param boxId
	 * @return
	 */
	public BoxPack findBoxByBox(Long domainId, String rackCd, String boxId);
	
	/**
	 * 송장 번호로 박스 조회
	 * 
	 * @param domainId
	 * @param rackCd
	 * @param invoiceNo
	 * @return
	 */
	public BoxPack findBoxByInvoice(Long domainId, String rackCd, String invoiceNo);

	/**
	 * 투입 박스 유형 (주문 번호, 송장 번호, 박스 ID, 버킷 ID)에 따라 검수 항목 조회
	 * 
	 * @param domainId
	 * @param rackCd
	 * @param inputType - 주문 번호, 송장 번호, 박스 ID, 버킷 ID
	 * @param inputId - ID
	 * @return
	 */
	public OutInspection findInspectionByInput(Long domainId, String rackCd, String inputType, String inputId);
	
	/**
	 * 버킷 코드로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param rackCd
	 * @param bucketCd
	 * @return
	 */
	public OutInspection findInspectionByBucket(Long domainId, String rackCd, String bucketCd);
	
	/**
	 * 박스 ID로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param rackCd
	 * @param boxId
	 * @return
	 */
	public OutInspection findInspectionByBox(Long domainId, String rackCd, String boxId);
	
	/**
	 * 송장 번호로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param rackCd
	 * @param invoiceNo
	 * @return
	 */
	public OutInspection findInspectionByInvoice(Long domainId, String rackCd, String invoiceNo);
	
	/**
	 * 박스 정보로 검수 항목 조회
	 * 
	 * @param box
	 * @return
	 */
	public OutInspection findInspectionByBoxPack(BoxPack box);
	
	/**
	 * 박스 ID, 상품 코드로 검수 수행
	 * 
	 * @param domainId
	 * @param boxPackId
	 * @param skuCd
	 * @return
	 */
	public OutInspection doInspection(Long domainId, String boxPackId, String skuCd);
	
	/**
	 * 송장 ID로 검수 완료
	 * 
	 * @param box
	 * @param boxWeight 박스 무게
	 * @param printerId
	 * @return
	 */
	public BoxPack finishInspection(BoxPack box, Float boxWeight, String printerId);
	
	/**
	 * 박스 분할
	 * 
	 * @param sourceBox
	 * @param inspectionItems
	 * @param printerId
	 * @return
	 */
	public BoxPack splitBox(BoxPack sourceBox, List<InspectionItem> inspectionItems, String printerId);
	
	/**
	 * 박스 송장 라벨 발행
	 * 
	 * @param box
	 * @param printerId
	 * @return
	 */
	public void printInvoiceLabel(BoxPack box, String printerId);
	
	/**
	 * 거래명세서 출력 
	 * 
	 * @param box
	 * @param printerId
	 * @return
	 */
	public void printTradeStatement(BoxPack box, String printerId);

}
