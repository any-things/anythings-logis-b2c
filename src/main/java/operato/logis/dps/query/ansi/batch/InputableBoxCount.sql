SELECT COUNT(DISTINCT(ORDER_NO)) AS INPUTABLE_BOX
  FROM JOB_INSTANCES
 WHERE DOMAIN_ID = :domainId
   AND BATCH_ID = :batchId
   AND EQUIP_TYPE = :equipType
 #if($equipCd)
   AND EQUIP_CD = :equipCd
 #end
   AND ORDER_TYPE = 'MT'
   AND STATUS IN ('BW','W') -- 박스 요청 대기 / 박스 맵핑 대기