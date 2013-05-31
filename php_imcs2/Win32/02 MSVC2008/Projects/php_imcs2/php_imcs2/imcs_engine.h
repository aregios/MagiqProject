
#ifndef IMCS_ENGINE_H
#define IMCS_ENGINE_H

#include "imcs_base.h"																//Include base class definition
#include "imcs_opencv.h"															//Include OpenCV definitions 

const int ST_OK = 0;																//Status OK
const int ST_INVALID_IMAGE = 1;														//Invalid image file
const int ST_INVALID_PARAMETERS = 2;												//Invalid parameters

const int RES_TYPE_VALUE = 1;														//Result is a value
const int RES_TYPE_PERCENT = 100;													//Result is a percentage

const int RF_NONE	 = 0;															//No colors reduction
const int RF_MIN	 = 2;															//Minimum colors reduction factor
const int RF_MIN_EMD = 8;															//Minimum colors reduction factor for EMD comparisons
const int RF_MAX	 = 64;															//Maximum colors reduction factor

const int COMP_CORREL		 = CV_COMP_CORREL;										//0
const int COMP_CHISQR		 = CV_COMP_CHISQR;										//1
const int COMP_INTERSECT	 = CV_COMP_INTERSECT;									//2
const int COMP_BHATTACHARYYA = CV_COMP_BHATTACHARYYA;								//3
const int COMP_EMD_L1		 = 4;													//4
const int COMP_EMD_L2		 = 5;													//5


//Derive from IMCSBase and overide functions
class IMCSEngine: public IMCSBase {

	public:

		//Setup comparison parameters
		int prepare(const char* image_file1, 
					const int	compare_method, 
					const int	reduce_factor, 
					int&		message_len,
					char*		message);


		//Perform statistical comparison 
		int compare(const char* image_file2, 
					double&		result,
					int&		result_type,
					int&		message_len,
					char*		message);


		//Timing functions
		double getTimingStart(void);												//Start timing
		double getTimingResult(const double tStart);								//Stop timing and get result

	private:

		//Class data
		bool im_prepared;															//Prepared flag

		int	im_compare_method;														//Compare method
		int	im_reduce_factor;														//Reduce color factor
		int im_dim_size;															//Histograms dimension size

		Mat image1;																	//Image1 data matrix
		Mat image2;																	//Image2 data matrix
		
		MatND image_histo1;															//Image1 3D histogram matrix
		MatND image_histo2;															//Image2 3D histogram matrix

		Mat emd_sig1;																//Image1 EMD signature matrix
		Mat emd_sig2;																//Image2 EMD signature matrix


		//Core functions
		void colorReduce(Mat &image, const int divisor);							//Color reduction function
		void calcHistBGR(Mat &image, OutputArray histogram, const int dim_size);	//Histogram creation for RGB images (3 channels used for 3D histogram)
		void calcEMDSig(MatND &histogram, Mat &signature, const int dim_size);		//EMD signature creation from 3D histogram		
};

#endif

