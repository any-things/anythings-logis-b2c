#if($orderType == 'OT') -- 단수 / 단포 
SELECT 
	OT.ID
FROM (
	WITH T_ORDER AS (
		SELECT *
			FROM JOB_INSTANCES
		WHERE 
			DOMAIN_ID = :domainId
			AND BATCH_ID = :batchId
			#if($boxTypeCd)
			AND BOX_TYPE_CD = :boxTypeCd
			#end
			#if($orderType)
			AND ORDER_TYPE = :orderType
			#end
	),
	T_BEF_JOB AS (
		SELECT 
			1 AS ORDER_SEQ, ID, PICK_QTY - PICKED_QTY AS PICK_QTY
		FROM
			T_ORDER
		WHERE
			BOX_ID = :bucketCd
	),
	T_READY_JOB AS (
		SELECT 
			2 AS ORDER_SEQ, A.ID, A.PICK_QTY
		FROM (
			SELECT 
				ID, PICK_QTY
			FROM 
				T_ORDER
			WHERE
				INPUT_SEQ = 0
				AND STATUS = 'W'
				ORDER BY PICK_QTY
		) A
		LIMIT 1
	)
	SELECT 
		B.*
	FROM (
		SELECT 
			*
		FROM (
			SELECT 
				ORDER_SEQ, ID, PICK_QTY 
			FROM 
				T_BEF_JOB
				
			UNION ALL

			SELECT
				ORDER_SEQ, ID, PICK_QTY FROM T_READY_JOB
		) B
		ORDER BY
			B.ORDER_SEQ, B.PICK_QTY
	)
	LIMIT 1
) OT

#else -- 합포 및 기타 등등 
SELECT
	MT.ORDER_NO
FROM (
	SELECT
		A.ORDER_NO
	FROM (
		SELECT 
			ORDER_NO, 
			COUNT(DISTINCT SKU_CD) AS SKU_CNT,
			SUM(PICK_QTY) AS PICK_CNT
		FROM
			JOB_INSTANCES
		WHERE
			DOMAIN_ID = :domainId
			AND BATCH_ID = :batchId
			AND INPUT_SEQ = 0
			AND STATUS = 'W'
			#if($boxTypeCd)
			AND BOX_TYPE_CD = :boxTypeCd
			#end
			#if($orderType)
			AND ORDER_TYPE = :orderType
			#end
		GROUP BY
			ORDER_NO
	) A
	ORDER BY 
		A.SKU_CNT ASC, A.PICK_CNT ASC
) MT
	LIMIT 1
#end