
#include <stdio.h>
#include "imcs_engine.h"

#ifdef WIN32
#define snprintf sprintf_s
#endif

#define IMCS_DEBUG_TIMES
#undef IMCS_DEBUG_TIMES																//Disable functions timing 

#define IMCS_DEBUG_IMAGES
#undef IMCS_DEBUG_IMAGES															//Disable images display


//Setup comparison parameters
int IMCSEngine::prepare(const char* image_file1, 
						const int	compare_method, 
						const int	reduce_factor, 
						int&		message_len,
						char*		message) {


	//Reset prepared flag
	im_prepared = false;
	

	//Check for valid compare method value
	if (compare_method != COMP_CORREL && compare_method != COMP_CHISQR && compare_method != COMP_INTERSECT) {
		if (compare_method != COMP_BHATTACHARYYA && compare_method != COMP_EMD_L1 && compare_method != COMP_EMD_L2) {
			message_len = snprintf(message, message_len, "Invalid compare method value: %d", compare_method);
			return ST_INVALID_PARAMETERS;			
		}
	}


	//Color reduction factor value must be 0 or in [2, 64]
	if (reduce_factor != RF_NONE && (reduce_factor < RF_MIN || reduce_factor > RF_MAX)) {
		message_len = snprintf(message, message_len, "Invalid color reduction factor value: %d for compare method: %d", reduce_factor, compare_method);
		return ST_INVALID_PARAMETERS;	
	}


	//Color reduction factor value must be in [8, 64] for EMD comparisons 
	if (reduce_factor < RF_MIN_EMD && (compare_method == COMP_EMD_L1 || compare_method == COMP_EMD_L2)) {
		message_len = snprintf(message, message_len, "Invalid color reduction factor value: %d for compare method: %d", reduce_factor, compare_method);
		return ST_INVALID_PARAMETERS;	
	}


	//Read image file data
	image1 = imread(image_file1, CV_LOAD_IMAGE_COLOR);
	if (image1.empty() == true || image1.channels() != 3) {
		message_len = snprintf(message, message_len, "Invalid image file: %s", image_file1);
		return ST_INVALID_IMAGE;
	}


	//Save parameters to class variables
	im_compare_method = compare_method;
	im_reduce_factor = reduce_factor; 
	im_dim_size = 256;																//Initial size = 256 (number of colors per channel)


	//Adjust histogram dimensions (bins) size and reduce image colors
	if (im_reduce_factor >= RF_MIN) {
		im_dim_size = (im_dim_size / im_reduce_factor);								//Set histogram dimensions size

		#ifdef IMCS_DEBUG_IMAGES	
		namedWindow("Original Image1");												//Create window
		imshow("Original Image1", image1);											//Display original image1
		#endif		

		colorReduce(image1, im_reduce_factor);										//Reduce image colors

		#ifdef IMCS_DEBUG_IMAGES
		namedWindow("Reduced Image1");												//Create window
		imshow("Reduced Image1", image1);											//Display color reduced image1
		waitKey(0);
		#endif
	}
	

	//Create image histogram
	calcHistBGR(image1, image_histo1, im_dim_size);
	

	//Calculate EMD signature matrix from histogram
	if (im_compare_method == COMP_EMD_L1 || im_compare_method == COMP_EMD_L2) {
		calcEMDSig(image_histo1, emd_sig1, im_dim_size);	
	}


	//Success
	im_prepared = true;
	return ST_OK;
}



//Perform statistical comparison 
int IMCSEngine::compare(const char* image_file2, 
						double&		result,
						int&		result_type,
						int&		message_len,
						char*		message) {


	//Check if prepared successfully 
	if (im_prepared == false) {
		message_len = snprintf(message, message_len, "Invalid prepared state: %d", im_prepared);
		return ST_INVALID_PARAMETERS;	
	}


	//Read image file data
	image2 = imread(image_file2, CV_LOAD_IMAGE_COLOR);
	if (image2.empty() == true || image2.channels() != 3) {
		message_len = snprintf(message, message_len, "Invalid image file: %s", image_file2);
		return ST_INVALID_IMAGE;
	}


	//Reduce image colors
	if (im_reduce_factor >= RF_MIN) {
		colorReduce(image2, im_reduce_factor);
	}
	

	//Create image histogram 
	calcHistBGR(image2, image_histo2, im_dim_size);


	#ifdef IMCS_DEBUG_TIMES
	double tStart = getTimingStart();													//Start timing
	#endif

	double identity = 0.0;																//Used only in IM_COMP_INTERSECT

	switch (im_compare_method){
		case COMP_CORREL:
			result = compareHist(image_histo1, image_histo2, CV_COMP_CORREL);			//Comparison result (1 is perfect)
			result = (result > 0) ? result : 0;											//Eliminate negative values
			result_type = RES_TYPE_PERCENT;
			break;

		case COMP_CHISQR:
			result = compareHist(image_histo1, image_histo2, CV_COMP_CHISQR);			//Comparison result (0 is perfect)
			result_type = RES_TYPE_VALUE;
			break;

		case COMP_INTERSECT:
			identity = compareHist(image_histo1, image_histo1, CV_COMP_INTERSECT);		//Calculate "identity" value for image1 histogram
			result = compareHist(image_histo1, image_histo2, CV_COMP_INTERSECT);		//Comparison result (higher score is better match)
			result = (result / identity);												//Express as identity fraction
			result_type = RES_TYPE_PERCENT;
			break;

		case COMP_BHATTACHARYYA:
			result = compareHist(image_histo1, image_histo2, CV_COMP_BHATTACHARYYA);	//Comparison result (0 is perfect match)
			result = (1.0 - result);													//Invert scale (1 is perfect)										 
			result_type = RES_TYPE_PERCENT;
			break;

		case COMP_EMD_L1:
			calcEMDSig(image_histo2, emd_sig2, im_dim_size);
			result = EMD(emd_sig1, emd_sig2, CV_DIST_L1);								//Calculate distance (0 is perfect match)
			result_type = RES_TYPE_VALUE;
			break;

		case COMP_EMD_L2:
			calcEMDSig(image_histo2, emd_sig2, im_dim_size);
			result = EMD(emd_sig1, emd_sig2, CV_DIST_L2);								//Calculate distance (0 is perfect match)
			result_type = RES_TYPE_VALUE;
			break;
	}


	#ifdef IMCS_DEBUG_TIMES
	double tElapsed = getTimingResult(tStart);											//Get timing result
	printf("image_compare_stat() exec time: %fms\n", tElapsed);							//Display timing results
	#endif


	//Info message on success
	if (result_type == RES_TYPE_PERCENT) {
		message_len = snprintf(message, message_len, "Result: %f%% for compare method: %d", (result * 100), im_compare_method);
	} else {
		message_len = snprintf(message, message_len, "Result: %f for compare method: %d", result, im_compare_method);
	}

	//Success
	return ST_OK;
}



