alter table options
    add constraint uk_options_product_id_name unique (product_id, name);
