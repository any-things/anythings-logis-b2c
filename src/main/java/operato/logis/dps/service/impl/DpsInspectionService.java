package operato.logis.dps.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import operato.logis.dps.entity.DpsBoxItem;
import operato.logis.dps.entity.DpsBoxPack;
import operato.logis.dps.model.DpsInspItem;
import operato.logis.dps.model.DpsInspection;
import operato.logis.dps.service.api.IDpsInspectionService;
import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.TrayBox;
import xyz.anythings.base.service.impl.AbstractInstructionService;
import xyz.anythings.base.service.util.BatchJobConfigUtil;
import xyz.anythings.sys.event.model.PrintEvent;
import xyz.anythings.sys.util.AnyEntityUtil;
import xyz.elidom.dbist.dml.Query;
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
	 * 검수 정보 조회
	 * 
	 * @param sql
	 * @param params
	 * @param orderInfo
	 * @param exceptionWhenEmpty
	 * @return
	 */
	private DpsInspection findInspection(boolean reprintMode, Map<String, Object> params, String orderInfo, boolean exceptionWhenEmpty) {
		Query condition = new Query();
		Iterator<String> keyIter = params.keySet().iterator();
		while(keyIter.hasNext()) {
			String key = keyIter.next();
			condition.addFilter(key, params.get(key));
		}
		
		if(reprintMode) {
			condition.addFilter("status", "in", ValueUtil.toList(LogisConstants.JOB_STATUS_EXAMINATED, LogisConstants.JOB_STATUS_FINAL_OUT, LogisConstants.JOB_STATUS_REPORTED));
		}
		DpsBoxPack boxPack = this.queryManager.selectByCondition(DpsBoxPack.class, condition);
		
		if(boxPack == null) {
			if(exceptionWhenEmpty) {
				throw ThrowUtil.newNotFoundRecord("terms.label.inspection", ValueUtil.toString(orderInfo));
			} else {
				return null;
			}
		} else {
			return ValueUtil.populate(boxPack, new DpsInspection());
		}
	}
	
	/**
	 * 검수 리스트 조회
	 * 
	 * @param sql
	 * @param params
	 * @param orderInfo
	 * @param exceptionWhenEmpty
	 * @return
	 */
	private List<DpsInspection> searchInspectionList(boolean reprintMode, Map<String, Object> params, String orderInfo, boolean exceptionWhenEmpty) {
		Query condition = new Query();
		Iterator<String> keyIter = params.keySet().iterator();
		while(keyIter.hasNext()) {
			String key = keyIter.next();
			condition.addFilter(key, params.get(key));
		}
		
		if(reprintMode) {
			condition.addFilter("status", "in", ValueUtil.toList(LogisConstants.JOB_STATUS_EXAMINATED, LogisConstants.JOB_STATUS_FINAL_OUT, LogisConstants.JOB_STATUS_REPORTED));
		}
		List<DpsBoxPack> boxPackList = this.queryManager.selectList(DpsBoxPack.class, condition);
		
		if(ValueUtil.isEmpty(boxPackList)) {
			if(exceptionWhenEmpty) {
				throw ThrowUtil.newNotFoundRecord("terms.label.inspection", ValueUtil.toString(orderInfo));
			} else {
				return null;
			}
		} else {
			List<DpsInspection> inspectionList = new ArrayList<DpsInspection>();
			for(DpsBoxPack boxPack : boxPackList) {
				inspectionList.add(ValueUtil.populate(boxPack, new DpsInspection()));
			}
			
			return inspectionList;
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
		
		//if(!params.containsKey("invoiceId")) {
		//	params.put("invoiceId", inspection.getInvoiceId());
		//}
		//String sql = this.dpsInspectionQueryStore.getSearchInspectionItemsQuery();
		//List<DpsInspItem> items = this.queryManager.selectListBySql(sql, params, DpsInspItem.class, 0, 0);
		
		Map<String, Object> condition = ValueUtil.newMap("dpsBoxPackId", inspection.getId());
		List<DpsBoxItem> boxItems = this.queryManager.selectList(DpsBoxItem.class, condition);
		
		if(ValueUtil.isNotEmpty(boxItems)) {
			List<DpsInspItem> items = new ArrayList<DpsInspItem>(boxItems.size());
			
			for(DpsBoxItem item : boxItems) {
				items.add(ValueUtil.populate(item, new DpsInspItem()));
			}
			
			inspection.setItems(items);
		}
		
		return inspection;
	}
	
	@Override
	public DpsInspection findInspectionByInput(JobBatch batch, String inputType, String inputId, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		//String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId", batch.getDomainId(), batch.getId());
		
		if(ValueUtil.isEqualIgnoreCase(inputType, "box") || ValueUtil.isEqualIgnoreCase(inputType, "tray")) {
			params.put("boxId", inputId);
			
		} else if(ValueUtil.isEqualIgnoreCase(inputType, "orderNo")) {
			params.put("orderNo", inputId);
			
		} else if(ValueUtil.isEqualIgnoreCase(inputType, "invoiceId")) {
			params.put("invoiceId", inputId);
		}
		
		DpsInspection inspection = this.findInspection(reprintMode, params, inputId, exceptionWhenEmpty);
		if(inspection != null && (ValueUtil.isEqualIgnoreCase(inputType, "box") || ValueUtil.isEqualIgnoreCase(inputType, "tray"))) {
			inspection.setBoxType(inputType);
		}
		
		return this.searchInpsectionItems(inspection, params);
	}

	@Override
	public DpsInspection findInspectionByTray(JobBatch batch, String trayCd, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		//String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,boxId", batch.getDomainId(), batch.getId(), trayCd);
		DpsInspection inspection = this.findInspection(reprintMode, params, trayCd, exceptionWhenEmpty);

		if(inspection != null) {
			inspection.setBoxType("tray");
		}
		
		return this.searchInpsectionItems(inspection, params);
	}

	@Override
	public DpsInspection findInspectionByBox(JobBatch batch, String boxId, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		//String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,boxId", batch.getDomainId(), batch.getId(), boxId);
		DpsInspection inspection = this.findInspection(reprintMode, params, boxId, exceptionWhenEmpty);

		if(inspection != null) {
			inspection.setBoxType("box");
		}
		
		return this.searchInpsectionItems(inspection, params);
	}

	@Override
	public DpsInspection findInspectionByInvoice(JobBatch batch, String invoiceId, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		//String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,invoiceId", batch.getDomainId(), batch.getId(), invoiceId);
		DpsInspection inspection = this.findInspection(reprintMode, params, invoiceId, exceptionWhenEmpty);
		return this.searchInpsectionItems(inspection, params);
	}
	
	@Override
	public DpsInspection findInspectionByOrder(JobBatch batch, String orderNo, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		//String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,orderNo", batch.getDomainId(), batch.getId(), orderNo);
		DpsInspection inspection = this.findInspection(reprintMode, params, orderNo, exceptionWhenEmpty);
		return this.searchInpsectionItems(inspection, params);
	}
	
	@Override
	public List<DpsInspection> searchInspectionList(JobBatch batch, String orderNo, boolean reprintMode, boolean exceptionWhenEmpty) {
		
		//String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId,orderNo", batch.getDomainId(), batch.getId(), orderNo);
		return this.searchInspectionList(reprintMode, params, orderNo, exceptionWhenEmpty);
	}

	@Override
	public DpsInspection findInspectionByBoxPack(DpsBoxPack box, boolean reprintMode) {
		
		//String sql = this.dpsInspectionQueryStore.getFindInspectionQuery();
		Map<String, Object> params = ValueUtil.newMap("domainId,batchId", box.getDomainId(), box.getBatchId());
		
		if(ValueUtil.isNotEmpty(box.getInvoiceId())) {
			params.put("invoiceId", box.getInvoiceId());
			
		} else if(ValueUtil.isNotEmpty(box.getOrderNo())) {
			params.put("orderNo", box.getOrderNo());
			
		} else if(ValueUtil.isNotEmpty(box.getBoxId())) {
			params.put("boxId", box.getBoxId());
		} 
		
		DpsInspection inspection = this.findInspection(reprintMode, params, box.getOrderNo(), true);
		return this.searchInpsectionItems(inspection, params);
	}

	@Override
	public void finishInspection(JobBatch batch, String invoiceId, Float boxWeight, String printerId, Object ... params) {
		
		// 박스 조회
		DpsBoxPack box = AnyEntityUtil.findEntityBy(batch.getDomainId(), false, DpsBoxPack.class, null, "batchId,invoiceId", batch.getId(), invoiceId);
		
		if(box == null) {
			throw ThrowUtil.newNotFoundRecord("terms.menu.DpsBoxPack", invoiceId);
		}
		
		// 검수 완료 처리 
		this.finishInspection(batch, box, boxWeight, printerId);
	}

	@Override
	public void finishInspection(JobBatch batch, DpsBoxPack box, Float boxWeight, String printerId, Object ... params) {
		
		// 1. 박스 상태 업데이트
		box.setStatus(LogisConstants.JOB_STATUS_EXAMINATED);
		box.setManualInspStatus(LogisConstants.PASS_STATUS);
		box.setManualInspectedAt(DateUtil.currentTimeStr());
		
		// 2. 작업 정보 검수 완료 처리
		Map<String, Object> updateParams = ValueUtil.newMap("status,manualInspStatus,nowStr,now,updaterId,domainId,batchId,orderNo",
				LogisConstants.JOB_STATUS_EXAMINATED,
				LogisConstants.PASS_STATUS,
				DateUtil.currentTimeStr(),
				new Date(),
				User.currentUser().getId(),
				batch.getDomainId(),
				batch.getId(),
				box.getOrderNo()
			);
		
		if(boxWeight != null) {
			box.setBoxRealWt(boxWeight);
			updateParams.put("boxRealWt", boxWeight);
		}
		
		String sql = "update job_instances set inspected_qty = pick_qty, status = :status, manual_insp_status = :manualInspStatus, manual_inspected_at = :nowStr, #if($boxRealWt) box_real_wt = :boxRealWt, #end updater_id = :updaterId, updated_at = :now where domain_id = :domainId and batch_id = :batchId and class_cd = :orderNo";
		this.queryManager.executeBySql(sql, updateParams);
		
		// 3. 박스 유형이 트레이라면 트레이 상태 변경
		TrayBox condition = new TrayBox();
		condition.setTrayCd(box.getBoxId());
		TrayBox tray = this.queryManager.selectByCondition(TrayBox.class, condition);
		
		if(tray != null) {
			tray.setStatus(LogisConstants.JOB_STATUS_WAIT);
			this.queryManager.update(tray, "status", "updaterId", "updatedAt");
		}
	}

	@Override
	public DpsBoxPack splitBox(JobBatch batch, DpsBoxPack sourceBox, List<DpsInspItem> inspectionItems, String printerId, Object ... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int printInvoiceLabel(JobBatch batch, DpsBoxPack box, String printerId) {
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
	public int printTradeStatement(JobBatch batch, DpsBoxPack box, String printerId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void inspectionAction(Long domainId, String boxPackId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void inspectionAction(DpsBoxPack box) {
		// TODO Auto-generated method stub
		
	}

}
