
WITH T_OT_JOBS AS (
    SELECT X.*
         , DECODE(STATUS, 'W', 1, 0) AS W_STATUS_CNT 
         , DECODE(STATUS, 'P', 1, 0) AS P_STATUS_CNT
         , 1 AS TOT_CNT
      FROM JOB_INSTANCES X
     WHERE X.DOMAIN_ID = :domainId
       AND X.BATCH_ID = :batchId
       AND X.ORDER_TYPE = 'OT'
),
T_TOT_BOX_SUMMARY AS (
    SELECT TOT_CNT
         , TOT_CNT - W_CNT AS COMP_CNT
      FROM (
            SELECT SUM(TOT_CNT) AS TOT_CNT
                 , SUM(W_STATUS_CNT) + SUM(P_STATUS_CNT) AS W_CNT
              FROM T_OT_JOBS
           )
),
T_SKU_ORDER_INFOM AS (
    SELECT PICK_QTY, BOX_TYPE_CD
         , TOT_ORDER_CNT
         , TOT_ORDER_CNT - W_CNT AS COMP_ORDER_CNT
		#if($jobBoxType)
         , CASE WHEN PICK_QTY = :jobPcs AND BOX_TYPE_CD = :jobBoxType THEN 1
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
           )
     ORDER BY PICK_QTY, BOX_TYPE_CD 
)
SELECT X.*, Y.*
  FROM T_TOT_BOX_SUMMARY X
     , T_SKU_ORDER_INFOM Y
 ORDER BY Y.SELECTED_JOB DESC, Y.PICK_QTY, Y.BOX_TYPE_CD -- 현재 작업 중인 타입이 최상위  