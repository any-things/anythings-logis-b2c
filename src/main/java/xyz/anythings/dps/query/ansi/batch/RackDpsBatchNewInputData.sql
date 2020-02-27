#if($orderType == 'MT') -- 합포 

	SELECT DISTINCT X.BATCH_ID, X.EQUIP_TYPE, X.EQUIP_CD
	     , Y.STATION_CD, X.ORDER_NO
	     , X.COM_CD
	     , X.DOMAIN_ID
	  FROM (
	        SELECT BATCH_ID, EQUIP_TYPE, EQUIP_CD
	             , SUB_EQUIP_CD AS CELL_CD
	             , ORDER_NO
	             , COM_CD
	             , DOMAIN_ID
	          FROM JOB_INSTANCES
	         WHERE DOMAIN_ID = :domainId
	           AND BATCH_ID = :batchId
	           AND EQUIP_TYPE = :equipType
	           AND ORDER_NO = :orderNo
	       ) X
	     , (SELECT DOMAIN_ID, EQUIP_TYPE, EQUIP_CD
	             , STATION_CD, CELL_CD
	             , CELL_SEQ
	          FROM CELLS
	         WHERE DOMAIN_ID = :domainId
	           AND EQUIP_TYPE = :equipType
	           AND ACTIVE_FLAG = 1
	       ) Y
	 WHERE X.DOMAIN_ID = Y.DOMAIN_ID
	   AND X.EQUIP_TYPE = Y.EQUIP_TYPE
	   AND X.EQUIP_CD = Y.EQUIP_CD
	   AND X.CELL_CD = Y.CELL_CD
	 ORDER BY X.EQUIP_CD, Y.STATION_CD
	 
#elseif($orderType == 'OT') -- 단포   

    SELECT BATCH_ID, EQUIP_TYPE, EQUIP_CD
         , '' AS STATION_CD
         , ORDER_NO
         , COM_CD
         , DOMAIN_ID
      FROM JOB_INSTANCES
     WHERE DOMAIN_ID = :domainId
       AND BATCH_ID = :batchId
       AND EQUIP_TYPE = :equipType
       AND ORDER_NO = :orderNo
       
#end