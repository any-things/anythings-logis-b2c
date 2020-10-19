package operato.logis.dps.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import operato.logis.dps.model.DpsInspItem;
import operato.logis.dps.model.DpsInspection;
import operato.logis.dps.query.store.DpsInspectionQueryStore;
import operato.logis.dps.service.api.IDpsInspectionService;
import xyz.anythings.base.entity.BoxPack;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.TrayBox;
import xyz.anythings.base.service.impl.AbstractInstructionService;
import xyz.anythings.base.service.util.BatchJobConfigUtil;
import xyz.anythings.sys.event.model.PrintEvent;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.ValueUtil;
import xyz.elidom.util.DateUtil;

/**
 * DPS 출고 검수 서비스
 * 
 * @author shortstop
 */
@Component("dpsInspectionService")
public class DpsInspectionService extends AbstractInstructionService implements IDpsInspectionService {

	/**
	 * DPS 출고 검수 처리용 쿼리 스토어
	 */	
	@Autowired
	private DpsInspectionQueryStore dpsInspectionQueryStore;
	
	/**
	 * 박스 조회
	 * 
	 * @param domainId
	 * @param batchId
	 * @param invoiceId
	 * @param exceptionWhenEmpty
	 * @return
	 */
	private BoxPack findBoxByInvoiceId(Long domainId, String batchId, String invoiceId, boolean exceptionWhenEmpty) {
		
		BoxPack box = AnyEntityUtil.findEntityBy(domainId, false, BoxPack.class, null, "batchId,invoiceId", batchId, invoiceId);
		
		if(box == null && exceptionWhenEmpty) {
			throw ThrowUtil.newNotFoundRecord("terms.menu.BoxPack", invoiceId);
		}
		
		return box;
	}
	
	/**
	 * 검수 정보 조회
	 * 
	 * @param sql
	 * @param params
	 * @param exceptionWhenEmpty
	 * @return
	 */
	private DpsInspection findInspection(String sql, Map<String, Object> params, boolean exceptionWhenEmpty) {
		
		DpsInspection inspection = this.queryManager.selectBySql(sql, params, DpsInspection.class);
		
		if(inspection == null) {
			if(exceptionWhenEmpty) {
				Object data = (params == null) ? null : (params.containsKey("boxId") ? params.get("boxId") : (params.containsKey("orderNo") ? params.get("orderNo") : (params.containsKey("invoiceId") ? params.get("invoiceId") : null)));
				throw ThrowUtil.newNotFoundRecord("terms.label.inspection", ValueUtil.toString(data));
			} else {
				return null;
			}
		} else {
			return inspection;
		}
	}
	
	/**
	 * 검수 항목 조회 처리 ...
	 * 
	 * @param inspection
	 * @param params
	 * @return
	 */
	private DpsInspection searchInpsectionItems(DpsInspection inspection, Map<String, Object> params) {
		
		if(inspection == null) {
			return null;
		}
		
		if(!params.containsKey("invoiceId")) {
			params.put("invoiceId", inspection.getInvoiceId());
		}
		
		String sql = this.dpsInspectionQueryStore.getSearchInspectionItemsQuery();
		List<DpsInspItem> items = this.queryManager.selectListBySql(sql, params, DpsInspItem.class, 0, 0);
		inspection.setItems(items);
		return inspection;
	}
	
	@Override
	public DpsInspection findInspectionByInput(JobBatch batch, String inputType, String inputId, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId());
		
		if(ValueUtil.isEqualIgnoreCase(inputType, "box") || ValueUtil.isEqualIgnoreCase(inputType, "tray")) {
			params.put("boxId", inputId);
			
		} else if(ValueUtil.isEqualIgnoreCase(inputType, "orderNo")) {
			params.put("orderNo", inputId);
			
		} else if(ValueUtil.isEqualIgnoreCase(inputType, "invoiceId")) {
			params.put("invoiceId", inputId);
		}
		
		params.put("status", reprintMode ? BoxPack.BOX_STATUS_EXAMED : BoxPack.BOX_STATUS_BOXED);

		DpsInspection inspection = this.findInspection(sql, params, exceptionWhenEmpty);
		if(inspection != null && (ValueUtil.isEqualIgnoreCase(inputType, "box") || ValueUtil.isEqualIgnoreCase(inputType, "tray"))) {
			inspection.setBoxType(inputType);
		}
		
