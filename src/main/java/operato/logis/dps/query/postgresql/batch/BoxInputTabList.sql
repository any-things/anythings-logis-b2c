WITH T_INPUTS AS (
    SELECT *
      FROM JOB_INPUTS
     WHERE DOMAIN_ID = :domainId
       AND BATCH_ID = :batchId
       AND EQUIP_TYPE = :equipType
),
T_STATION_INPUTS AS (
    SELECT *
      FROM T_INPUTS
     WHERE EQUIP_CD = :equipCd
       AND STATION_CD = :equipZone
),
T_R_INPUTS AS (
    SELECT DOMAIN_ID, BATCH_ID , BOX_ID, ORDER_NO, INPUT_SEQ
      FROM (
            SELECT *
              FROM T_STATION_INPUTS
             WHERE STATUS = 'R'
             ORDER BY INPUT_SEQ DESC
           )
     WHERE ROWNUM = 1
),
T_E_INPUTS AS (
    SELECT DOMAIN_ID, BATCH_ID , BOX_ID, ORDER_NO, INPUT_SEQ
      FROM (
            SELECT *
              FROM T_STATION_INPUTS
             WHERE STATUS in ( 'F', 'E')
             ORDER BY INPUT_SEQ DESC
           ) 
     WHERE ROWNUM = 1
),
T_W_INPUTS AS (
    SELECT DOMAIN_ID, BATCH_ID , BOX_ID, ORDER_NO, INPUT_SEQ
      FROM (
            SELECT *
              FROM T_STATION_INPUTS
             WHERE STATUS = 'W'
             ORDER BY INPUT_SEQ ASC
           ) 
     WHERE ROWNUM = 1
),
T_B_INPUT AS (
    SELECT DOMAIN_ID , BATCH_ID, BOX_ID, ORDER_NO, INPUT_SEQ
      FROM (
            SELECT *
              FROM (
              	#if($selectedInputId)
              		SELECT 1 AS ORDER_SEQ , X.DOMAIN_ID, X.BATCH_ID , X.BOX_ID, X.ORDER_NO, X.INPUT_SEQ
              		  FROM T_STATION_INPUTS X
              		 WHERE ID = :selectedInputId
              		 UNION ALL 
              	#end
                    SELECT 2 as ORDER_SEQ , X.*
                      FROM T_R_INPUTS X
                     UNION ALL
                    SELECT 3 as ORDER_SEQ , X.*
                      FROM T_E_INPUTS X
                      UNION ALL
                    SELECT 4 as ORDER_SEQ , X.*
                      FROM T_W_INPUTS X
                   )
             ORDER BY ORDER_SEQ ASC
           )
     WHERE ROWNUM = 1
),
T_INPUTS_GROUP AS (
    SELECT DOMAIN_ID , BATCH_ID , BOX_ID, ORDER_NO, MIN(INPUT_SEQ) AS INPUT_SEQ
      FROM T_INPUTS
     GROUP BY DOMAIN_ID , BATCH_ID , BOX_ID, ORDER_NO
),
T_INPUTS_FILTER AS (
    SELECT *
      FROM T_INPUTS
    #if($viewOnlyMyJob)
     WHERE (DOMAIN_ID , BATCH_ID , BOX_ID, ORDER_NO ) 
            in (SELECT DOMAIN_ID , BATCH_ID , BOX_ID, ORDER_NO
                  FROM T_STATION_INPUTS )
	#end
),
T_INPUT_SEQ_RES AS (
    SELECT *
      FROM (
            SELECT SEQ, BATCH_ID, ORDER_NO, BOX_ID, COLOR_CD
                 , MAX(DECODE(STATION_CD, :equipZone, ID, '')) AS ID 
                 , MIN(ID) AS U_ID 
                 , SUM(DECODE(STATION_CD, :equipZone, 1, 0))  AS HAS_MY_JOBS
                 , MAX(DECODE(STATION_CD, :equipZone, INPUT_SEQ, 0)) AS INPUT_SEQ 
                 , DECODE(SUM(CASE WHEN STATION_CD = :equipZone AND STATUS in ('W', 'R') THEN 10
                                   WHEN STATUS in ( 'F', 'U', 'E' ) THEN 0
                                   ELSE 100 
                               END ), 10 , 1, 0 ) AS IS_MY_ZONE_IS_LAST
                 , 0 AS MY_ZONE_PROGRESS_RATE
                 , MAX(INPUT_SEQ) AS ORDER_SEQ
                 , MAX(DECODE(STATION_CD, :equipZone, STATUS, NULL)) AS STATUS 
              FROM (
                    SELECT 1 as SEQ, X.*
                      FROM T_INPUTS_FILTER X
                     WHERE (DOMAIN_ID , BATCH_ID , BOX_ID, ORDER_NO ) 
                            in (
                                SELECT DOMAIN_ID, BATCH_ID, BOX_ID, ORDER_NO 
                                  FROM (
                                        SELECT *
                                          FROM T_INPUTS_GROUP
                                         WHERE INPUT_SEQ < (SELECT INPUT_SEQ FROM T_B_INPUT)
                                           AND (DOMAIN_ID, BATCH_ID, BOX_ID, ORDER_NO )
                                                not in ( SELECT DOMAIN_ID, BATCH_ID, BOX_ID, ORDER_NO
                                                           FROM T_B_INPUT)
                                         ORDER BY INPUT_SEQ DESC
                                       )
                                 WHERE ROWNUM = 1
                               )
                     UNION ALL
                    SELECT 2 as SEQ, X.*
                      FROM T_INPUTS_FILTER X
                     WHERE (DOMAIN_ID , BATCH_ID , BOX_ID, ORDER_NO ) 
                            = ( SELECT DOMAIN_ID , BATCH_ID , BOX_ID, ORDER_NO
                                  FROM T_B_INPUT )
                     UNION ALL
                    SELECT 3 as SEQ, X.*
                      FROM T_INPUTS_FILTER X
                     WHERE (DOMAIN_ID , BATCH_ID , BOX_ID, ORDER_NO ) 
                            in (
                                SELECT DOMAIN_ID, BATCH_ID, BOX_ID, ORDER_NO 
                                  FROM (
                                        SELECT *
                                          FROM T_INPUTS_GROUP
                                         WHERE INPUT_SEQ > (SELECT INPUT_SEQ FROM T_B_INPUT)
                                           AND (DOMAIN_ID, BATCH_ID, BOX_ID, ORDER_NO )
                                                not in ( SELECT DOMAIN_ID, BATCH_ID, BOX_ID, ORDER_NO
                                                           FROM T_B_INPUT)
                                         ORDER BY INPUT_SEQ ASC
                                       )
                                 WHERE ROWNUM <= 2
                               )
                   )
             GROUP BY SEQ, BATCH_ID, ORDER_NO, BOX_ID, COLOR_CD
           )
)
SELECT X.BATCH_ID, X.ORDER_NO, X.BOX_ID, X.COLOR_CD
     , NVL(X.ID, X.U_ID) AS ID
     , X.HAS_MY_JOBS, X.IS_MY_ZONE_IS_LAST
     , DECODE(X.INPUT_SEQ, 0, X.ORDER_SEQ, X.INPUT_SEQ) AS INPUT_SEQ
     , DECODE(NVL(PICKED_QTY,0), 0, 0, PICK_QTY/PICKED_QTY) * 100 MY_ZONE_PROGRESS_RATE
     , X.STATUS
