SELECT
	*
FROM
	DPS_BOX_ITEMS
WHERE
	DPS_BOX_PACK_ID = (:batchId || '-' || :orderNo)