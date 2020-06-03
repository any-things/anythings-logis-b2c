SELECT
	J.SKU_CD,
	J.SKU_NM,
	S.SKU_BARCD,
	COALESCE(SUM(J.PICKED_QTY), 0) AS PICKED_QTY
FROM
	JOB_INSTANCES J INNER JOIN SKU S 
	ON J.COM_CD = S.COM_CD AND J.SKU_CD = S.SKU_CD
WHERE
	J.DOMAIN_ID = :domainId
	#if($batchId)
	AND J.BATCH_ID = :batchId
	#end
	#if($orderNo)
	AND J.ORDER_NO = :orderNo
	#end
	#if($invoiceId)
	AND J.INVOICE_ID = :invoiceId
	#end
	#if($boxId)
	AND J.BOX_ID = :boxId
	#end
	#if($status)
	AND J.STATUS = :status
	#end
GROUP BY
	J.SKU_CD, J.SKU_NM, S.SKU_BARCD