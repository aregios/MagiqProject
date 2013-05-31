
#include "imcs_factory.h"
#include "imcs_base.h"
#include "imcs_engine.h"

IMCSBase* IMCSFactory::getIMCSEngine() {
	return new IMCSEngine;
}
