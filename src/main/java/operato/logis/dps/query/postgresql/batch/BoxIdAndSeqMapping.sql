UPDATE JOB_INSTANCES
	SET INPUT_SEQ = :inputSeq
		, BOX_ID = :boxId
		, COLOR_CD = :colorCd
		, STATUS = 'I' 
		, INPUT_AT = :inputAt
		, UPDATER_ID = :userId
 WHERE
	DOMAIN_ID = :domainId
	AND BATCH_ID = :batchId
	AND EQUIP_TYPE = :equipType
	AND CLASS_CD = :classCd