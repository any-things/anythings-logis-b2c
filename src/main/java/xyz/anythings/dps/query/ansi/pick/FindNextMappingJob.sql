#if($mapColumn == 'order_no') -- 주문 기준 맵핑 
    #if($orderType == 'OT') -- 단수 / 단포 
		SELECT ID
		  FROM (
		        WITH T_ORDER AS (
		            SELECT *
		              FROM JOB_INSTANCES
		             WHERE DOMAIN_ID = :domainId
		               AND BATCH_ID = :batchId
                      #if($boxTypeCd)
                       AND BOX_TYPE_CD = :boxTypeCd
                      #end                
                      #if($orderType)
                       AND ORDER_TYPE = :orderType
                      #end                
		        ),
		        T_BEF_JOB AS (
		            SELECT 1 AS ORDER_SEQ, ID, PICK_QTY - PICKED_QTY AS PICK_QTY
		              FROM T_ORDER
		             WHERE BOX_ID = :bucketCd
		        ),
		        T_READY_JOB AS (
		            SELECT 2 AS ORDER_SEQ, ID, PICK_QTY
		              FROM (
		                    SELECT ID, PICK_QTY
		                      FROM T_ORDER
		                     WHERE INPUT_SEQ = 0
		                       AND STATUS = 'W'
		                     ORDER BY PICK_QTY
		                   )
		             WHERE ROWNUM = 1
		        )
		        SELECT *
		          FROM (
		                SELECT *
		                  FROM (
		                        SELECT ORDER_SEQ, ID, PICK_QTY FROM T_BEF_JOB
		                         UNION ALL
		                        SELECT ORDER_SEQ, ID, PICK_QTY FROM T_READY_JOB
		                       )
		                 ORDER BY ORDER_SEQ, PICK_QTY
		               )
		         WHERE ROWNUM = 1
		       )
    
    #else -- 합포 및 기타 등등 
        SELECT ORDER_NO
          FROM (
                SELECT ORDER_NO
                FROM (
                        SELECT ORDER_NO
                            , COUNT(DISTINCT SKU_CD) AS SKU_CNT
                            , SUM(PICK_QTY) AS PICK_CNT
                        FROM JOB_INSTANCES
                        WHERE DOMAIN_ID = :domainId
                        AND BATCH_ID = :batchId
                        AND INPUT_SEQ = 0
                        AND STATUS = 'W'
                        #if($boxTypeCd)
                        AND BOX_TYPE_CD = :boxTypeCd
                        #end                
                        #if($orderType)
                        AND ORDER_TYPE = :orderType
                        #end                
                        GROUP BY ORDER_NO  
                    )
                ORDER BY SKU_CNT ASC , PICK_CNT ASC
            )
        WHERE ROWNUM = 1 
    #end
#elseif($mapColumn == 'sku_cd') -- 상품 기준 맵핑 
#elseif($mapColumn == 'shop_cd') -- 샵 기준 맵핑 
#end