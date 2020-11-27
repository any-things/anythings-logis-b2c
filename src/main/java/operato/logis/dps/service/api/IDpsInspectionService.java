package operato.logis.dps.service.api;

import java.util.List;

import operato.logis.dps.entity.DpsBoxPack;
import operato.logis.dps.model.DpsInspItem;
import operato.logis.dps.model.DpsInspection;
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
	 * @param exceptionWhenEmpty
	 * @return
	 */
	public DpsInspection findInspectionByInput(JobBatch batch, String inputType, String inputId, boolean reprintMode, boolean exceptionWhenEmpty);
	
	/**
	 * 트레이 코드로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param trayCd
	 * @param reprintMode
	 * @param exceptionWhenEmpty
	 * @return
	 */
	public DpsInspection findInspectionByTray(JobBatch batch, String trayCd, boolean reprintMode, boolean exceptionWhenEmpty);
	
	/**
	 * 박스 ID로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param boxId
	 * @param reprintMode
	 * @param exceptionWhenEmpty
	 * @return
	 */
	public DpsInspection findInspectionByBox(JobBatch batch, String boxId, boolean reprintMode, boolean exceptionWhenEmpty);
	
	/**
	 * 송장 번호로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param invoiceId
	 * @param reprintMode
	 * @param exceptionWhenEmpty
	 * @return
	 */
	public DpsInspection findInspectionByInvoice(JobBatch batch, String invoiceId, boolean reprintMode, boolean exceptionWhenEmpty);
	
	/**
	 * 주문 번호로 검수 항목 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param orderNo
	 * @param reprintMode
	 * @param exceptionWhenEmpty
	 * @return
	 */
	public DpsInspection findInspectionByOrder(JobBatch batch, String orderNo, boolean reprintMode, boolean exceptionWhenEmpty);
	
	/**
	 * 송장 분할인 경우 주문 번호로 재출력을 위해 검수 리스트 조회
	 * 
	 * @param batch
	 * @param orderNo
	 * @param reprintMode
	 * @param exceptionWhenEmpty
	 * @return
	 */
	public List<DpsInspection> searchInspectionList(JobBatch batch, String orderNo, boolean reprintMode, boolean exceptionWhenEmpty);
	
	/**
	 * 박스 정보로 검수 항목 조회
	 * 
	 * @param box
	 * @param reprintMode
	 * @return
	 */
	public DpsInspection findInspectionByBoxPack(DpsBoxPack box, boolean reprintMode);

	/**
	 * 송장 ID로 검수 완료
	 * 
	 * @param batch
	 * @param invoiceId
	 * @param boxWeight 박스 무게
	 * @param printerId
	 * @param params 기타 파라미터 ...
	 */
	public void finishInspection(JobBatch batch, String invoiceId, Float boxWeight, String printerId, Object ... params);
	
	/**
	 * 박스 실적 정보로 검수 완료
	 * 
	 * @param batch
	 * @param box
	 * @param boxWeight 박스 무게
	 * @param printerId
	 */
	public void finishInspection(JobBatch batch, DpsBoxPack box, Float boxWeight, String printerId, Object ... params);
	
	/**
	 * 박스 분할
	 * 
	 * @param batch
	 * @param sourceBox
	 * @param inspectionItems
	 * @param printerId
	 * @param params
	 * @return 분할된 박스
	 */
	public DpsBoxPack splitBox(JobBatch batch, DpsBoxPack sourceBox, List<DpsInspItem> inspectionItems, String printerId, Object ... params);
		
	/**
	 * 박스 송장 라벨 발행
	 * 
	 * @param batch
	 * @param box
	 * @param printerId
	 * @return 출력 매수
	 */
	public int printInvoiceLabel(JobBatch batch, DpsBoxPack box, String printerId);
	
	/**
	 * 박스 송장 라벨 발행
	 * 
	 * @param batch
	 * @param inspection
	 * @param printerId
	 * @return 출력 매수
	 */
	public int printInvoiceLabel(JobBatch batch, DpsInspection inspection, String printerId);
	
	/**
	 * 거래명세서 출력 
	 * 
	 * @param batch
	 * @param box
	 * @param printerId
	 * @return 출력 매수
	 */
	public int printTradeStatement(JobBatch batch, DpsBoxPack box, String printerId);
	
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
	public void inspectionAction(DpsBoxPack box);

}
