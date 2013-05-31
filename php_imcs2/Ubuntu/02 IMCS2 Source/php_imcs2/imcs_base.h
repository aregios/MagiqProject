
#ifndef IMCS_BASE_H
#define IMCS_BASE_H

class IMCSBase {

	public:

		//Setup comparison parameters
		virtual int prepare(const char* image_file1, 
							const int	compare_method, 
							const int	reduce_factor, 
							int&		message_len,
							char*		message) = 0;


		//Perform statistical comparison 
		virtual int compare(const char* image_file2, 
							double&		result,
							int&		result_type,
							int&		message_len,
							char*		message) = 0;

};

#endif
