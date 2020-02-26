SELECT BOX_TYPE_CD
     , INPUT_PLAN_QTY
     , INPUT_BOX_QTY
     , INPUT_PLAN_QTY - INPUT_BOX_QTY AS REMAIN_QTY
  FROM (
        SELECT X.BOX_TYPE_CD
             , SUM(X.INPUT_PLAN_QTY) AS INPUT_PLAN_QTY
             , NVL(SUM(Y.INPUT_BOX_QTY),0) AS INPUT_BOX_QTY
          FROM (
                SELECT BOX_TYPE_CD, COUNT(DISTINCT(ORDER_NO)) AS INPUT_PLAN_QTY
                  FROM ORDERS
				 WHERE DOMAIN_ID = :domainId
				   AND BATCH_ID = :batchId
				   AND EQUIP_TYPE = :equipType
				 #if($equipCd)
				   AND EQUIP_CD = :equipCd
				 #end
				   AND ORDER_TYPE = 'MT'
				 GROUP BY BOX_TYPE_CD
               ) X
             , (
                SELECT BOX_TYPE_CD, COUNT(DISTINCT(ORDER_NO)) AS INPUT_BOX_QTY
                  FROM JOB_INSTANCES
				 WHERE DOMAIN_ID = :domainId
				   AND BATCH_ID = :batchId
				   AND EQUIP_TYPE = :equipType
				 #if($equipCd)
				   AND EQUIP_CD = :equipCd
				 #end
				   AND ORDER_TYPE = 'MT'
				   AND STATUS NOT IN ('BW','W') -- 박스 요청 대기 / 박스 맵핑 대기 
                 GROUP BY BOX_TYPE_CD 
               ) Y
         WHERE X.BOX_TYPE_CD = Y.BOX_TYPE_CD(+)
         GROUP BY X.BOX_TYPE_CD
       )
 ORDER BY BOX_TYPE_CD