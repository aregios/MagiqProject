
#include "php_imcs.h"

#ifdef WIN32
#include "imcs_base.h"
#include "imcs_factory.h"
IMCSFactory imcs_factory;
#else
#include "imcs_engine.h"
#endif

#undef IMCS_DEBUG


zend_object_handlers imcs_object_handlers;


struct imcs_object {
	zend_object zstd;
	IMCSBase *engine;
};


void imcs_free_storage(void *object TSRMLS_DC)
{
    imcs_object *obj = (imcs_object *)object;
    delete obj->engine; 

    zend_hash_destroy(obj->zstd.properties);
    FREE_HASHTABLE(obj->zstd.properties);

    efree(obj);
}


zend_object_value imcs_create_handler(zend_class_entry *class_type TSRMLS_DC)
{
    zend_object_value retval;

    imcs_object *obj = (imcs_object *)emalloc(sizeof(imcs_object));
    memset(obj, 0, sizeof(imcs_object));
    obj->zstd.ce = class_type;

    ALLOC_HASHTABLE(obj->zstd.properties);
    zend_hash_init(obj->zstd.properties, 0, NULL, ZVAL_PTR_DTOR, 0);
	object_properties_init(&(obj->zstd), class_type);

    retval.handle = zend_objects_store_put(obj, NULL, imcs_free_storage, NULL TSRMLS_CC);
    retval.handlers = &imcs_object_handlers;

    return retval;
}


zend_class_entry *imcs_ce;	


ZEND_METHOD(IMCSEngine, __construct) {
	imcs_object *obj = (imcs_object *)zend_object_store_get_object(getThis() TSRMLS_CC);
	if (obj!= NULL) {
		#ifdef WIN32
		obj->engine = imcs_factory.getIMCSEngine(); 
		#else
		obj->engine = new IMCSEngine();
		#endif
	}
}



ZEND_BEGIN_ARG_INFO_EX(arginfo_pepare, 0, 0, 1)	//(function name, pass_rest_by_reference, return_reference, required_num_args)
	ZEND_ARG_INFO(0, image_file1)
	ZEND_ARG_INFO(0, compare_method)
	ZEND_ARG_INFO(0, reduce_factor)
ZEND_END_ARG_INFO()


ZEND_METHOD(IMCSEngine, prepare) {

	char* image_file1;
	int image_file1_len = 0;
	int compare_method = 0;
	int reduce_factor = 0;

	if (zend_parse_parameters(ZEND_NUM_ARGS() TSRMLS_CC, "sll", &image_file1, 
																&image_file1_len, 
																&compare_method,
																&reduce_factor) == FAILURE) {
		RETURN_NULL();
	}

	#ifdef IMCS_DEBUG
	php_printf("<br>Passed file1: ");
	PHPWRITE(image_file1, image_file1_len);
	php_printf("<br>Passed compare method: %d", compare_method);
	php_printf("<br>Passed reduce factor: %d", reduce_factor);
	#endif

    imcs_object *obj = (imcs_object *)zend_object_store_get_object(getThis() TSRMLS_CC);
	if (obj!= NULL && obj->engine != NULL) {

		char message[512];
		int message_len = 512;

		int status = obj->engine->prepare(image_file1, compare_method, reduce_factor, message_len, message);
	
		#ifdef IMCS_DEBUG
		php_printf("<br>prepare() returned: ");
		PHPWRITE(message, message_len);
		php_printf("<br>Message length: %d", message_len);
		#endif

		array_init(return_value);
		add_assoc_long(return_value, "status", status);
		add_assoc_string(return_value, "message", message, 1);
    }
}



ZEND_BEGIN_ARG_INFO_EX(arginfo_compare, 0, 0, 1)		//(function name, pass_rest_by_reference, return_reference, required_num_args)
	ZEND_ARG_INFO(0, image_file2)
ZEND_END_ARG_INFO()


ZEND_METHOD(IMCSEngine, compare) {

	char* image_file2;
	int image_file2_len = 0;

	if (zend_parse_parameters(ZEND_NUM_ARGS() TSRMLS_CC, "s", &image_file2, &image_file2_len) == FAILURE) {
		RETURN_NULL();
	}

	#ifdef IMCS_DEBUG
	php_printf("<br>Passed file2: ");
	PHPWRITE(image_file2, image_file2_len);
	#endif

    imcs_object *obj = (imcs_object *)zend_object_store_get_object(getThis() TSRMLS_CC);
	if (obj!= NULL && obj->engine != NULL) {
		
		double result = 0.0;
		int result_type = 0;

		char message[512];
		int message_len = 512;

		int status = obj->engine->compare(image_file2, result, result_type, message_len, message);
		
		#ifdef IMCS_DEBUG
		php_printf("<br>compare() returned: ");
		PHPWRITE(message, message_len);
		php_printf("<br>Message length: %d", message_len);
		#endif

		array_init(return_value);
		add_assoc_long(return_value, "status", status);
		add_assoc_string(return_value, "message", message, 1);
		add_assoc_double(return_value, "result", result);
		add_assoc_long(return_value, "result_type", result_type);
    }
}



zend_function_entry IMCSEngine_methods[] = {	
	ZEND_ME(IMCSEngine, __construct,  NULL,				ZEND_ACC_PUBLIC | ZEND_ACC_CTOR)
	ZEND_ME(IMCSEngine, prepare,      arginfo_pepare,	ZEND_ACC_PUBLIC)
	ZEND_ME(IMCSEngine, compare,      arginfo_compare,	ZEND_ACC_PUBLIC)
	{NULL, NULL, NULL}
};



PHP_MINIT_FUNCTION(imcs) {

	zend_class_entry ce;
    INIT_CLASS_ENTRY(ce, "IMCSEngine", IMCSEngine_methods);
    imcs_ce = zend_register_internal_class(&ce TSRMLS_CC);

	imcs_ce->create_object = imcs_create_handler;
    memcpy(&imcs_object_handlers, zend_get_std_object_handlers(), sizeof(zend_object_handlers));
    imcs_object_handlers.clone_obj = NULL;

    return SUCCESS;
};



PHP_MINFO_FUNCTION(imcs) {
	php_info_print_table_start();
	php_info_print_table_row(2, "Statistical image comparison", "enabled");
	php_info_print_table_end();
};



zend_module_entry imcs_module_entry = {
	STANDARD_MODULE_HEADER,
	PHP_EXT_NAME,
	NULL,
	PHP_MINIT(imcs),
	NULL,
	NULL,	
	NULL,
	PHP_MINFO(imcs),
	PHP_EXT_VERSION,
	STANDARD_MODULE_PROPERTIES
};


extern "C" {
ZEND_GET_MODULE(imcs);  
}
