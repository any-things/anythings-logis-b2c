/* Copyright © HatioLab Inc. All rights reserved. */
package operato.logis.dps.web.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import operato.logis.dps.config.ModuleProperties;
import operato.logis.dps.query.store.DpsAssignQueryStore;
import operato.logis.dps.query.store.DpsBatchQueryStore;
import operato.logis.dps.query.store.DpsBoxQueryStore;
import operato.logis.dps.query.store.DpsInspectionQueryStore;
import operato.logis.dps.query.store.DpsPickQueryStore;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;

/**
 * Operato Logis DPS Startup시 Framework 초기화 클래스 
 * 
 * @author yang
 */
@Component
public class OperatoLogisDpsInitializer {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(OperatoLogisDpsInitializer.class);
	
	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	@Autowired
	private IEntityFieldCache entityFieldCache;
	
	@Autowired
	private ModuleProperties module;
	
	@Autowired
	private ModuleConfigSet configSet;
	
	@Autowired
	private IQueryManager queryManager;

	/**
	 * DPS 배치용 쿼리 스토어
	 */	
	@Autowired
	private DpsBatchQueryStore dpsBatchQueryStore;
	/**
	 * DPS 할당용 쿼리 스토어
	 */	
	@Autowired
	private DpsAssignQueryStore dpsAssignQueryStore;
	/**
	 * DPS 피킹용 쿼리 스토어
	 */
	@Autowired
	private DpsPickQueryStore dpsPickQueryStore;
	/**
	 * DPS 박스 처리용 쿼리 스토어
	 */	
	@Autowired
	private DpsBoxQueryStore dpsBoxQueryStore;
	/**
	 * DPS 출고 검수 처리용 쿼리 스토어
	 */	
	@Autowired
	private DpsInspectionQueryStore dpsInspectionQueryStore;
	
	@EventListener({ ContextRefreshedEvent.class })
	public void refresh(ContextRefreshedEvent event) {
		this.logger.info("Operato Logistics DPS module refreshing...");
		
		this.logger.info("Operato Logistics DPS module refreshed!");
	}
	
	@EventListener({ApplicationReadyEvent.class})
    void ready(ApplicationReadyEvent event) {
		this.logger.info("Operato Logistics DPS module initializing...");
		
		this.configSet.addConfig(this.module.getName(), this.module);
		this.scanServices();
		this.initQueryStores();
		this.logger.info("Operato Logistics DPS module initialized!");
    }
	
	/**
	 * 모듈 서비스 스캔 
	 */
	private void scanServices() {
		this.entityFieldCache.scanEntityFieldsByBasePackage(this.module.getBasePackage());
		this.restFinder.scanServicesByPackage(this.module.getName(), this.module.getBasePackage());
	}
	
	/**
	 * 쿼리 스토어 초기화
	 */
	private void initQueryStores() {
		String dbType = this.queryManager.getDbType();
		this.dpsBatchQueryStore.initQueryStore(dbType);
		this.dpsAssignQueryStore.initQueryStore(dbType);
		this.dpsPickQueryStore.initQueryStore(dbType);
		this.dpsBoxQueryStore.initQueryStore(dbType);
		this.dpsInspectionQueryStore.initQueryStore(dbType);
	}

}