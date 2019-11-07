/* Copyright © HatioLab Inc. All rights reserved. */
package xyz.anythings.dps.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.FormatUtil;

/**
 * anythings-logis-dps 모듈 정보 파일
 * 
 * @author yang
 */
@Component("anythingsLogisDPSModuleProperties")
@EnableConfigurationProperties
@PropertySource("classpath:/properties/anythings-logis-dps.properties")
public class ModuleProperties implements IModuleProperties {
	
	/**
	 * 모듈명
	 */
	@Value("${anythings.logis.dps.name}")
	private String name;
	
	/**
	 * 버전
	 */
	@Value("${anythings.logis.dps.version}")
	private String version;
	
	/**
	 * Module Built Time 
	 */
	@Value("${anythings.logis.dps.built.at}")
	private String builtAt;	
	
	/**
	 * 모듈 설명
	 */
	@Value("${anythings.logis.dps.description}")
	private String description;
	
	/**
	 * 부모 모듈
	 */
	@Value("${anythings.logis.dps.parentModule}")
	private String parentModule;
	
	/**
	 * 모듈 Base Package
	 */
	@Value("${anythings.logis.dps.basePackage}")
	private String basePackage;
	
	/**
	 * Scan Service Path
	 */
	@Value("${anythings.logis.dps.scanServicePackage}")
	private String scanServicePackage;
	
	/**
	 * Scan Entity Path
	 */
	@Value("${anythings.logis.dps.scanEntityPackage}")
	private String scanEntityPackage;
	
	/**
	 * Project Name
	 * @return
	 */
	@Value("${anythings.logis.dps.projectName}")
	private String projectName;
	
	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}
	
	public String getBuiltAt() {
		return builtAt;
	}

	public String getDescription() {
		return this.description;
	}
	
	public String getParentModule() {
		return this.parentModule;
	}

	public String getBasePackage() {
		return this.basePackage;
	}

	public String getScanServicePackage() {
		return this.scanServicePackage;
	}

	public String getScanEntityPackage() {
		return this.scanEntityPackage;
	}
	
	public String getProjectName() {
		return this.projectName;
	}

	@Override
	public String toString() {
		return FormatUtil.toJsonString(this);
	}
}