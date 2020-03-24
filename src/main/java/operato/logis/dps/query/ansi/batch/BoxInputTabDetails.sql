WITH T_INSTANCES AS (
    SELECT X.*, Y.SIDE_CD
      FROM (
            SELECT *
              FROM JOB_INSTANCES
             WHERE DOMAIN_ID = :domainId
               AND BATCH_ID = :batchId
               AND EQUIP_TYPE = :equipType
               AND EQUIP_CD = :equipCd
               AND ORDER_NO = :orderNo
           ) X
         , (SELECT *
              FROM CELLS
             WHERE DOMAIN_ID = :domainId
               AND EQUIP_TYPE = :equipType
               AND EQUIP_CD = :equipCd
               AND STATION_CD = :stationCd) Y
     WHERE X.DOMAIN_ID = Y.DOMAIN_ID
       AND X.EQUIP_TYPE = Y.EQUIP_TYPE
       AND X.EQUIP_CD = Y.EQUIP_CD
       AND X.SUB_EQUIP_CD = Y.CELL_CD
),
T_GWS AS (
     SELECT X.IND_CD, Y.GW_CD, Y.GW_NM
       FROM (
            SELECT *
              FROM INDICATORS
             WHERE (DOMAIN_ID, IND_CD) in (SELECT DOMAIN_ID, IND_CD FROM T_INSTANCES)
            ) X
          , (
             SELECT DOMAIN_ID, GW_CD , GW_NM
               FROM GATEWAYS
              WHERE DOMAIN_ID = :domainId
                AND STAGE_CD = :stageCd
            ) Y
      WHERE X.DOMAIN_ID = Y.DOMAIN_ID
        AND X.GW_CD = Y.GW_CD
)
SELECT X.*, Y.GW_CD, Y.GW_NM AS GW_PATH
  FROM T_INSTANCES X
     , T_GWS Y
 WHERE X.IND_CD = Y.IND_CD