
#ifndef PHP_IMCS_H
#define PHP_IMCS_H

extern "C" {
 
#include "main/php.h"

#ifdef WIN32
#include "Zend/zend_config.w32.h"
#else
#include "Zend/zend_config.h"
#endif
#include "ext/standard/info.h"

}

#define PHP_EXT_NAME "imcs2"
#define PHP_EXT_VERSION "1.0"

extern zend_module_entry imcs_module_entry;
#define phpext_imcs_ptr &imcs_module_entry

#endif
