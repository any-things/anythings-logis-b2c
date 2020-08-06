
WITH T_OT_JOBS AS (
    SELECT X.*
         , CASE WHEN STATUS = 'W' THEN 1 ELSE 0 END AS W_STATUS_CNT 
         , CASE WHEN STATUS = 'P' THEN 1 ELSE 0 END AS P_STATUS_CNT
         , 1 AS TOT_CNT
      FROM JOB_INSTANCES X
     WHERE X.DOMAIN_ID = :domainId
       AND X.BATCH_ID = :batchId
       AND X.ORDER_TYPE = 'OT'
       AND X.SKU_CD = :skuCd
),
T_TOT_BOX_SUMMARY AS (
    SELECT A.TOT_CNT
         , A.TOT_CNT - A.W_CNT AS COMP_CNT
      FROM (
            SELECT SUM(TOT_CNT) AS TOT_CNT
                 , SUM(W_STATUS_CNT) + SUM(P_STATUS_CNT) AS W_CNT
              FROM T_OT_JOBS
           ) A
),
T_SKU_ORDER_INFOM AS (
    SELECT B.PICK_QTY
    	 , B.BOX_TYPE_CD
         , B.TOT_ORDER_CNT
         , B.TOT_ORDER_CNT - B.W_CNT AS COMP_ORDER_CNT
		#if($jobBoxType)
         , CASE WHEN B.PICK_QTY = :jobPcs AND B.BOX_TYPE_CD = :jobBoxType THEN 1
                ELSE 0
            END AS SELECTED_JOB
        #else
         , 0 AS SELECTED_JOB
        #end
      FROM (
            SELECT PICK_QTY
                 , BOX_TYPE_CD
                 , SUM(TOT_CNT) AS TOT_ORDER_CNT
                 , SUM(W_STATUS_CNT) + SUM(P_STATUS_CNT) AS W_CNT
              FROM T_OT_JOBS
             WHERE SKU_CD = :skuCd
             GROUP BY PICK_QTY, BOX_TYPE_CD
           ) B
     ORDER BY B.PICK_QTY, B.BOX_TYPE_CD 
)
SELECT X.*, Y.*
  FROM T_TOT_BOX_SUMMARY X
     , T_SKU_ORDER_INFOM Y
 ORDER BY Y.SELECTED_JOB DESC, Y.PICK_QTY, Y.BOX_TYPE_CD -- 현재 작업 중인 타입이 최상위  