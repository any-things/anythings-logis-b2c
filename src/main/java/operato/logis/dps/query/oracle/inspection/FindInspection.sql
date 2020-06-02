SELECT
	ORDER_NO,
	INVOICE_ID,
	BOX_ID,
	STATUS,
	COUNT(DISTINCT(SKU_CD)) AS SKU_QTY,
	NVL(SUM(PICKED_QTY), 0) AS PICKED_QTY
FROM
	JOB_INSTANCES
WHERE
	DOMAIN_ID = :domainId
	#if($batchId)
	AND BATCH_ID = :batchId
	#end
	#if($orderNo)
	AND ORDER_NO = :orderNo
	#end
	#if($invoiceId)
	AND INVOICE_ID = :invoiceId
	#end
	#if($boxId)
	AND BOX_ID = :boxId
	#end
	AND STATUS = :status
GROUP BY
	ORDER_NO, INVOICE_ID, BOX_ID, STATUS