package xyz.anythings.dps.service;

import org.springframework.stereotype.Component;

import xyz.anythings.base.event.IClassifyInEvent;
import xyz.anythings.base.event.IClassifyRunEvent;
import xyz.anythings.base.service.api.IPickingService;


/**
 * DPS 박스 처리 포함한 피킹 서비스 트랜잭션 구현 
 * @author yang
 *
 */
@Component("dpsPickingService")
public class DpsPickingService extends DpsClassificationService implements IPickingService{

	/**
	 * 2-2. 투입 : 배치 작업에 공 박스 투입
	 * 
	 * @param inputEvent
	 * @return
	 */
	@Override
	public Object inputEmptyBox(IClassifyInEvent inputEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 2-3. 투입 : 배치 작업에 공 트레이 투입
	 * 
	 * @param inputEvent
	 * @return
	 */
	@Override
	public Object inputEmptyTray(IClassifyInEvent inputEvent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 3-1. 소분류 : 피킹 작업 확정 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	public void confirmPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 3-2. 소분류 : 피킹 취소 (예정 수량보다 분류 처리할 실물이 작아서 처리할 수 없는 경우 취소 처리)
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 */
	@Override
	public void cancelPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 3-3. 소분류 : 수량을 조정하여 분할 피킹 처리
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	public int splitPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * 3-4. 소분류 : 피킹 확정 처리된 작업 취소
	 * 
	 * @param exeEvent 분류 작업 이벤트
	 * @return
	 */
	@Override
	public int undoPick(IClassifyRunEvent exeEvent) {
		// TODO Auto-generated method stub
		return 0;
	}

}
