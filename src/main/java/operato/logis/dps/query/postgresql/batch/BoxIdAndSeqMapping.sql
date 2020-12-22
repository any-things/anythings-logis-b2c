UPDATE JOB_INSTANCES
	SET INPUT_SEQ = :inputSeq
		, BOX_ID = :boxId
		, COLOR_CD = :colorCd
		, STATUS = CASE WHEN (ORDER_TYPE = 'OT') THEN 'P' ELSE 'I' END -- 단포 작업은 Picking, 합포 작업은 Input 
		, INPUT_AT = :inputAt
		, UPDATER_ID = :userId
 WHERE
	DOMAIN_ID = :domainId
	AND BATCH_ID = :batchId
	AND EQUIP_TYPE = :equipType
	AND CLASS_CD = :classCd
	AND (ORDER_TYPE = 'OT' OR EXISTS (SELECT
											1
										FROM
											CELLS Y
										WHERE
											Y.DOMAIN_ID = JOB_INSTANCES.DOMAIN_ID
											AND Y.EQUIP_TYPE = JOB_INSTANCES.EQUIP_TYPE
											AND Y.EQUIP_CD = JOB_INSTANCES.EQUIP_CD
											AND Y.CELL_CD = JOB_INSTANCES.SUB_EQUIP_CD
											#if($stationCd)
											AND Y.STATION_CD = :stationCd
											#end)
	)