#if($selectedInputId)
	 , DECODE(X.ID, :selectedInputId, 1, 0 ) AS IS_SELECTED_ITEM
#end     
  FROM T_INPUT_SEQ_RES X
     , (SELECT DOMAIN_ID, BATCH_ID, ORDER_NO, SUM(PICK_QTY) AS PICK_QTY, SUM(PICKED_QTY) AS PICKED_QTY
          FROM JOB_INSTANCES
         WHERE (DOMAIN_ID, BATCH_ID, ORDER_NO)
                 in (SELECT DOMAIN_ID, BATCH_ID, ORDER_NO FROM T_INPUT_SEQ_RES)
           AND (EQUIP_TYPE, EQUIP_CD, SUB_EQUIP_CD) 
                 in (SELECT EQUIP_TYPE, EQUIP_CD, CELL_CD
                       FROM CELLS Y
                      WHERE Y.DOMAIN_ID = :domainId
                        AND Y.EQUIP_TYPE = :equipType
                        AND Y.EQUIP_CD = :equipCd
                        AND Y.STATION_CD = :equipZone
                    )
         GROUP BY DOMAIN_ID, BATCH_ID, ORDER_NO
       ) Y
 WHERE X.BATCH_ID = Y.BATCH_ID(+)
   AND X.ORDER_NO = Y.ORDER_NO(+)
 ORDER BY X.SEQ, X.ORDER_SEQ
