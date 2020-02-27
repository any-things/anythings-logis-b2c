SELECT BATCH_ID, INPUT_SEQ, EQUIP_TYPE, EQUIP_CD, ORDER_NO, BOX_ID, BOX_TYPE
     , SKU_QTY, PLAN_QTY, RESULT_QTY
     , DECODE(PLAN_QTY , RESULT_QTY , '1', '0') AS STATUS
     , COLOR_CD
  FROM (
	     SELECT BATCH_ID, MIN(INPUT_SEQ) AS INPUT_SEQ, EQUIP_TYPE, EQUIP_CD, ORDER_NO, BOX_ID, BOX_TYPE_CD AS BOX_TYPE
	          , COUNT(1) AS SKU_QTY
	          , SUM(PICK_QTY) AS PLAN_QTY 
	          , NVL(SUM(PICKED_QTY),0) AS RESULT_QTY
	          , MAX(COLOR_CD) AS COLOR_CD
	      FROM (
	            SELECT BATCH_ID, ORDER_NO, MIN(INPUT_SEQ) AS INPUT_SEQ, EQUIP_TYPE, EQUIP_CD, BOX_ID, BOX_TYPE_CD
	                 , COM_CD, SKU_CD
	                 , SUM(PICK_QTY) AS PICK_QTY 
	                 , NVL(SUM(PICKED_QTY),0) AS PICKED_QTY
	                 , MAX(COLOR_CD) AS COLOR_CD
	              FROM JOB_INSTANCES
	             WHERE DOMAIN_ID = :domainId
	               AND BATCH_ID = :batchId
	               AND EQUIP_TYPE = :equipType
	             #if($equipCd)
	               AND EQUIP_CD = :equipCd
	             #end
	               AND STATUS  not in ('BW','W')
	               AND ORDER_TYPE = 'MT'
	             GROUP BY BATCH_ID, EQUIP_TYPE, EQUIP_CD, ORDER_NO, BOX_ID, BOX_TYPE_CD, COM_CD, SKU_CD
	           )
	     GROUP BY BATCH_ID, EQUIP_TYPE, EQUIP_CD, ORDER_NO, BOX_ID, BOX_TYPE_CD  
       )
 ORDER BY INPUT_SEQ DESC