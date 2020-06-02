package operato.logis.dps.service.api;

import java.util.List;

import operato.logis.dps.model.DpsInspItem;
import operato.logis.dps.model.DpsInspection;
import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.JobBatch;

/**
 * DPS 출고 검수용 서비스
 * 
 * @author shortstop
 */
public interface IDpsInspectionService {

	/**
	 * 투입 박스 유형 (주문 번호, 송장 번호, 박스 ID, 버킷 ID)에 따라 검수 항목 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param inputType - 주문 번호, 송장 번호, 박스 ID, 버킷 ID
	 * @param inputId - ID
	 * @return
	 */
	public DpsInspection findInspectionByInput(JobBatch batch, String inputType, String inputId);
	
	/**
	 * 트레이 코드로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param trayCd
	 * @return
	 */
	public DpsInspection findInspectionByTray(JobBatch batch, String trayCd);
	
	/**
	 * 박스 ID로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param boxId
	 * @return
	 */
	public DpsInspection findInspectionByBox(JobBatch batch, String boxId);
	
	/**
	 * 송장 번호로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param invoiceId
	 * @return
	 */
	public DpsInspection findInspectionByInvoice(JobBatch batch, String invoiceId);
	
	/**
	 * 박스 정보로 검수 항목 조회
	 * 
	 * @param box
	 * @return
	 */
	public DpsInspection findInspectionByBoxPack(BoxPack box);

	/**
	 * 송장 ID로 검수 완료
	 * 
	 * @param batch
	 * @param invoiceId
	 * @param boxWeight 박스 무게
	 * @param printerId
	 */
	public void finishInspection(JobBatch batch, String invoiceId, Float boxWeight, String printerId);
	
	/**
	 * 박스 실적 정보로 검수 완료
	 * 
	 * @param box
	 * @param boxWeight 박스 무게
	 * @param printerId
	 */
	public void finishInspection(BoxPack box, Float boxWeight, String printerId);
	
	/**
	 * 박스 분할
	 * 
	 * @param sourceBox
	 * @param inspectionItems
	 * @param printerId
	 * @return 분할된 박스
	 */
	public BoxPack splitBox(BoxPack sourceBox, List<DpsInspItem> inspectionItems, String printerId);
	
	/**
	 * 박스 송장 라벨 발행
	 * 
	 * @param batch
	 * @param box
	 * @param printerId
	 * @return 출력 매수
	 */
	public int printInvoiceLabel(JobBatch batch, BoxPack box, String printerId);
	
	/**
	 * 거래명세서 출력 
	 * 
	 * @param batch
	 * @param box
	 * @param printerId
	 * @return 출력 매수
	 */
	public int printTradeStatement(JobBatch batch, BoxPack box, String printerId);
	
	/**
	 * 출고 검수 후 액션 처리
	 * 
	 * @param domainId
	 * @param boxPackId
	 */
	public void inspectionAction(Long domainId, String boxPackId);
	
	/**
	 * 출고 검수 후 액션 처리
	 * 
	 * @param box
	 */
	public void inspectionAction(BoxPack box);

}