		return this.searchInpsectionItems(inspection, params);
	}

	@Override
	public DpsInspection findInspectionByTray(JobBatch batch, String trayCd, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,boxId", batch.getDomainId(), batch.getId(), trayCd);
		params.put("status", reprintMode ? BoxPack.BOX_STATUS_EXAMED : BoxPack.BOX_STATUS_BOXED);
		DpsInspection inspection = this.findInspection(sql, params, exceptionWhenEmpty);

		if(inspection != null) {
			inspection.setBoxType("tray");
		}
		
		return this.searchInpsectionItems(inspection, params);
	}

	@Override
	public DpsInspection findInspectionByBox(JobBatch batch, String boxId, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,boxId", batch.getDomainId(), batch.getId(), boxId);
		params.put("status", reprintMode ? BoxPack.BOX_STATUS_EXAMED : BoxPack.BOX_STATUS_BOXED);
		DpsInspection inspection = this.findInspection(sql, params, exceptionWhenEmpty);

		if(inspection != null) {
			inspection.setBoxType("box");
		}
		
		return this.searchInpsectionItems(inspection, params);
	}

	@Override
	public DpsInspection findInspectionByInvoice(JobBatch batch, String invoiceId, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,invoiceId", batch.getDomainId(), batch.getId(), invoiceId);
		params.put("status", reprintMode ? BoxPack.BOX_STATUS_EXAMED : BoxPack.BOX_STATUS_BOXED);
		DpsInspection inspection = this.findInspection(sql, params, exceptionWhenEmpty);
		return this.searchInpsectionItems(inspection, params);
	}
	
	@Override
	public DpsInspection findInspectionByOrder(JobBatch batch, String orderNo, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,orderNo", batch.getDomainId(), batch.getId(), orderNo);
		params.put("status", reprintMode ? BoxPack.BOX_STATUS_EXAMED : BoxPack.BOX_STATUS_BOXED);
		DpsInspection inspection = this.findInspection(sql, params, exceptionWhenEmpty);
		return this.searchInpsectionItems(inspection, params);
	}
	
	@Override
	public List<DpsInspection> searchInspectionList(JobBatch batch, String orderNo, boolean reprintMode, boolean exceptionWhenEmpty) {
		String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,orderNo", batch.getDomainId(), batch.getId(), orderNo);
		params.put("status", reprintMode ? BoxPack.BOX_STATUS_EXAMED : BoxPack.BOX_STATUS_BOXED);
		return this.queryManager.selectListBySql(sql, params, DpsInspection.class, 0, 0);
	}

	@Override
	public DpsInspection findInspectionByBoxPack(BoxPack box, boolean reprintMode) {
		
		String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId", box.getDomainId(), box.getBatchId());
		params.put("status", reprintMode ? BoxPack.BOX_STATUS_EXAMED : BoxPack.BOX_STATUS_BOXED);
		
		if(ValueUtil.isNotEmpty(box.getInvoiceId())) {
			params.put("invoiceId", box.getInvoiceId());
			
		} else if(ValueUtil.isNotEmpty(box.getOrderNo())) {
			params.put("orderNo", box.getOrderNo());
			
		} else if(ValueUtil.isNotEmpty(box.getBoxId())) {
			params.put("boxId", box.getBoxId());
		} 
		
		DpsInspection inspection = this.findInspection(sql, params, true);
		return this.searchInpsectionItems(inspection, params);
	}

	@Override
	public void finishInspection(JobBatch batch, String invoiceId, Float boxWeight, String printerId, Object ... params) {
		
		// 박스 조회
		BoxPack box = this.findBoxByInvoiceId(batch.getDomainId(), batch.getId(), invoiceId, true);
		// 검수 완료 처리 
		this.finishInspection(batch, box, boxWeight, printerId);
	}

	@Override
	public void finishInspection(JobBatch batch, BoxPack box, Float boxWeight, String printerId, Object ... params) {
		
		// 1. 박스 검수 완료 처리
		if(boxWeight != null) {
			box.setBoxWt(boxWeight);
		}
		box.setStatus(BoxPack.BOX_STATUS_EXAMED);
		box.setInspectorId(User.currentUser().getId());
		box.setInspEndedAt(DateUtil.currentTimeStr());
		
		if(ValueUtil.isEmpty(box.getInspStartedAt())) {
			box.setInspStartedAt(box.getInspEndedAt());
		}
		
		this.queryManager.update(box, "boxWt", "status", "inspectionId", "inspStartedAt", "inspEndedAt", "updaterId", "updatedAt");
		
		// 2. 박스 내품 검수 항목 완료 처리
		Map<String, Object> boxItemParams = ValueUtil.newMap("domainId,batchId,boxPackId,status,updaterId,now", box.getDomainId(), box.getBatchId(), box.getId(), BoxPack.BOX_STATUS_EXAMED, box.getInspectorId(), new Date());
		String sql = "update box_items set status = :status, updater_id = :updaterId, updated_at = :now where domain_id = :domainId and batch_id = :batchId and box_pack_id = :boxPackId";
		this.queryManager.executeBySql(sql, boxItemParams);
		
		// 3. 작업 정보 검수 완료 처리
		sql = "update job_instances set status = :status, pass_flag = true, updater_id = :updaterId, updated_at = :now where domain_id = :domainId and batch_id = :batchId and box_pack_id = :boxPackId";
		this.queryManager.executeBySql(sql, boxItemParams);
		
		// 4. 박스 유형이 트레이라면 트레이 상태 변경
		TrayBox condition = new TrayBox();
		condition.setTrayCd(box.getBoxId());
		TrayBox tray = this.queryManager.selectByCondition(TrayBox.class, condition);
		
		if(tray != null) {
			tray.setStatus(BoxPack.BOX_STATUS_WAIT);
			this.queryManager.update(tray, "status", "updaterId", "updatedAt");
		}
	}

	@Override
	public BoxPack splitBox(JobBatch batch, BoxPack sourceBox, List<DpsInspItem> inspectionItems, String printerId, Object ... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int printInvoiceLabel(JobBatch batch, BoxPack box, String printerId) {
		String labelTemplate = BatchJobConfigUtil.getInvoiceLabelTemplate(batch);
		PrintEvent printEvent = new PrintEvent(batch.getDomainId(), batch.getJobType(), printerId, labelTemplate, ValueUtil.newMap("box", box));
		this.eventPublisher.publishEvent(printEvent);
		return 1;
	}
	
	@Override
	public int printInvoiceLabel(JobBatch batch, DpsInspection inspection, String printerId) {
		String labelTemplate = BatchJobConfigUtil.getInvoiceLabelTemplate(batch);
		PrintEvent printEvent = new PrintEvent(batch.getDomainId(), batch.getJobType(), printerId, labelTemplate, ValueUtil.newMap("box", inspection));
		this.eventPublisher.publishEvent(printEvent);
		return 1;
	}

	@Override
	public int printTradeStatement(JobBatch batch, BoxPack box, String printerId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void inspectionAction(Long domainId, String boxPackId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void inspectionAction(BoxPack box) {
		// TODO Auto-generated method stub
		
	}

}
