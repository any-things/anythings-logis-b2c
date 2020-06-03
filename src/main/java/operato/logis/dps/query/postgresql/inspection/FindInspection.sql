SELECT
	BATCH_ID,
	ORDER_NO,
	INVOICE_ID,
	BOX_ID,
	STATUS,
	COUNT(DISTINCT(SKU_CD)) AS SKU_QTY,
	COALESCE(SUM(PICKED_QTY), 0) AS PICKED_QTY
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
	#if($status)
	AND STATUS = :status
	#end
GROUP BY
	BATCH_ID, ORDER_NO, INVOICE_ID, BOX_ID, STATUS