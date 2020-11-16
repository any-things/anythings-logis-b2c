insert into job_instances(
	id
	, domain_id
	, batch_id
	, job_type
	, job_date
	, job_seq
	, stage_cd
	, equip_type
	, equip_cd
	, sub_equip_cd
	, ind_cd
	, status
	, com_cd
	, order_no
	, class_cd
	, box_class_cd
	, shop_cd
	, shop_nm
	, sku_cd
	, sku_nm
	, box_type_cd
	, box_in_qty
	, box_id
	, invoice_id
	, input_seq
	, color_cd
	, order_type
	, pick_qty
	, picking_qty
	, picked_qty
	, created_at
	, updated_at
	, creator_id
	, updater_id)
select 
	uuid_generate_v4()
	, :domainId
	, :batchId
	, max(job_type)
	, max(job_date)
	, max(job_seq)
	, max(stage_cd)
	, :equipType
	, :equipCd
	, :cellCd
	, :indCd
	, 'A'
	, max(com_cd)
	, max(order_no)
	, max(class_cd)
	, max(box_class_cd)
	, max(shop_cd)
	, max(shop_nm)
	, max(sku_cd)
	, max(sku_nm)
	, max(box_type_cd)
	, max(box_in_qty)
	, max(box_id)
	, max(invoice_id)
	, 0
	, :colorCd
	, max(order_type)
	, :assignQty
	, 0
	, 0
	, now()
	, now()
	, 'system'
	, 'system'
from
	ORDERS
WHERE
	DOMAIN_ID = :domainId
	AND BATCH_ID = :batchId 
	AND CLASS_CD = :orderNo 
	AND SKU_CD = :skuCd
GROUP BY 
	BATCH_ID, CLASS_CD, SKU_CD