//Calculate 3D histogram for BGR image
void IMCSEngine::calcHistBGR(Mat &image, OutputArray histogram, const int dim_size) {

	#ifdef IMCS_DEBUG_TIMES
	double tStart = getTimingStart();												//Start timing
	#endif

	int dims_count = 3;
	int dims_array[] = { 0, 1, 2 };
	int dims_sizes[] = { dim_size, dim_size, dim_size };
	
	float range_dimB[] = { 0, 255 };
	float range_dimG[] = { 0, 255 };
	float range_dimR[] = { 0, 255 };
	const float* dims_ranges[] = { range_dimB, range_dimG, range_dimR };

	calcHist(&image, 1, dims_array, Mat(), histogram, dims_count, dims_sizes, dims_ranges, true, false);	//Calculate image 3D histogram
	normalize(histogram, histogram, 1, 0, NORM_L2, -1, Mat());						//Normalize histogram

	#ifdef IMCS_DEBUG_TIMES
	double tElapsed = getTimingResult(tStart);										//Get timing result
	printf("calcHistBGR() exec time: %fms\n", tElapsed);							//Display timing results
	#endif
}



//Reduce Color by divisor factor
void IMCSEngine::colorReduce(Mat &image, const int divisor) {

	#ifdef IMCS_DEBUG_TIMES
	double tStart = getTimingStart();												//Start timing
	#endif

	if (image.isContinuous()) {														//No pixels padding
		image.reshape(1, (image.cols * image.rows));								//Reshape to one dimensional array
	}

	int rows = image.rows;															//Number of rows
	int cols = image.cols * image.channels();										//Number of columns

	for (int j = 0; j < rows; j++) {
		uchar* dataPtr = image.ptr<uchar>(j);										//Pointer to 1st element of each row
		for (int i = 0; i < cols; i++) {
			dataPtr[i] = ((dataPtr[i] / divisor) * divisor) + (divisor / 2);		//Reduce each element value
		}
	}

	#ifdef IMCS_DEBUG_TIMES	
	double tElapsed = getTimingResult(tStart);										//Get timing result
	printf("colorReduce() exec time: %fms\n", tElapsed);							//Display timing results
	#endif
}



//Calculate EMD signature from 3D histogram
void IMCSEngine::calcEMDSig(MatND &histogram, Mat &signature, const int dim_size) {

	#ifdef IMCS_DEBUG_TIMES
	double tStart = getTimingStart();												//Start timing
	#endif

	int dims_sizes[] = { dim_size, dim_size, dim_size };							//Histogram dimension sizes
	int d0 = dims_sizes[0];															//1st histogram dimension size
	int d1 = dims_sizes[1];															//2nd histogram dimension size
	int d2 = dims_sizes[2];															//3rd histogram dimension size
	
	int sig_rows_count = d0 * d1 * d2;												//Signature array total rows	
	signature.create(sig_rows_count, 4, CV_32FC1);									//Signature array: 1 col for values count + 3 cols for coords = total 4 cols

	for(int i = 0; i < d0; i++) {
		for(int j = 0; j < d1; j++) {
			for (int k = 0; k < d2; k++) {
				int sig_row = (i * d1 * d1) + (j * d2) + k;

				signature.at<float>(sig_row, 0) = histogram.at<float>(i, j, k);
				signature.at<float>(sig_row, 1) = (float)i;
				signature.at<float>(sig_row, 2) = (float)j;
				signature.at<float>(sig_row, 3) = (float)k;
			}
		}
	}

	#ifdef IMCS_DEBUG_TIMES
	double tElapsed = getTimingResult(tStart);										//Get timing result
	printf("calcEMDSign() exec time: %fms\n", tElapsed);							//Display timing results
	#endif
}



//Execution timing functions
double IMCSEngine::getTimingStart(void) {
	return (double)getTickCount();													//Get timing starting point 
}

double IMCSEngine::getTimingResult(const double tStart) {
	return (((double)getTickCount() - tStart) / getTickFrequency()) * 1000;			//Calculate execution time in milliseconds relative to tStart
}
