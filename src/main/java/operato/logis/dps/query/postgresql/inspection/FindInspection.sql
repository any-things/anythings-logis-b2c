SELECT
	*
FROM
	DPS_BOX_PAKCS
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